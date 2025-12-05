package finance.controllers

import finance.domain.ServiceResult
import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.services.ValidationAmountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.Locale

@CrossOrigin
@Tag(name = "Validation Amount Management", description = "Operations for managing validation amounts")
@RestController
@RequestMapping("/api/validation/amount")
@PreAuthorize("hasAuthority('USER')")
class ValidationAmountController(
    private var validationAmountService: ValidationAmountService,
) : StandardizedBaseController(),
    StandardRestController<ValidationAmount, Long> {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Interface implementation - GET /api/validation/amount/active (no parameters)
     * Delegates to parameterized version
     */
    override fun findAllActive(): ResponseEntity<List<ValidationAmount>> = findAllActiveWithFilters(null, null)

    /**
     * Standardized collection retrieval with filtering - GET /api/validation/amount/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     * Supports optional query parameters for filtering:
     * - accountNameOwner: Filter by account name
     * - transactionState: Filter by transaction state (cleared, outstanding, future)
     */
    @Operation(summary = "Get all active validation amounts (with optional filters)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active validation amounts retrieved"),
            ApiResponse(responseCode = "404", description = "No validation amounts found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    fun findAllActiveWithFilters(
        @org.springframework.web.bind.annotation.RequestParam(required = false) accountNameOwner: String?,
        @org.springframework.web.bind.annotation.RequestParam(required = false) transactionState: String?,
    ): ResponseEntity<List<ValidationAmount>> {
        // Convert transactionState string to enum if provided
        val state =
            transactionState?.let {
                try {
                    TransactionState.valueOf(
                        it
                            .lowercase()
                            .replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() },
                    )
                } catch (ex: IllegalArgumentException) {
                    logger.warn("Invalid transaction state provided: $it")
                    null
                }
            }

        // Use filtered method if any parameters provided, otherwise use standard method
        val result =
            if (accountNameOwner != null || state != null) {
                validationAmountService.findAllActiveFiltered(accountNameOwner, state)
            } else {
                validationAmountService.findAllActive()
            }

        return when (result) {
            is ServiceResult.Success -> {
                val filterMsg =
                    buildString {
                        if (accountNameOwner != null) append(" for account=$accountNameOwner")
                        if (state != null) append(" with state=$state")
                    }
                logger.info("Retrieved ${result.data.size} active validation amounts$filterMsg")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("No validation amounts found")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving validation amounts: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/validation/amount/{validationId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @Operation(summary = "Get validation amount by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Validation amount retrieved"),
            ApiResponse(responseCode = "404", description = "Validation amount not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/{validationId}", produces = ["application/json"])
    override fun findById(
        @PathVariable("validationId") id: Long,
    ): ResponseEntity<ValidationAmount> =
        when (val result = validationAmountService.findById(id)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved validation amount: $id")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Validation amount not found: $id")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving validation amount $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized entity creation - POST /api/validation/amount
     * Returns 201 CREATED
     */
    @Operation(summary = "Create validation amount")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Validation amount created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: ValidationAmount,
    ): ResponseEntity<ValidationAmount> =
        when (val result = validationAmountService.save(entity)) {
            is ServiceResult.Success -> {
                logger.info("Validation amount created successfully: ${result.data.validationId}")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating validation amount: ${result.errors}")
                ResponseEntity.badRequest().build<ValidationAmount>()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating validation amount: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<ValidationAmount>()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error creating validation amount: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<ValidationAmount>()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<ValidationAmount>()
            }
        }

    /**
     * Standardized entity update - PUT /api/validation/amount/{validationId}
     * Uses entity type instead of Map<String, Any>
     */
    @Operation(summary = "Update validation amount by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Validation amount updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Validation amount not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PutMapping("/{validationId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("validationId") id: Long,
        @Valid @RequestBody entity: ValidationAmount,
    ): ResponseEntity<ValidationAmount> {
        // Ensure the validationId in the path matches the entity
        entity.validationId = id

        return when (val result = validationAmountService.update(entity)) {
            is ServiceResult.Success -> {
                logger.info("Validation amount updated successfully: $id")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Validation amount not found for update: $id")
                ResponseEntity.notFound().build<ValidationAmount>()
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating validation amount: ${result.errors}")
                ResponseEntity.badRequest().build<ValidationAmount>()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating validation amount: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<ValidationAmount>()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error updating validation amount $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<ValidationAmount>()
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/validation/amount/{validationId}
     * Returns 200 OK with deleted entity
     */
    @Operation(summary = "Delete validation amount by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Validation amount deleted"),
            ApiResponse(responseCode = "404", description = "Validation amount not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @DeleteMapping("/{validationId}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("validationId") id: Long,
    ): ResponseEntity<ValidationAmount> {
        // First check if the validation amount exists
        val findResult = validationAmountService.findById(id)
        if (findResult !is ServiceResult.Success) {
            logger.warn("Validation amount not found for deletion: $id")
            return ResponseEntity.notFound().build<ValidationAmount>()
        }

        val validationAmountToDelete = findResult.data

        return when (val result = validationAmountService.deleteById(id)) {
            is ServiceResult.Success -> {
                logger.info("Validation amount deleted successfully: $id")
                ResponseEntity.ok(validationAmountToDelete)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Validation amount not found for deletion: $id")
                ResponseEntity.notFound().build<ValidationAmount>()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error deleting validation amount $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<ValidationAmount>()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<ValidationAmount>()
            }
        }
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - POST /api/validation/amount/insert/{accountNameOwner}
     * Maintains original behavior
     */
    @PostMapping("/insert/{accountNameOwner}", consumes = ["application/json"], produces = ["application/json"])
    fun insertValidationAmount(
        @RequestBody validationAmount: ValidationAmount,
        @PathVariable("accountNameOwner") accountNameOwner: String,
    ): ResponseEntity<*> =
        try {
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

    // curl -k https://localhost:8443/validation/amount/select/test_brian/cleared
    @GetMapping("/select/{accountNameOwner}/{transactionStateValue}")
    fun selectValidationAmountByAccountId(
        @PathVariable("accountNameOwner") accountNameOwner: String,
        @PathVariable("transactionStateValue") transactionStateValue: String,
    ): ResponseEntity<ValidationAmount> =
        handleCrudOperation("selectValidationAmountByAccountId", "$accountNameOwner/$transactionStateValue") {
            val newTransactionStateValue =
                transactionStateValue
                    .lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            val validationAmount =
                validationAmountService.findValidationAmountByAccountNameOwner(
                    accountNameOwner,
                    TransactionState.valueOf(newTransactionStateValue),
                )
            logger.info(mapper.writeValueAsString(validationAmount))
            validationAmount
        }
}
