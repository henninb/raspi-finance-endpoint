package finance.controllers

import finance.domain.PendingTransaction
import finance.services.StandardizedPendingTransactionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid

@CrossOrigin
@RestController
@RequestMapping("/api/pending/transaction")
class PendingTransactionController(private val pendingTransactionService: StandardizedPendingTransactionService) :
    StandardizedBaseController(), StandardRestController<PendingTransaction, Long> {

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/pending/transaction/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<PendingTransaction>> {
        return handleCrudOperation("Find all active pending transactions", null) {
            logger.debug("Retrieving all active pending transactions")
            val transactions: List<PendingTransaction> = pendingTransactionService.getAllPendingTransactions()
            logger.info("Retrieved ${transactions.size} active pending transactions")
            transactions
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/pending/transaction/{pendingTransactionId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{pendingTransactionId}", produces = ["application/json"])
    override fun findById(@PathVariable pendingTransactionId: Long): ResponseEntity<PendingTransaction> {
        return handleCrudOperation("Find pending transaction by ID", pendingTransactionId) {
            logger.debug("Retrieving pending transaction: $pendingTransactionId")
            val transaction = pendingTransactionService.findByPendingTransactionId(pendingTransactionId)
                .orElseThrow {
                    logger.warn("Pending transaction not found: $pendingTransactionId")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Pending transaction not found: $pendingTransactionId")
                }
            logger.info("Retrieved pending transaction: $pendingTransactionId")
            transaction
        }
    }

    /**
     * Standardized entity creation - POST /api/pending/transaction
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(@Valid @RequestBody pendingTransaction: PendingTransaction): ResponseEntity<PendingTransaction> {
        return handleCreateOperation("PendingTransaction", pendingTransaction.description ?: "unknown") {
            logger.info("Creating pending transaction: ${pendingTransaction.description}")
            val result = pendingTransactionService.insertPendingTransaction(pendingTransaction)
            logger.info("Pending transaction created successfully: ${result.pendingTransactionId}")
            result
        }
    }

    /**
     * Standardized entity update - PUT /api/pending/transaction/{pendingTransactionId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{pendingTransactionId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(@PathVariable pendingTransactionId: Long, @Valid @RequestBody pendingTransaction: PendingTransaction): ResponseEntity<PendingTransaction> {
        return handleCrudOperation("Update pending transaction", pendingTransactionId) {
            logger.info("Updating pending transaction: $pendingTransactionId")
            // Validate transaction exists first
            pendingTransactionService.findByPendingTransactionId(pendingTransactionId)
                .orElseThrow {
                    logger.warn("Pending transaction not found for update: $pendingTransactionId")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Pending transaction not found: $pendingTransactionId")
                }
            val result = pendingTransactionService.updatePendingTransaction(pendingTransaction)
            logger.info("Pending transaction updated successfully: $pendingTransactionId")
            result
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/pending/transaction/{pendingTransactionId}
     * Returns 200 OK with deleted entity (instead of 204 NO_CONTENT)
     */
    @DeleteMapping("/{pendingTransactionId}", produces = ["application/json"])
    override fun deleteById(@PathVariable pendingTransactionId: Long): ResponseEntity<PendingTransaction> {
        return handleDeleteOperation(
            "PendingTransaction",
            pendingTransactionId,
            { pendingTransactionService.findByPendingTransactionId(pendingTransactionId) },
            { pendingTransactionService.deletePendingTransaction(pendingTransactionId) }
        )
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/pending/transaction/all
     * Maintains original behavior including 404 when empty
     */
    @GetMapping("/all", produces = ["application/json"])
    fun getAllPendingTransactions(): ResponseEntity<List<PendingTransaction>> {
        return try {
            logger.debug("Retrieving all pending transactions (legacy endpoint)")
            val transactions = pendingTransactionService.getAllPendingTransactions()
            if (transactions.isEmpty()) {
                logger.warn("No pending transactions found")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "No pending transactions found.")
            }
            logger.info("Retrieved ${transactions.size} pending transactions")
            ResponseEntity.ok(transactions)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to retrieve pending transactions: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve pending transactions: ${ex.message}", ex)
        }
    }

    /**
     * Legacy endpoint - POST /api/pending/transaction/insert
     * Maintains original behavior
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertPendingTransaction(@RequestBody pendingTransaction: PendingTransaction): ResponseEntity<PendingTransaction> {
        return try {
            logger.info("Inserting pending transaction: ${pendingTransaction.description} (legacy endpoint)")
            val response = pendingTransactionService.insertPendingTransaction(pendingTransaction)
            logger.info("Pending transaction inserted successfully: ${response.pendingTransactionId}")
            ResponseEntity(response, HttpStatus.CREATED)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to insert pending transaction due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate pending transaction found.")
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error inserting pending transaction: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}", ex)
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid input inserting pending transaction: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Failed to insert pending transaction: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to insert pending transaction: ${ex.message}", ex)
        }
    }

    /**
     * Legacy endpoint - DELETE /api/pending/transaction/delete/{id}
     * Returns 204 NO_CONTENT for backward compatibility
     */
    @DeleteMapping("/delete/{id}")
    fun deletePendingTransaction(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            logger.info("Attempting to delete pending transaction: $id (legacy endpoint)")
            val success = pendingTransactionService.deletePendingTransaction(id)
            if (success) {
                logger.info("Pending transaction deleted successfully: $id")
                ResponseEntity.noContent().build()
            } else {
                logger.warn("Failed to delete pending transaction: $id")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to delete pending transaction with ID: $id")
            }
        } catch (ex: ResponseStatusException) {
            // Preserve legacy behavior: map NOT_FOUND to INTERNAL_SERVER_ERROR for this endpoint
            if (ex.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn("Pending transaction not found: $id (mapping 404 to 500 for legacy contract)")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PendingTransaction not found: $id")
            }
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to delete pending transaction $id: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete pending transaction: ${ex.message}", ex)
        }
    }


    /**
     * Legacy endpoint - DELETE /api/pending/transaction/delete/all
     * Bulk delete operation - remains as-is for backward compatibility
     */
    @DeleteMapping("/delete/all")
    fun deleteAllPendingTransactions(): ResponseEntity<Void> {
        return try {
            logger.info("Attempting to delete all pending transactions (legacy endpoint)")
            pendingTransactionService.deleteAllPendingTransactions()
            logger.info("All pending transactions deleted successfully")
            ResponseEntity.noContent().build()
        } catch (ex: Exception) {
            logger.error("Failed to delete all pending transactions: ${ex.message}", ex)
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to delete all pending transactions: ${ex.message}",
                ex
            )
        }
    }
}
