package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.configurations.CustomProperties
import finance.domain.ExcelFileColumn
import finance.domain.Transaction
import org.apache.logging.log4j.LogManager
import org.apache.poi.poifs.crypt.Decryptor
import org.apache.poi.poifs.crypt.EncryptionInfo
import org.apache.poi.poifs.crypt.Encryptor
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import java.io.*
import java.util.stream.IntStream

@Service
class ExcelFileService(
    private val customProperties: CustomProperties,
    private val transactionService: TransactionService,
    private var meterService: MeterService
) : IExcelFileService {

    @Throws(Exception::class)
    override fun processProtectedExcelFile(inputExcelFileName: String) {
        logger.info("${customProperties.excelInputFilePath}/${inputExcelFileName}")
        val fileStream =
            POIFSFileSystem(FileInputStream("${customProperties.excelInputFilePath}/${inputExcelFileName}"))
        val encryptionInfo = EncryptionInfo(fileStream)
        val decryptor = Decryptor.getInstance(encryptionInfo)
        val validPassword = decryptor.verifyPassword(customProperties.excelPassword)
        if (!validPassword) {
            throw RuntimeException("Password is not valid for file: $inputExcelFileName")
        }
        val inputStream = decryptor.getDataStream(fileStream)

        val workbook: Workbook = XSSFWorkbook(inputStream)
        filterWorkbookThenImportTransactions(workbook)
        inputStream.close()

        try {
            saveProtectedExcelFile(inputExcelFileName, workbook, encryptionInfo)
        } catch (outOfMemoryError: OutOfMemoryError) {
            logger.warn("Saving this excel file requires more memory to be provided to the Java JVM.")
            logger.warn("OutOfMemoryError, ${outOfMemoryError.message}")
            meterService.incrementExceptionCaughtCounter("OutOfMemoryError")
        }
    }

    override fun saveProtectedExcelFile(
        inputExcelFileName: String,
        workbook: Workbook,
        encryptionInfo: EncryptionInfo
    ) {
        val fileOutStream = FileOutputStream(File("${customProperties.excelInputFilePath}/new-${inputExcelFileName}"))
        val poiFileSystem = POIFSFileSystem()

        val encryptor: Encryptor = encryptionInfo.encryptor
        encryptor.confirmPassword(customProperties.excelPassword)
        val outputStream: OutputStream = encryptor.getDataStream(poiFileSystem)
        workbook.write(outputStream)
        outputStream.close()

        poiFileSystem.writeFilesystem(fileOutStream)
        fileOutStream.close()
        poiFileSystem.close()
    }

    override fun filterWorkbookThenImportTransactions(workbook: Workbook) {
        IntStream.range(0, workbook.numberOfSheets).filter { idx: Int ->
            (workbook.getSheetName(idx).contains("_brian") || workbook.getSheetName(idx)
                .contains("_kari")) && !workbook.isSheetHidden(idx)
        }.forEach { idx: Int ->
            if (!isExcludedAccount(customProperties.excludedAccounts, workbook.getSheetName(idx))) {
                processEachExcelSheet(workbook, idx)
            }
        }
    }

    @Throws(IOException::class)
    override fun processEachExcelSheet(workbook: Workbook, sheetNumber: Int) {
        val currentSheet = workbook.getSheetAt(sheetNumber)
        val transactionList = transactionService.findByAccountNameOwnerOrderByTransactionDate(
            workbook.getSheetName(sheetNumber).replace('.', '-')
        )

        logger.info(workbook.getSheetName(sheetNumber))
        removeEachRowInTheWorksheet(currentSheet)
        var currentRow = 1
        for (transaction in transactionList) {
            insertNewRow(currentSheet, currentRow, transaction)
            currentRow += 1
        }
    }

    override fun insertNewRow(currentSheet: Sheet, rowNumber: Int, transaction: Transaction) {
        val newRow = currentSheet.createRow(rowNumber)
        //ExcelFileColumn.values().forEach { println(it) }
        for (columnNumber in 1 until 8) {
            val newCell = newRow.createCell(columnNumber)
            when (ExcelFileColumn.fromInt(columnNumber)) {
                ExcelFileColumn.Guid -> {
                    newCell.setCellValue(transaction.guid)
                }
                ExcelFileColumn.TransactionDate -> {
                    newCell.setCellValue(transaction.transactionDate)
                }
                ExcelFileColumn.Description -> {
                    newCell.setCellValue(transaction.description)
                }
                ExcelFileColumn.Category -> {
                    newCell.setCellValue(transaction.category)
                }
                ExcelFileColumn.Amount -> {
                    newCell.setCellValue(transaction.amount.toDouble())
                }
                ExcelFileColumn.TransactionState -> {
                    newCell.setCellValue(transaction.transactionState.toString())
                }
                ExcelFileColumn.Notes -> {
                    newCell.setCellValue(transaction.notes)
                }
            }
        }
    }

    override fun removeEachRowInTheWorksheet(currentSheet: Sheet) {

        try {
            logger.info("currentSheet.lastRowNum = ${currentSheet.lastRowNum}")
            logger.info("currentSheet.physicalNumberOfRows = ${currentSheet.physicalNumberOfRows}")
            currentSheet.shiftRows(2, currentSheet.lastRowNum, -1)
        } catch (illegalArgumentException: IllegalArgumentException) {
            logger.warn("IllegalArgumentException: ${illegalArgumentException.message}")
            meterService.incrementExceptionCaughtCounter("IllegalArgumentException")
        }

        for (rowNumber in currentSheet.lastRowNum downTo 1) {
            val row = currentSheet.getRow(rowNumber)
            if (row != null) {
                currentSheet.removeRow(row)
            }
        }
    }

    override fun isExcludedAccount(accountExcludedList: List<String>, accountNameOwner: String): Boolean {
        return accountExcludedList.stream().anyMatch { str: String -> str.trim { it <= ' ' } == accountNameOwner }
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}
