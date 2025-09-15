package finance.controllers

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.services.ValidationAmountService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/api/validation/amount")
class ValidationAmountController(private var validationAmountService: ValidationAmountService) :
    StandardizedBaseController(), StandardRestController<ValidationAmount, Long> {

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/validation/amount/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<ValidationAmount>> {
        return handleCrudOperation("Find all active validation amounts", null) {
            logger.debug("Retrieving all active validation amounts (standardized)")
            val validationAmounts: List<ValidationAmount> = validationAmountService.findAllActiveValidationAmounts()
            logger.info("Retrieved ${validationAmounts.size} active validation amounts (standardized)")
            validationAmounts
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/validation/amount/{validationId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{validationId}", produces = ["application/json"])
    override fun findById(@PathVariable validationId: Long): ResponseEntity<ValidationAmount> {
        return handleCrudOperation("Find validation amount by ID", validationId) {
            logger.debug("Retrieving validation amount: $validationId (standardized)")
            val validationAmount = validationAmountService.findValidationAmountById(validationId)
                .orElseThrow {
                    logger.warn("Validation amount not found: $validationId (standardized)")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Validation amount not found: $validationId")
                }
            logger.info("Retrieved validation amount: $validationId (standardized)")
            validationAmount
        }
    }

    /**
     * Standardized entity creation - POST /api/validation/amount
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(@Valid @RequestBody validationAmount: ValidationAmount): ResponseEntity<ValidationAmount> {
        return handleCreateOperation("ValidationAmount", validationAmount.validationId) {
            logger.info("Creating validation amount (standardized)")
            val result = validationAmountService.insertValidationAmount(validationAmount)
            logger.info("Validation amount created successfully: ${result.validationId} (standardized)")
            result
        }
    }

    /**
     * Standardized entity update - PUT /api/validation/amount/{validationId}
     * Uses entity type instead of Map<String, Any>
     */
    @PutMapping("/{validationId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(@PathVariable validationId: Long, @Valid @RequestBody validationAmount: ValidationAmount): ResponseEntity<ValidationAmount> {
        return handleCrudOperation("Update validation amount", validationId) {
            logger.info("Updating validation amount: $validationId (standardized)")
            // Validate validation amount exists first
            validationAmountService.findValidationAmountById(validationId)
                .orElseThrow {
                    logger.warn("Validation amount not found for update: $validationId (standardized)")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Validation amount not found: $validationId")
                }
            val result = validationAmountService.updateValidationAmount(validationAmount)
            logger.info("Validation amount updated successfully: $validationId (standardized)")
            result
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/validation/amount/{validationId}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{validationId}", produces = ["application/json"])
    override fun deleteById(@PathVariable validationId: Long): ResponseEntity<ValidationAmount> {
        return handleDeleteOperation(
            "ValidationAmount",
            validationId,
            { validationAmountService.findValidationAmountById(validationId) },
            { validationAmountService.deleteValidationAmount(validationId) }
        )
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - POST /api/validation/amount/insert/{accountNameOwner}
     * Maintains original behavior
     */
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
