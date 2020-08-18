package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.configurations.CustomProperties
import finance.domain.Transaction
import org.apache.poi.poifs.crypt.Decryptor
import org.apache.poi.poifs.crypt.EncryptionInfo
import org.apache.poi.poifs.crypt.Encryptor
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.*
import java.util.stream.IntStream


@Service
class ExcelFileService @Autowired constructor(private val customProperties: CustomProperties, private val transactionService: TransactionService) {
    @Throws(Exception::class)
    fun processProtectedExcelFile(inputExcelFileName: String) {
        logger.info("${customProperties.excelInputFilePath}/${inputExcelFileName}")
        val fileStream = POIFSFileSystem(FileInputStream("${customProperties.excelInputFilePath}/${inputExcelFileName}"))
        val encryptionInfo = EncryptionInfo(fileStream)
        val decryptor = Decryptor.getInstance(encryptionInfo)
        decryptor.verifyPassword(customProperties.excelPassword)
        val inputStream = decryptor.getDataStream(fileStream)

        val workbook: Workbook = XSSFWorkbook(inputStream)
        filterWorkbookThenImportTransactions(workbook)
        inputStream.close()

        try {
            saveProtectedExcelFile(inputExcelFileName, workbook, encryptionInfo)
        } catch (e: OutOfMemoryError) {
            logger.error("Saving this excel file requires more memory to be provided to the Java JVM.")
        }
    }

    private fun saveProtectedExcelFile(inputExcelFileName: String, workbook: Workbook, encryptionInfo: EncryptionInfo) {
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

    private fun filterWorkbookThenImportTransactions(workbook: Workbook) {
        IntStream.range(0, workbook.numberOfSheets).filter { idx: Int -> (workbook.getSheetName(idx).contains("_brian") || workbook.getSheetName(idx).contains("_kari")) && !workbook.isSheetHidden(idx) }.forEach { idx: Int ->
            if (!isExcludedAccount(customProperties.excludedAccounts, workbook.getSheetName(idx))) {
                processEachExcelSheet(workbook, idx)
            }
        }
    }
    
    @Throws(IOException::class)
    private fun processEachExcelSheet(workbook: Workbook, sheetNumber: Int) {
        val currentSheet = workbook.getSheetAt(sheetNumber)
        val transactionList  = transactionService.findByAccountNameOwnerIgnoreCaseOrderByTransactionDate(workbook.getSheetName(sheetNumber).replace('.', '-'))

        logger.info(workbook.getSheetName(sheetNumber))
        removeEachRowInTheWorksheet(currentSheet)
        var currentRow = 1
        for( transaction in transactionList ) {
            insertNewRow(currentSheet, currentRow, transaction)
            currentRow += 1
        }
    }

    private fun insertNewRow(currentSheet: Sheet, rowNumber: Int , transaction: Transaction) {
        val newRow = currentSheet.createRow(rowNumber)
        for (columnNumber in 1 until 8) {
            val newCell = newRow.createCell(columnNumber)
            when (columnNumber) {
                COL_GUID -> {
                    newCell.setCellValue(transaction.guid)
                }
                COL_TRANSACTION_DATE -> {
                    newCell.setCellValue(transaction.transactionDate)
                }
                COL_DESCRIPTION -> {
                    newCell.setCellValue(transaction.description)
                }
                COL_CATEGORY -> {
                    newCell.setCellValue(transaction.category)
                }
                COL_AMOUNT -> {
                    newCell.setCellValue(transaction.amount.toDouble())
                }
                COL_CLEARED -> {
                    newCell.setCellValue(transaction.cleared.toDouble())
                }
                COL_NOTES -> {
                    newCell.setCellValue(transaction.notes)
                }
                else -> {
                    throw RuntimeException("no column match for transaction guid value:  ${transaction.guid}")
                }
            }
        }
    }

    private fun removeEachRowInTheWorksheet(currentSheet: Sheet) {

        try {
            logger.info("currentSheet.lastRowNum = ${currentSheet.lastRowNum}")
            logger.info("currentSheet.physicalNumberOfRows = ${currentSheet.physicalNumberOfRows}")
            currentSheet.shiftRows(2, currentSheet.lastRowNum, - 1)
        } catch (e: IllegalArgumentException) {
            logger.info("issues with this index")
        }

        for (rowNumber in currentSheet.lastRowNum downTo 1) {
            val row = currentSheet.getRow(rowNumber)
            if (row != null) {
                currentSheet.removeRow(row)
            }
        }
    }

    private fun isExcludedAccount(accountExcludedList: List<String>, accountNameOwner: String): Boolean {
        return accountExcludedList.stream().anyMatch { str: String -> str.trim { it <= ' ' } == accountNameOwner }
    }

    companion object {
        const val COL_GUID = 1
        const val COL_TRANSACTION_DATE = 2
        const val COL_DESCRIPTION = 3
        const val COL_CATEGORY = 4
        const val COL_AMOUNT = 5
        const val COL_CLEARED = 6
        const val COL_NOTES = 7
        val mapper = ObjectMapper()
        val logger: Logger
            get() = LoggerFactory.getLogger(ExcelFileService::class.java)
    }
}
