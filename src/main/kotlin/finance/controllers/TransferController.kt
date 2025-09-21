package finance.controllers

import finance.domain.Transfer
import finance.services.ITransferService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/api/transfer")
class TransferController(private var transferService: ITransferService) :
    StandardizedBaseController(), StandardRestController<Transfer, Long> {

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/transfer/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Transfer>> {
        return handleCrudOperation("Find all active transfers", null) {
            logger.debug("Retrieving all active transfers")
            val transfers: List<Transfer> = transferService.findAllTransfers()
            logger.info("Retrieved ${transfers.size} active transfers")
            transfers
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/transfer/{transferId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{transferId}", produces = ["application/json"])
    override fun findById(@PathVariable transferId: Long): ResponseEntity<Transfer> {
        return handleCrudOperation("Find transfer by ID", transferId) {
            logger.debug("Retrieving transfer: $transferId")
            val transfer = transferService.findByTransferId(transferId)
                .orElseThrow {
                    logger.warn("Transfer not found: $transferId")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found: $transferId")
                }
            logger.info("Retrieved transfer: $transferId")
            transfer
        }
    }

    /**
     * Standardized entity creation - POST /api/transfer
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(@Valid @RequestBody transfer: Transfer): ResponseEntity<Transfer> {
        return handleCreateOperation("Transfer", transfer.transferId) {
            logger.info("Creating transfer from ${transfer.sourceAccount} to ${transfer.destinationAccount}")
            val result = transferService.insertTransfer(transfer)
            logger.info("Transfer created successfully: ${result.transferId}")
            result
        }
    }

    /**
     * Standardized entity update - PUT /api/transfer/{transferId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{transferId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(@PathVariable transferId: Long, @Valid @RequestBody transfer: Transfer): ResponseEntity<Transfer> {
        return handleCrudOperation("Update transfer", transferId) {
            logger.info("Updating transfer: $transferId")
            // Validate transfer exists first
            transferService.findByTransferId(transferId)
                .orElseThrow {
                    logger.warn("Transfer not found for update: $transferId")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found: $transferId")
                }
            val result = transferService.updateTransfer(transfer)
            logger.info("Transfer updated successfully: $transferId")
            result
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/transfer/{transferId}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{transferId}", produces = ["application/json"])
    override fun deleteById(@PathVariable transferId: Long): ResponseEntity<Transfer> {
        return handleDeleteOperation(
            "Transfer",
            transferId,
            { transferService.findByTransferId(transferId) },
            { transferService.deleteByTransferId(transferId) }
        )
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/transfer/select
     * Maintains original behavior
     */
    @GetMapping("/select", produces = ["application/json"])
    fun selectAllTransfers(): ResponseEntity<List<Transfer>> {
        return try {
            logger.debug("Retrieving all transfers (legacy endpoint)")
            val transfers = transferService.findAllTransfers()
            logger.info("Retrieved ${transfers.size} transfers")
            ResponseEntity.ok(transfers)
        } catch (ex: Exception) {
            logger.error("Failed to retrieve transfers: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transfers: ${ex.message}", ex)
        }
    }

    /**
     * Legacy endpoint - POST /api/transfer/insert
     * Maintains original behavior and return patterns
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertTransfer(@Valid @RequestBody transfer: Transfer): ResponseEntity<Transfer> {
        return try {
            logger.info("Inserting transfer from ${transfer.sourceAccount} to ${transfer.destinationAccount} (legacy endpoint)")
            val transferResponse = transferService.insertTransfer(transfer)
            logger.info("Transfer inserted successfully: ${transferResponse.transferId}")
            ResponseEntity.ok(transferResponse)  // Legacy returns 200 OK, not 201 CREATED
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to insert transfer due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate transfer found.")
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error inserting transfer: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}", ex)
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid input inserting transfer: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error inserting transfer: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    /**
     * Legacy endpoint - DELETE /api/transfer/delete/{transferId}
     * Maintains original behavior and snake_case parameter
     */
    @DeleteMapping("/delete/{transferId}", produces = ["application/json"])
    fun deleteByTransferId(@PathVariable transferId: Long): ResponseEntity<Transfer> {
        return try {
            logger.info("Attempting to delete transfer: $transferId (legacy endpoint)")
            val transferOptional: Optional<Transfer> = transferService.findByTransferId(transferId)

            if (transferOptional.isPresent) {
                val transfer: Transfer = transferOptional.get()
                logger.info("transfer deleted: ${transfer.transferId}")
                transferService.deleteByTransferId(transferId)
                ResponseEntity.ok(transfer)
            } else {
                logger.warn("Transfer not found for deletion: $transferId")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Transfer not found: $transferId")
            }
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to delete transfer $transferId: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete transfer: ${ex.message}", ex)
        }
    }
}