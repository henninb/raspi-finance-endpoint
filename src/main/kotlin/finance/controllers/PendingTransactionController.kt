package finance.controllers

import finance.domain.PendingTransaction
import finance.domain.ServiceResult
import finance.services.PendingTransactionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

@CrossOrigin
@Tag(name = "Pending Transaction Management", description = "Operations for managing pending transactions")
@RestController
@RequestMapping("/api/pending/transaction")
class PendingTransactionController(
    private val pendingTransactionService: PendingTransactionService,
) : StandardizedBaseController(),
    StandardRestController<PendingTransaction, Long> {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/pending/transaction/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @Operation(summary = "Get all active pending transactions")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active pending transactions retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<PendingTransaction>> =
        when (val result = pendingTransactionService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} active pending transactions")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("No pending transactions found")
                ResponseEntity.ok(emptyList())
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving pending transactions: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized single entity retrieval - GET /api/pending/transaction/{pendingTransactionId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @Operation(summary = "Get pending transaction by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Pending transaction retrieved"),
            ApiResponse(responseCode = "404", description = "Pending transaction not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/{pendingTransactionId}", produces = ["application/json"])
    override fun findById(
        @PathVariable("pendingTransactionId") id: Long,
    ): ResponseEntity<PendingTransaction> =
        when (val result = pendingTransactionService.findById(id)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved pending transaction: $id")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Pending transaction not found: $id")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving pending transaction $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized entity creation - POST /api/pending/transaction
     * Returns 201 CREATED
     */
    @Operation(summary = "Create pending transaction")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Pending transaction created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: PendingTransaction,
    ): ResponseEntity<PendingTransaction> =
        when (val result = pendingTransactionService.save(entity)) {
            is ServiceResult.Success -> {
                logger.info("Pending transaction created successfully: ${result.data.pendingTransactionId}")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating pending transaction: ${result.errors}")
                ResponseEntity.badRequest().build()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating pending transaction: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error creating pending transaction: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized entity update - PUT /api/pending/transaction/{pendingTransactionId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @Operation(summary = "Update pending transaction by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Pending transaction updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Pending transaction not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PutMapping("/{pendingTransactionId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("pendingTransactionId") id: Long,
        @Valid @RequestBody entity: PendingTransaction,
    ): ResponseEntity<PendingTransaction> {
        // Ensure the ID in the path matches the entity
        entity.pendingTransactionId = id

        return when (val result = pendingTransactionService.update(entity)) {
            is ServiceResult.Success -> {
                logger.info("Pending transaction updated successfully: $id")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Pending transaction not found for update: $id")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating pending transaction: ${result.errors}")
                ResponseEntity.badRequest().build()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating pending transaction: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error updating pending transaction $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/pending/transaction/{pendingTransactionId}
     * Returns 200 OK with deleted entity (instead of 204 NO_CONTENT)
     */
    @Operation(summary = "Delete pending transaction by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Pending transaction deleted"),
            ApiResponse(responseCode = "404", description = "Pending transaction not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @DeleteMapping("/{pendingTransactionId}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("pendingTransactionId") id: Long,
    ): ResponseEntity<PendingTransaction> {
        // First get the entity to return it
        val entityResult = pendingTransactionService.findById(id)
        if (entityResult !is ServiceResult.Success) {
            return when (entityResult) {
                is ServiceResult.NotFound -> {
                    logger.warn("Pending transaction not found for deletion: $id")
                    ResponseEntity.notFound().build()
                }

                is ServiceResult.SystemError -> {
                    logger.error("System error retrieving pending transaction for deletion $id: ${entityResult.exception.message}", entityResult.exception)
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }

                else -> {
                    logger.error("Unexpected result type retrieving entity: $entityResult")
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
            }
        }

        val entityToDelete = entityResult.data

        return when (val result = pendingTransactionService.deleteById(id)) {
            is ServiceResult.Success -> {
                logger.info("Pending transaction deleted successfully: $id")
                ResponseEntity.ok(entityToDelete)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Pending transaction not found for deletion: $id")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error deleting pending transaction $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - DELETE /api/pending/transaction/delete/all
     * Bulk delete operation - remains as-is for backward compatibility
     * @deprecated Bulk delete operations should be replaced with individual deletes or batch processing
     */
    @Deprecated("Bulk delete operations should be replaced with individual deletes or batch processing")
    @Operation(summary = "Delete all pending transactions (legacy)")
    @ApiResponses(value = [ApiResponse(responseCode = "204", description = "All pending transactions deleted"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @DeleteMapping("/delete/all")
    fun deleteAllPendingTransactions(): ResponseEntity<Void> {
        logger.info("Attempting to delete all pending transactions (legacy endpoint)")
        return when (val result = pendingTransactionService.deleteAll()) {
            is ServiceResult.Success -> {
                logger.info("All pending transactions deleted successfully")
                ResponseEntity.noContent().build()
            }

            is ServiceResult.SystemError -> {
                logger.error("Failed to delete all pending transactions: ${result.exception.message}", result.exception)
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete all pending transactions: ${result.exception.message}",
                    result.exception,
                )
            }

            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete all pending transactions: Unexpected error",
                )
            }
        }
    }
}
