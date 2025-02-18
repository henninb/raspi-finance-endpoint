package finance.controllers

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.services.ValidationAmountService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/validation/amount", "/api/validation/amount")
class ValidationAmountController(private var validationAmountService: ValidationAmountService) : BaseController() {

    @PostMapping("/insert/{accountNameOwner}", consumes = ["application/json"], produces = ["application/json"])
    fun insertValidationAmount(
        @RequestBody validationAmount: ValidationAmount,
        @PathVariable("accountNameOwner") accountNameOwner: String
    ): ResponseEntity<ValidationAmount> {
        return try {
            val validationAmountResponse =
                validationAmountService.insertValidationAmount(accountNameOwner, validationAmount)

            logger.info("ValidationAmount inserted successfully")
            logger.info(mapper.writeValueAsString(validationAmountResponse))

            ResponseEntity.ok(validationAmountResponse)
        } catch (ex: ResponseStatusException) {
            logger.error("Failed to insert validation amount: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert validation amount: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error occurred while inserting validation amount: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    //curl -k https://hornsup:8443/validation/amount/select/amazon_brian
    @GetMapping("/select/{accountNameOwner}/{transactionStateValue}")
    fun selectValidationAmountByAccountId(
        @PathVariable("accountNameOwner") accountNameOwner: String,
        @PathVariable("transactionStateValue") transactionStateValue: String
    ): ResponseEntity<ValidationAmount> {

        val newTransactionStateValue = transactionStateValue.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val validationAmount = validationAmountService.findValidationAmountByAccountNameOwner(
            accountNameOwner,
            TransactionState.valueOf(newTransactionStateValue)
        )
        logger.info(mapper.writeValueAsString(validationAmount))
        return ResponseEntity.ok(validationAmount)
    }
}
