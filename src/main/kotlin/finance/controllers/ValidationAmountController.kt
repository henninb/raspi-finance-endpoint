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
@RequestMapping("/api/validation/amount")
class ValidationAmountController(private var validationAmountService: ValidationAmountService) : BaseController() {

    // curl -k --header "Content-Type: application/json" --request POST --data '{"transactionDate": "2024-01-01", "amount": 100.00}' https://localhost:8443/validation/amount/insert/test_brian
    @PostMapping("/insert/{accountNameOwner}", consumes = ["application/json"], produces = ["application/json"])
    fun insertValidationAmount(
        @RequestBody validationAmount: ValidationAmount,
        @PathVariable("accountNameOwner") accountNameOwner: String
    ): ResponseEntity<*> {
        return try {
            val validationAmountResponse =
                validationAmountService.insertValidationAmount(accountNameOwner, validationAmount)

            logger.info("ValidationAmount inserted successfully")
            logger.info(mapper.writeValueAsString(validationAmountResponse))

            ResponseEntity.ok(validationAmountResponse)
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error inserting validation amount: ${ex.message}", ex)
            val errorResponse = mapOf("error" to "Validation error: ${ex.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid input inserting validation amount: ${ex.message}", ex)
            val errorResponse = mapOf("error" to "Invalid input: ${ex.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
        } catch (ex: ResponseStatusException) {
            logger.error("Failed to insert validation amount: ${ex.message}", ex)
            val errorResponse = mapOf("error" to "Failed to insert validation amount: ${ex.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
        } catch (ex: Exception) {
            logger.error("Unexpected error occurred while inserting validation amount: ${ex.message}", ex)
            val errorResponse = mapOf("error" to "Unexpected error: ${ex.message}")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
        }
    }

    // curl -k https://localhost:8443/validation/amount/select/test_brian/cleared
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
