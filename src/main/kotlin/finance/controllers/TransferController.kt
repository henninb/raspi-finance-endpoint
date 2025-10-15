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
class TransferController(
    private var standardizedTransferService: StandardizedTransferService,
) : StandardizedBaseController(),
    StandardRestController<Transfer, Long> {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/transfer/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Transfer>> =
        when (val result = standardizedTransferService.findAllActive()) {
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

    /**
     * Standardized single entity retrieval - GET /api/transfer/{transferId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{transferId}", produces = ["application/json"])
    override fun findById(
        @PathVariable("transferId") id: Long,
    ): ResponseEntity<Transfer> =
        when (val result = standardizedTransferService.findById(id)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved transfer: $id")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Transfer not found: $id")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transfer $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized entity creation - POST /api/transfer
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: Transfer,
    ): ResponseEntity<Transfer> =
        when (val result = standardizedTransferService.save(entity)) {
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

    /**
     * Standardized entity update - PUT /api/transfer/{transferId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{transferId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("transferId") id: Long,
        @Valid @RequestBody entity: Transfer,
    ): ResponseEntity<Transfer> {
        // Ensure the path ID matches the entity ID
        entity.transferId = id

        @Suppress("REDUNDANT_ELSE_IN_WHEN") // Defensive programming: handle unexpected ServiceResult types
        return when (val result = standardizedTransferService.update(entity)) {
            is ServiceResult.Success -> {
                logger.info("Transfer updated successfully: $id")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Transfer not found for update: $id")
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
                logger.error("System error updating transfer $id: ${result.exception.message}", result.exception)
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
        @PathVariable("transferId") id: Long,
    ): ResponseEntity<Transfer> {
        // First, retrieve the transfer to return it
        val transferToDelete =
            when (val findResult = standardizedTransferService.findById(id)) {
                is ServiceResult.Success -> findResult.data
                is ServiceResult.NotFound -> {
                    logger.warn("Transfer not found for deletion: $id")
                    return ResponseEntity.notFound().build()
                }
                else -> {
                    logger.error("Error retrieving transfer for deletion: $id")
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                }
            }

        // Then delete it
        return when (val deleteResult = standardizedTransferService.deleteById(id)) {
            is ServiceResult.Success -> {
                logger.info("Transfer deleted successfully: $id")
                ResponseEntity.ok(transferToDelete)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Transfer not found for deletion: $id")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error deleting transfer $id: ${deleteResult.exception.message}", deleteResult.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $deleteResult")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }
}
