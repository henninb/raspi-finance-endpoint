package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.services.ExcelFileService
import org.apache.logging.log4j.LogManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
@RequestMapping("/excel")
class ExcelFileController(private var excelFileService: ExcelFileService) : BaseController() {

    @GetMapping(path = ["/file/export"], produces = ["application/json"])
    fun exportExcelFile(): ResponseEntity<String> {
        excelFileService.processProtectedExcelFile("finance_db_master.xlsm")
        return ResponseEntity.ok("finished loading and saving excel file")
    }
}