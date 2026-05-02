package finance.controllers

import finance.domain.ServiceResult
import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.domain.toCreatedResponse
import finance.domain.toListOkResponse
import finance.domain.toOkResponse
import finance.services.ValidationAmountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.Locale

@Tag(name = "Validation Amount Management", description = "Operations for managing validation amounts")
@RestController
@RequestMapping("/api/validation/amount")
@PreAuthorize("hasAuthority('USER')")
class ValidationAmountController(
    private var validationAmountService: ValidationAmountService,
) : StandardizedBaseController(),
    StandardRestController<ValidationAmount, Long> {
    override fun findAllActive(): ResponseEntity<List<ValidationAmount>> = findAllActiveWithFilters(null, null)

    @Operation(summary = "Get all active validation amounts (with optional filters)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active validation amounts retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    fun findAllActiveWithFilters(
        @RequestParam(required = false) accountNameOwner: String?,
        @RequestParam(required = false) transactionState: String?,
    ): ResponseEntity<List<ValidationAmount>> {
        val state =
            transactionState?.let {
                try {
                    TransactionState.valueOf(
                        it.lowercase().replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                        },
                    )
                } catch (ex: IllegalArgumentException) {
                    logger.warn("Invalid transaction state: $it")
                    null
                }
            }
        val result =
            if (accountNameOwner != null || state != null) {
                validationAmountService.findAllActiveFiltered(accountNameOwner, state)
            } else {
                validationAmountService.findAllActive()
            }
        return result.toListOkResponse()
    }

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
    ): ResponseEntity<ValidationAmount> = validationAmountService.findById(id).toOkResponse()

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
    ): ResponseEntity<ValidationAmount> = validationAmountService.save(entity).toCreatedResponse()

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
        entity.validationId = id
        return validationAmountService.update(entity).toOkResponse()
    }

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
    ): ResponseEntity<ValidationAmount> = validationAmountService.deleteById(id).toOkResponse()

    // ===== LEGACY ENDPOINTS =====

    @PostMapping("/insert/{accountNameOwner}", consumes = ["application/json"], produces = ["application/json"])
    fun insertValidationAmount(
        @RequestBody validationAmount: ValidationAmount,
        @PathVariable("accountNameOwner") accountNameOwner: String,
    ): ResponseEntity<*> =
        try {
            val response = validationAmountService.insertValidationAmount(accountNameOwner, validationAmount)
            logger.info(writeJson(response))
            ResponseEntity.ok(response)
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error inserting validation amount: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "Validation error: ${ex.message}"))
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid input inserting validation amount: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "Invalid input: ${ex.message}"))
        } catch (ex: ResponseStatusException) {
            ResponseEntity.status(ex.statusCode).body(mapOf("error" to "Failed to insert validation amount: ${ex.message}"))
        } catch (ex: Exception) {
            logger.error("Unexpected error inserting validation amount: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Unexpected error: ${ex.message}"))
        }

    @GetMapping("/select/{accountNameOwner}/{transactionStateValue}")
    fun selectValidationAmountByAccountId(
        @PathVariable("accountNameOwner") accountNameOwner: String,
        @PathVariable("transactionStateValue") transactionStateValue: String,
    ): ResponseEntity<ValidationAmount> =
        handleCrudOperation("selectValidationAmountByAccountId", "$accountNameOwner/$transactionStateValue") {
            val normalizedState =
                transactionStateValue
                    .lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            val validationAmount =
                validationAmountService.findValidationAmountByAccountNameOwner(
                    accountNameOwner,
                    TransactionState.valueOf(normalizedState),
                )
            logger.info(writeJson(validationAmount))
            validationAmount
        }
}
