package finance.controllers

import finance.domain.ValidationAmount
import finance.services.ValidationAmountService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/validation/amount")
class ValidationAmountController(private var validationAmountService: ValidationAmountService) : BaseController() {

    @PostMapping("/insert", produces = ["application/json"])
    fun insertValidationAmount(@RequestBody validationAmount: ValidationAmount): ResponseEntity<String> {
        validationAmountService.insertValidationAmount(validationAmount)
        logger.info("validationAmount inserted")
        return ResponseEntity.ok("validationAmount inserted")
    }
}
