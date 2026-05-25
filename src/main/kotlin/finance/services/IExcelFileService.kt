package finance.services

import finance.domain.Transaction
import org.apache.poi.poifs.crypt.EncryptionInfo
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook

interface IExcelFileService {

    fun processProtectedExcelFile(inputExcelFileName: String)
    fun saveProtectedExcelFile(inputExcelFileName: String, workbook: Workbook, encryptionInfo: EncryptionInfo)
    fun filterWorkbookThenImportTransactions(workbook: Workbook)

    fun processEachExcelSheet(workbook: Workbook, sheetNumber: Int)
    fun insertNewRow(currentSheet: Sheet, rowNumber: Int, transaction: Transaction)
    fun cloneSheetTemplate(workbook: Workbook, newName: String)
}