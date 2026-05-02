package finance.controllers

import finance.controllers.dto.TransactionAccountChangeInputDto
import finance.domain.ReceiptImage
import finance.domain.ServiceResult
import finance.domain.Totals
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.toCreatedResponse
import finance.domain.toListOkResponse
import finance.domain.toOkResponse
import finance.domain.toPagedOkResponse
import finance.services.MeterService
import finance.services.TransactionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
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

@Tag(name = "Transaction Management", description = "Operations for managing transactions")
@RestController
@RequestMapping("/api/transaction")
@PreAuthorize("hasAuthority('USER')")
class TransactionController(
    private val transactionService: TransactionService,
    private val meterService: MeterService,
) : StandardizedBaseController(),
    StandardRestController<Transaction, String> {
    @Operation(summary = "Get all active transactions")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active transactions retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Transaction>> = transactionService.findAllActive().toListOkResponse()

    @Operation(summary = "Get all active transactions (paginated)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of transactions returned"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active/paged", produces = ["application/json"])
    override fun findAllActivePaged(pageable: Pageable): ResponseEntity<Page<Transaction>> = transactionService.findAllActive(pageable).toPagedOkResponse(pageable)

    @Operation(summary = "Get transaction by GUID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Transaction retrieved"),
            ApiResponse(responseCode = "404", description = "Transaction not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/{guid}", produces = ["application/json"])
    override fun findById(
        @PathVariable("guid") id: String,
    ): ResponseEntity<Transaction> {
        val result = transactionService.findById(id)
        if (result is ServiceResult.NotFound) meterService.incrementTransactionRestSelectNoneFoundCounter("unknown")
        return result.toOkResponse()
    }

    @Operation(summary = "Create transaction")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Transaction created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: Transaction,
    ): ResponseEntity<Transaction> = transactionService.save(entity).toCreatedResponse()

    @Operation(summary = "Update transaction by GUID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Transaction updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Transaction not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PutMapping("/{guid}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("guid") id: String,
        @Valid @RequestBody entity: Transaction,
    ): ResponseEntity<Transaction> {
        entity.guid = id
        return transactionService.update(entity).toOkResponse()
    }

    @Operation(summary = "Delete transaction by GUID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Transaction deleted"),
            ApiResponse(responseCode = "404", description = "Transaction not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @DeleteMapping("/{guid}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("guid") id: String,
    ): ResponseEntity<Transaction> = transactionService.deleteById(id).toOkResponse()

    // ===== BUSINESS LOGIC ENDPOINTS =====

    @Operation(summary = "Find transactions by date range (paged)")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Page returned"), ApiResponse(responseCode = "400", description = "Invalid range"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @GetMapping("/date-range", produces = ["application/json"])
    fun findByDateRange(
        @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: java.time.LocalDate,
        @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: java.time.LocalDate,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<Transaction>> {
        val pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending())
        return transactionService.findTransactionsByDateRangeStandardized(start, end, pageable).toPagedOkResponse(pageable)
    }

    @Operation(summary = "List transactions for an account")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Transactions returned"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @GetMapping("/account/select/{accountNameOwner}", produces = ["application/json"])
    fun selectByAccountNameOwner(
        @PathVariable("accountNameOwner") accountNameOwner: String,
    ): ResponseEntity<List<Transaction>> = transactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner).toListOkResponse()

    @Operation(summary = "List transactions for an account (paginated)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of transactions returned"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/account/select/{accountNameOwner}/paged", produces = ["application/json"])
    fun selectByAccountNameOwnerPaged(
        @PathVariable("accountNameOwner") accountNameOwner: String,
        pageable: Pageable,
    ): ResponseEntity<Page<Transaction>> = transactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner, pageable).toPagedOkResponse(pageable)

    @Operation(summary = "Get totals for an account")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Totals returned"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @GetMapping("/account/totals/{accountNameOwner}", produces = ["application/json"])
    fun selectTotalsCleared(
        @PathVariable("accountNameOwner") accountNameOwner: String,
    ): ResponseEntity<Totals> = ResponseEntity.ok(transactionService.calculateActiveTotalsByAccountNameOwner(accountNameOwner))

    @Operation(summary = "Update transaction state by GUID")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Transaction state updated"), ApiResponse(responseCode = "400", description = "Invalid state"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @PutMapping("/state/update/{guid}/{transactionStateValue}", consumes = ["application/json"], produces = ["application/json"])
    fun updateTransactionState(
        @PathVariable("guid") guid: String,
        @PathVariable("transactionStateValue") transactionStateValue: String,
    ): ResponseEntity<Transaction> {
        val normalizedState =
            transactionStateValue
                .lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val transactionState =
            try {
                TransactionState.valueOf(normalizedState)
            } catch (ex: IllegalArgumentException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transaction state: $transactionStateValue", ex)
            }
        return when (val result = transactionService.updateTransactionStateStandardized(guid, transactionState)) {
            is ServiceResult.Success -> ResponseEntity.ok(result.data)
            is ServiceResult.NotFound -> ResponseEntity.notFound().build()
            is ServiceResult.ValidationError -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${result.errors}")
            is ServiceResult.BusinessError -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, result.message)
            is ServiceResult.SystemError -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update transaction state")
        }
    }

    @Operation(summary = "Create a future-dated transaction")
    @ApiResponses(value = [ApiResponse(responseCode = "201", description = "Future transaction created"), ApiResponse(responseCode = "400", description = "Validation error"), ApiResponse(responseCode = "409", description = "Duplicate"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @PostMapping("/future", consumes = ["application/json"], produces = ["application/json"])
    fun insertFutureTransaction(
        @Valid @RequestBody transaction: Transaction,
    ): ResponseEntity<Transaction> =
        when (val result = transactionService.createAndSaveFutureTransaction(transaction)) {
            is ServiceResult.Success -> ResponseEntity(result.data, HttpStatus.CREATED)
            is ServiceResult.NotFound -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error creating future transaction")
            is ServiceResult.ValidationError -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${result.errors}")
            is ServiceResult.BusinessError -> throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate future transaction found.")
            is ServiceResult.SystemError -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error creating future transaction")
        }

    @Operation(summary = "Change the accountNameOwner for a transaction")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Transaction updated"), ApiResponse(responseCode = "400", description = "Missing fields"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @PutMapping("/update/account", consumes = ["application/json"], produces = ["application/json"])
    fun changeTransactionAccountNameOwner(
        @Valid @RequestBody payload: TransactionAccountChangeInputDto,
    ): ResponseEntity<Transaction> = transactionService.changeAccountNameOwnerStandardized(payload.accountNameOwner, payload.guid).toOkResponse()

    @Operation(summary = "Update receipt image for a transaction")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Receipt image updated"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @PutMapping("/update/receipt/image/{guid}", produces = ["application/json"])
    fun updateTransactionReceiptImageByGuid(
        @PathVariable("guid") guid: String,
        @RequestBody payload: String,
    ): ResponseEntity<ReceiptImage> =
        when (val result = transactionService.updateTransactionReceiptImageByGuidStandardized(guid, payload)) {
            is ServiceResult.Success -> ResponseEntity.ok(result.data)
            is ServiceResult.NotFound -> ResponseEntity.notFound().build()
            else -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update receipt image")
        }

    @Operation(summary = "Delete receipt image for a transaction")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Receipt image deleted"), ApiResponse(responseCode = "404", description = "Transaction or receipt image not found"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @DeleteMapping("/receipt/image/{guid}", produces = ["application/json"])
    fun deleteTransactionReceiptImageByGuid(
        @PathVariable("guid") guid: String,
    ): ResponseEntity<Void> =
        when (val result = transactionService.deleteReceiptImageForTransactionByGuidStandardized(guid)) {
            is ServiceResult.Success -> ResponseEntity.ok().build()
            is ServiceResult.NotFound -> ResponseEntity.notFound().build()
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }

    @Operation(summary = "List transactions by category name")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Transactions returned"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @GetMapping("/category/{category_name}", produces = ["application/json"])
    fun selectTransactionsByCategory(
        @PathVariable("category_name") categoryName: String,
    ): ResponseEntity<List<Transaction>> = transactionService.findTransactionsByCategoryStandardized(categoryName).toListOkResponse()

    @Operation(summary = "List transactions by description name")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Transactions returned"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @GetMapping("/description/{description_name}", produces = ["application/json"])
    fun selectTransactionsByDescription(
        @PathVariable("description_name") descriptionName: String,
    ): ResponseEntity<List<Transaction>> = transactionService.findTransactionsByDescriptionStandardized(descriptionName).toListOkResponse()
}
