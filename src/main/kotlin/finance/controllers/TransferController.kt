package finance.controllers

import finance.domain.Transfer
import finance.domain.toCreatedResponse
import finance.domain.toListOkResponse
import finance.domain.toOkResponse
import finance.domain.toPagedOkResponse
import finance.services.TransferService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Transfer Management", description = "Operations for managing transfers")
@RestController
@RequestMapping("/api/transfer")
@PreAuthorize("hasAuthority('USER')")
class TransferController(
    private var transferService: TransferService,
) : StandardizedBaseController(),
    StandardRestController<Transfer, Long> {
    @Operation(summary = "Get all active transfers")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active transfers retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Transfer>> = transferService.findAllActive().toListOkResponse()

    @Operation(summary = "Get all active transfers (paginated)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of transfers returned"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active/paged", produces = ["application/json"])
    override fun findAllActivePaged(pageable: Pageable): ResponseEntity<Page<Transfer>> = transferService.findAllActive(pageable).toPagedOkResponse(pageable)

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
    ): ResponseEntity<Transfer> = transferService.findById(id).toOkResponse()

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
    ): ResponseEntity<Transfer> = transferService.save(entity).toCreatedResponse()

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
        entity.transferId = id
        return transferService.update(entity).toOkResponse()
    }

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
    ): ResponseEntity<Transfer> = transferService.deleteById(id).toOkResponse()
}
