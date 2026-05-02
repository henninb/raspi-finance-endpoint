package finance.controllers

import finance.domain.PendingTransaction
import finance.domain.ServiceResult
import finance.domain.toCreatedResponse
import finance.domain.toListOkResponse
import finance.domain.toOkResponse
import finance.services.PendingTransactionService
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
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Pending Transaction Management", description = "Operations for managing pending transactions")
@RestController
@RequestMapping("/api/pending/transaction")
@PreAuthorize("hasAuthority('USER')")
class PendingTransactionController(
    private val pendingTransactionService: PendingTransactionService,
) : StandardizedBaseController(),
    StandardRestController<PendingTransaction, Long> {
    @Operation(summary = "Get all active pending transactions")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active pending transactions retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<PendingTransaction>> = pendingTransactionService.findAllActive().toListOkResponse()

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
    ): ResponseEntity<PendingTransaction> = pendingTransactionService.findById(id).toOkResponse()

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
    ): ResponseEntity<PendingTransaction> = pendingTransactionService.save(entity).toCreatedResponse()

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
        entity.pendingTransactionId = id
        return pendingTransactionService.update(entity).toOkResponse()
    }

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
    ): ResponseEntity<PendingTransaction> = pendingTransactionService.deleteById(id).toOkResponse()

    @Deprecated("Bulk delete operations should be replaced with individual deletes or batch processing")
    @Operation(summary = "Delete all pending transactions (legacy)")
    @ApiResponses(value = [ApiResponse(responseCode = "204", description = "All pending transactions deleted"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @DeleteMapping("/delete/all")
    fun deleteAllPendingTransactions(): ResponseEntity<Void> =
        when (val result = pendingTransactionService.deleteAll()) {
            is ServiceResult.Success -> ResponseEntity.noContent().build()

            is ServiceResult.SystemError -> throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to delete all pending transactions: ${result.exception.message}",
                result.exception,
            )

            else -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error deleting pending transactions")
        }
}
