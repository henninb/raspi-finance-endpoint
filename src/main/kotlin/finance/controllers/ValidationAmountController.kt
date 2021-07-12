package finance.controllers

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.services.ValidationAmountService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/validation/amount")
class ValidationAmountController(private var validationAmountService: ValidationAmountService) : BaseController() {

    @PostMapping("/insert/{accountNameOwner}", produces = ["application/json"])
    fun insertValidationAmount(@RequestBody validationAmount: ValidationAmount, @PathVariable("accountNameOwner") accountNameOwner : String) : ResponseEntity<String> {
        val validationAmountResponse = validationAmountService.insertValidationAmount(accountNameOwner, validationAmount)
        logger.info("validationAmount inserted")
        logger.info(mapper.writeValueAsString(validationAmountResponse))
        return ResponseEntity.ok(mapper.writeValueAsString(validationAmountResponse))
    }

    //curl -k https://hornsup:8080/validation/amount/select/amazon_brian
    @GetMapping("/select/{accountNameOwner}/{transactionStateValue}")
    fun selectValidationAmountByAccountId(@PathVariable("accountNameOwner") accountNameOwner: String, @PathVariable("transactionStateValue") transactionStateValue: String): ResponseEntity<String> {

        val newTransactionStateValue = transactionStateValue.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val validationAmount = validationAmountService.findValidationAmountByAccountNameOwner(accountNameOwner, TransactionState.valueOf(newTransactionStateValue))
        logger.info(mapper.writeValueAsString(validationAmount))
        return ResponseEntity.ok(mapper.writeValueAsString(validationAmount))
    }
}
