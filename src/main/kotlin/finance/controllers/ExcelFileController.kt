package finance.controllers

import finance.services.ExcelFileService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@CrossOrigin
@RestController
@RequestMapping("/excel", "/api/excel")
class ExcelFileController(private val excelFileService: ExcelFileService) : BaseController() {

    // curl -k https://localhost:8443/excel/file/export
    @GetMapping("/file/export", produces = ["application/json"])
    fun exportExcelFile(): ResponseEntity<String> {
        return try {
            logger.info("Processing protected excel file: finance_db_master.xlsm")
            excelFileService.processProtectedExcelFile("finance_db_master.xlsm")
            logger.info("Excel file processed successfully")
            ResponseEntity.ok("Excel file processed successfully")
        } catch (ex: Exception) {
            logger.error("Failed to process excel file: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process excel file: ${ex.message}", ex)
        }
    }
}