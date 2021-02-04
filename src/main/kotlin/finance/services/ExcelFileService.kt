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
import java.util.*
import java.util.stream.IntStream

@Service
class ExcelFileService(
    private val customProperties: CustomProperties,
    private val transactionService: TransactionService,
    private val accountService: AccountService,
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
            //TODO: add metric
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
        val fileOutStream = FileOutputStream(File("${customProperties.excelInputFilePath}/${UUID.randomUUID()}-${inputExcelFileName}"))
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
        val accounts = accountService.findByActiveStatusOrderByAccountNameOwner()
        accounts.forEach { account ->
            cloneSheetTemplate(workbook, account.accountNameOwner)
        }
        logger.debug("workbook.numberOfSheets: ${workbook.numberOfSheets}")
        IntStream.range(0, workbook.numberOfSheets).filter { idx: Int ->
            workbook.getSheetName(idx).contains("_")
        }.forEach { idx: Int ->
            processEachExcelSheet(workbook, idx)
        }
    }

    private fun cloneSheetTemplate(workbook: Workbook, newName: String) {
        val newSheet = workbook.cloneSheet(workbook.getSheetIndex("template"))
        val newIndex = workbook.getSheetIndex(newSheet)
        workbook.setSheetName(newIndex, newName)
    }

    @Throws(IOException::class)
    override fun processEachExcelSheet(workbook: Workbook, sheetNumber: Int) {
        val currentSheet = workbook.getSheetAt(sheetNumber)
        val transactionList = transactionService.findByAccountNameOwnerOrderByTransactionDate(
            workbook.getSheetName(sheetNumber).replace('.', '-')
        )

        logger.debug("sheetName: ${workbook.getSheetName(sheetNumber)}")
        var currentRow = 1
        for (transaction in transactionList) {
            insertNewRow(currentSheet, currentRow, transaction)
            currentRow += 1
        }
    }

    override fun insertNewRow(currentSheet: Sheet, rowNumber: Int, transaction: Transaction) {
        val newRow = currentSheet.createRow(rowNumber)
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

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}
