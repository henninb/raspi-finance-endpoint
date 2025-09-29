package finance.controllers

import finance.domain.ServiceResult
import finance.domain.Transfer
import finance.services.StandardizedTransferService
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
import java.util.Optional

@CrossOrigin
@RestController
@RequestMapping("/api/transfer")
class TransferController(private var standardizedTransferService: StandardizedTransferService) :
    StandardizedBaseController(), StandardRestController<Transfer, Long> {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/transfer/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Transfer>> {
        return when (val result = standardizedTransferService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} active transfers")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("No transfers found")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transfers: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/transfer/{transferId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{transferId}", produces = ["application/json"])
    override fun findById(
        @PathVariable transferId: Long,
    ): ResponseEntity<Transfer> {
        return when (val result = standardizedTransferService.findById(transferId)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved transfer: $transferId")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Transfer not found: $transferId")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transfer $transferId: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity creation - POST /api/transfer
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody transfer: Transfer,
    ): ResponseEntity<Transfer> {
        return when (val result = standardizedTransferService.save(transfer)) {
            is ServiceResult.Success -> {
                logger.info("Transfer created successfully: ${result.data.transferId}")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating transfer: ${result.errors}")
                ResponseEntity.badRequest().build()
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating transfer: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error creating transfer: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity update - PUT /api/transfer/{transferId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{transferId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable transferId: Long,
        @Valid @RequestBody transfer: Transfer,
    ): ResponseEntity<Transfer> {
        // Ensure the path ID matches the entity ID
        transfer.transferId = transferId

        return when (val result = standardizedTransferService.update(transfer)) {
            is ServiceResult.Success -> {
                logger.info("Transfer updated successfully: $transferId")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Transfer not found for update: $transferId")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating transfer: ${result.errors}")
                ResponseEntity.badRequest().build()
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating transfer: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error updating transfer $transferId: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/transfer/{transferId}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{transferId}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable transferId: Long,
    ): ResponseEntity<Transfer> {
        // First, retrieve the transfer to return it
        val transferToDelete =
            when (val findResult = standardizedTransferService.findById(transferId)) {
                is ServiceResult.Success -> findResult.data
                is ServiceResult.NotFound -> {
                    logger.warn("Transfer not found for deletion: $transferId")
                    return ResponseEntity.notFound().build()
                }
                else -> {
                    logger.error("Error retrieving transfer for deletion: $transferId")
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
            }

        // Then delete it
        return when (val deleteResult = standardizedTransferService.deleteById(transferId)) {
            is ServiceResult.Success -> {
                logger.info("Transfer deleted successfully: $transferId")
                ResponseEntity.ok(transferToDelete)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Transfer not found for deletion: $transferId")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error deleting transfer $transferId: ${deleteResult.exception.message}", deleteResult.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $deleteResult")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/transfer/select
     * Maintains original behavior
     */
    @GetMapping("/select", produces = ["application/json"])
    fun selectAllTransfers(): ResponseEntity<List<Transfer>> {
        return when (val result = standardizedTransferService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} transfers (legacy endpoint)")
                ResponseEntity.ok(result.data)
            }
            else -> {
                logger.error("Failed to retrieve transfers: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transfers")
            }
        }
    }

    /**
     * Legacy endpoint - POST /api/transfer/insert
     * Maintains original behavior and return patterns
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertTransfer(
        @Valid @RequestBody transfer: Transfer,
    ): ResponseEntity<Transfer> {
        logger.info("Inserting transfer from ${transfer.sourceAccount} to ${transfer.destinationAccount} (legacy endpoint)")

        // Use legacy method for complex business logic (account validation, transaction creation)
        return try {
            val transferResponse = standardizedTransferService.insertTransfer(transfer)
            logger.info("Transfer inserted successfully: ${transferResponse.transferId}")
            ResponseEntity.ok(transferResponse) // Legacy returns 200 OK, not 201 CREATED
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
    fun deleteByTransferId(
        @PathVariable transferId: Long,
    ): ResponseEntity<Transfer> {
        return try {
            logger.info("Attempting to delete transfer: $transferId (legacy endpoint)")
            val transferOptional: Optional<Transfer> = standardizedTransferService.findByTransferId(transferId)

            if (transferOptional.isPresent) {
                val transfer: Transfer = transferOptional.get()
                logger.info("transfer deleted: ${transfer.transferId}")
                standardizedTransferService.deleteByTransferId(transferId)
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
