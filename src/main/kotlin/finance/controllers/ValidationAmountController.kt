package finance.controllers

import finance.domain.ValidationAmount
import finance.services.ValidationAmountService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

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

    @GetMapping("/select/{accountId}")
    fun selectValidationAmountByAccountId(@PathVariable("accountId") accountId: Long): ResponseEntity<String> {
        val validationAmountOptional = validationAmountService.findByAccountId(accountId)

        if (validationAmountOptional.isPresent) {
            return ResponseEntity.ok(mapper.writeValueAsString(validationAmountOptional.get()))
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "validationAmount not found for: $accountId")
    }
}
