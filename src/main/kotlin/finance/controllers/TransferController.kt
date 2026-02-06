package finance.controllers

import finance.domain.ServiceResult
import finance.domain.Transfer
import finance.services.TransferService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

@Tag(name = "Transfer Management", description = "Operations for managing transfers")
@RestController
@RequestMapping("/api/transfer")
class TransferController(
    private var transferService: TransferService,
) : StandardizedBaseController(),
    StandardRestController<Transfer, Long> {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/transfer/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @Operation(summary = "Get all active transfers")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active transfers retrieved"),
            ApiResponse(responseCode = "404", description = "No transfers found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Transfer>> =
        when (val result = transferService.findAllActive()) {
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
     * Paginated collection retrieval - GET /api/transfer/active/paged?page=0&size=50
     * Returns Page<Transfer> with metadata
     */
    @Operation(summary = "Get all active transfers (paginated)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of transfers returned"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active/paged", produces = ["application/json"])
    override fun findAllActivePaged(
        pageable: Pageable,
    ): ResponseEntity<Page<Transfer>> {
        logger.debug("Retrieving all active transfers (paginated) - page: ${pageable.pageNumber}, size: ${pageable.pageSize}")
        return when (val result = transferService.findAllActive(pageable)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved page ${pageable.pageNumber} with ${result.data.numberOfElements} transfers")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("No transfers found")
                ResponseEntity.ok(Page.empty(pageable))
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
    @Operation(summary = "Get transfer by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Transfer retrieved"),
            ApiResponse(responseCode = "404", description = "Transfer not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/{transferId}", produces = ["application/json"])
    override fun findById(
        @PathVariable("transferId") id: Long,
    ): ResponseEntity<Transfer> =
        when (val result = transferService.findById(id)) {
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
    @Operation(summary = "Create transfer")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Transfer created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: Transfer,
    ): ResponseEntity<Transfer> =
        when (val result = transferService.save(entity)) {
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
    @Operation(summary = "Update transfer by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Transfer updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Transfer not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PutMapping("/{transferId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("transferId") id: Long,
        @Valid @RequestBody entity: Transfer,
    ): ResponseEntity<Transfer> {
        // Ensure the path ID matches the entity ID
        entity.transferId = id

        @Suppress("REDUNDANT_ELSE_IN_WHEN") // Defensive programming: handle unexpected ServiceResult types
        return when (val result = transferService.update(entity)) {
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
    @Operation(summary = "Delete transfer by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Transfer deleted"),
            ApiResponse(responseCode = "404", description = "Transfer not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @DeleteMapping("/{transferId}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("transferId") id: Long,
    ): ResponseEntity<Transfer> {
        // First, retrieve the transfer to return it
        val transferToDelete =
            when (val findResult = transferService.findById(id)) {
                is ServiceResult.Success -> {
                    findResult.data
                }

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
        return when (val deleteResult = transferService.deleteById(id)) {
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
