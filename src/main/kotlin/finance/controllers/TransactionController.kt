package finance.controllers

import finance.controllers.dto.TransactionAccountChangeInputDto
import finance.domain.ReceiptImage
import finance.domain.ServiceResult
import finance.domain.Totals
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.services.MeterService
import finance.services.TransactionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/transaction/active
     * Returns all active transactions for the authenticated owner.
     */
    @Operation(summary = "Get all active transactions")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active transactions retrieved"),
            ApiResponse(responseCode = "404", description = "No transactions found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Transaction>> =
        when (val result = transactionService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} active transactions")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("No active transactions found")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.ValidationError -> {
                logger.error("Unexpected validation error retrieving active transactions: ${result.errors}")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            is ServiceResult.BusinessError -> {
                logger.error("Unexpected business error retrieving active transactions: ${result.message}")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving active transactions: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Paginated collection retrieval - GET /api/transaction/active/paged?page=0&size=50
     * Returns Page<Transaction> with metadata (totalElements, totalPages, etc.)
     */
    @Operation(summary = "Get all active transactions (paginated)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of transactions returned"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active/paged", produces = ["application/json"])
    override fun findAllActivePaged(
        pageable: Pageable,
    ): ResponseEntity<Page<Transaction>> {
        logger.debug("Retrieving all active transactions (paginated) - page: ${pageable.pageNumber}, size: ${pageable.pageSize}")
        return when (val result = transactionService.findAllActive(pageable)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved page ${pageable.pageNumber} with ${result.data.numberOfElements} transactions")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("No transactions found")
                ResponseEntity.ok(Page.empty(pageable))
            }

            is ServiceResult.ValidationError -> {
                logger.error("Unexpected validation error retrieving transactions: ${result.errors}")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            is ServiceResult.BusinessError -> {
                logger.error("Unexpected business error retrieving transactions: ${result.message}")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transactions: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/transaction/{guid}
     * Uses camelCase parameter without @PathVariable annotation
     */
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
    ): ResponseEntity<Transaction> =
        when (val result = transactionService.findById(id)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved transaction: $id")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Transaction not found: $id")
                meterService.incrementTransactionRestSelectNoneFoundCounter("unknown")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.ValidationError -> {
                logger.error("Unexpected validation error retrieving transaction $id: ${result.errors}")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            is ServiceResult.BusinessError -> {
                logger.error("Unexpected business error retrieving transaction $id: ${result.message}")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transaction $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized entity creation - POST /api/transaction
     * Returns 201 CREATED
     */
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
    ): ResponseEntity<Transaction> =
        when (val result = transactionService.save(entity)) {
            is ServiceResult.Success -> {
                logger.info("Transaction created successfully: ${entity.guid}")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.error("Unexpected not-found creating transaction: ${entity.guid}")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Transaction>()
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating transaction: ${result.errors}")
                ResponseEntity.badRequest().build<Transaction>()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating transaction: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<Transaction>()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error creating transaction: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Transaction>()
            }
        }

    /**
     * Standardized entity update - PUT /api/transaction/{guid}
     * Uses camelCase parameter without @PathVariable annotation
     */
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
        // Ensure the guid matches the path parameter
        entity.guid = id
        return when (val result = transactionService.update(entity)) {
            is ServiceResult.Success -> {
                logger.info("Transaction updated successfully: $id")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Transaction not found for update: $id")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating transaction: ${result.errors}")
                ResponseEntity.badRequest().build<Transaction>()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating transaction: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<Transaction>()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error updating transaction $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Transaction>()
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/transaction/{guid}
     * Returns 200 OK with deleted entity
     */
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
    ): ResponseEntity<Transaction> =
        when (val deleteResult = transactionService.deleteById(id)) {
            is ServiceResult.Success -> {
                logger.info("Transaction deleted successfully: $id")
                ResponseEntity.ok(deleteResult.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Transaction not found for deletion: $id")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.ValidationError -> {
                logger.error("Validation error deleting transaction: ${deleteResult.errors}")
                ResponseEntity.badRequest().build()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error deleting transaction: ${deleteResult.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error deleting transaction $id: ${deleteResult.exception.message}", deleteResult.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * New business endpoint - GET /api/transaction/date-range
     * Returns a paged list of transactions across all accounts filtered by transactionDate
     * Query params: startDate=yyyy-MM-dd, endDate=yyyy-MM-dd, plus standard Spring Data page params
     */
    @Operation(summary = "Find transactions by date range (paged)")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Page returned"), ApiResponse(responseCode = "400", description = "Invalid range"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @GetMapping("/date-range", produces = ["application/json"])
    fun findByDateRange(
        @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: java.time.LocalDate,
        @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: java.time.LocalDate,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<Transaction>> {
        val pageable: Pageable =
            org.springframework.data.domain.PageRequest.of(
                page,
                size,
                org.springframework.data.domain.Sort
                    .by("transactionDate")
                    .descending(),
            )
        return when (val result = transactionService.findTransactionsByDateRangeStandardized(start, end, pageable)) {
            is ServiceResult.Success -> ResponseEntity.ok(result.data)
            is ServiceResult.NotFound -> ResponseEntity.ok(Page.empty(pageable))
            is ServiceResult.ValidationError -> ResponseEntity.badRequest().build()
            is ServiceResult.BusinessError -> ResponseEntity.badRequest().build()
            is ServiceResult.SystemError -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Legacy business logic endpoint - GET /api/transaction/account/select/{accountNameOwner}
     * Returns transactions for specific account (business logic preserved)
     */
    @Operation(summary = "List transactions for an account")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Transactions returned"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @GetMapping("/account/select/{accountNameOwner}", produces = ["application/json"])
    fun selectByAccountNameOwner(
        @PathVariable("accountNameOwner") accountNameOwner: String,
    ): ResponseEntity<List<Transaction>> =
        when (val result = transactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner)) {
            is ServiceResult.Success -> {
                val transactions = result.data
                if (transactions.isEmpty()) {
                    logger.info("No transactions found for account: $accountNameOwner")
                } else {
                    logger.info("Retrieved ${transactions.size} transactions for account: $accountNameOwner")
                }
                ResponseEntity.ok(transactions)
            }

            is ServiceResult.NotFound -> {
                logger.info("No transactions found for account: $accountNameOwner")
                ResponseEntity.ok(emptyList())
            }

            is ServiceResult.ValidationError -> {
                logger.error("Unexpected validation error retrieving transactions for $accountNameOwner: ${result.errors}")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transactions")
            }

            is ServiceResult.BusinessError -> {
                logger.error("Unexpected business error retrieving transactions for $accountNameOwner: ${result.message}")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transactions")
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transactions for $accountNameOwner: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transactions: ${result.exception.message}", result.exception)
            }
        }

    /**
     * Paginated account transactions - GET /api/transaction/account/select/{accountNameOwner}/paged?page=0&size=50
     * Returns Page<Transaction> with two-tier sorting (transactionState DESC, transactionDate DESC)
     */
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
    ): ResponseEntity<Page<Transaction>> {
        logger.debug("Retrieving transactions for account: $accountNameOwner (paginated) - page: ${pageable.pageNumber}, size: ${pageable.pageSize}")
        return when (val result = transactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner, pageable)) {
            is ServiceResult.Success -> {
                val page = result.data
                logger.info("Retrieved page ${pageable.pageNumber} with ${page.numberOfElements} transactions for account: $accountNameOwner")
                ResponseEntity.ok(page)
            }

            is ServiceResult.NotFound -> {
                logger.warn("No transactions found for account: $accountNameOwner")
                ResponseEntity.ok(Page.empty(pageable))
            }

            is ServiceResult.ValidationError -> {
                logger.error("Unexpected validation error retrieving transactions for $accountNameOwner: ${result.errors}")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            is ServiceResult.BusinessError -> {
                logger.error("Unexpected business error retrieving transactions for $accountNameOwner: ${result.message}")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transactions for $accountNameOwner: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    // curl -k https://localhost:8443/transaction/account/totals/chase_brian
    @Operation(summary = "Get totals for an account")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Totals returned"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @GetMapping("/account/totals/{accountNameOwner}", produces = ["application/json"])
    fun selectTotalsCleared(
        @PathVariable("accountNameOwner") accountNameOwner: String,
    ): ResponseEntity<Totals> =
        try {
            logger.debug("Calculating totals for account: $accountNameOwner")
            val results: Totals =
                transactionService.calculateActiveTotalsByAccountNameOwner(accountNameOwner)

            logger.info("Calculated totals for account $accountNameOwner: $results")
            ResponseEntity.ok(results)
        } catch (ex: Exception) {
            logger.error("Failed to calculate totals for account $accountNameOwner: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to calculate account totals: ${ex.message}", ex)
        }

    // curl -k --header "Content-Type: application/json" --request PUT https://localhost:8443/transaction/state/update/340c315d-39ad-4a02-a294-84a74c1c7ddc/cleared
    @Operation(summary = "Update transaction state by GUID")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Transaction state updated"), ApiResponse(responseCode = "400", description = "Invalid state"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @PutMapping(
        "/state/update/{guid}/{transactionStateValue}",
        consumes = ["application/json"],
        produces = ["application/json"],
    )
    fun updateTransactionState(
        @PathVariable("guid") guid: String,
        @PathVariable("transactionStateValue") transactionStateValue: String,
    ): ResponseEntity<Transaction> {
        logger.info("Updating transaction state for $guid to $transactionStateValue")
        val newTransactionStateValue =
            transactionStateValue
                .lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val transactionState =
            try {
                TransactionState.valueOf(newTransactionStateValue)
            } catch (ex: IllegalArgumentException) {
                logger.error("Invalid transaction state value: $transactionStateValue for transaction $guid", ex)
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transaction state: $transactionStateValue", ex)
            }
        return when (val result = transactionService.updateTransactionStateStandardized(guid, transactionState)) {
            is ServiceResult.Success -> {
                logger.info("Transaction state updated successfully for $guid to $newTransactionStateValue")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Transaction not found for state update: $guid")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating transaction state: ${result.errors}")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating transaction state: ${result.message}")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, result.message)
            }

            is ServiceResult.SystemError -> {
                logger.error("System error updating transaction state for $guid: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update transaction state: ${result.exception.message}", result.exception)
            }
        }
    }

    // Modern business logic endpoint - POST /api/transaction/future
    // curl -k --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "description":"future transaction", "category":"misc", "amount": 15.00, "reoccurringType":"monthly"}' https://localhost:8443/transaction/future
    @Operation(summary = "Create a future-dated transaction")
    @ApiResponses(value = [ApiResponse(responseCode = "201", description = "Future transaction created"), ApiResponse(responseCode = "400", description = "Validation error"), ApiResponse(responseCode = "409", description = "Duplicate"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @PostMapping("/future", consumes = ["application/json"], produces = ["application/json"])
    fun insertFutureTransaction(
        @RequestBody transaction: Transaction,
    ): ResponseEntity<Transaction> {
        logger.info("Inserting future transaction for account: ${transaction.accountNameOwner}")
        return when (val result = transactionService.createAndSaveFutureTransaction(transaction)) {
            is ServiceResult.Success -> {
                logger.info("Future transaction inserted successfully: ${result.data.guid}")
                ResponseEntity(result.data, HttpStatus.CREATED)
            }

            is ServiceResult.NotFound -> {
                logger.error("Unexpected not-found creating future transaction: ${result.message}")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error creating future transaction")
            }

            is ServiceResult.ValidationError -> {
                logger.error("Validation error creating future transaction: ${result.errors}")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${result.errors}")
            }

            is ServiceResult.BusinessError -> {
                logger.error("Business error creating future transaction: ${result.message}")
                throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate future transaction found.")
            }

            is ServiceResult.SystemError -> {
                logger.error("Unexpected error creating future transaction: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${result.exception.message}", result.exception)
            }
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT --data '{"guid":"340c315d-39ad-4a02-a294-84a74c1c7ddc", "accountNameOwner":"new_account"}' https://localhost:8443/transaction/update/account
    @Operation(summary = "Change the accountNameOwner for a transaction")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Transaction updated"), ApiResponse(responseCode = "400", description = "Missing fields"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @PutMapping("/update/account", consumes = ["application/json"], produces = ["application/json"])
    fun changeTransactionAccountNameOwner(
        @Valid @RequestBody payload: TransactionAccountChangeInputDto,
    ): ResponseEntity<Transaction> {
        logger.info("Changing transaction account for guid ${payload.guid} to accountNameOwner ${payload.accountNameOwner}")
        return when (val result = transactionService.changeAccountNameOwnerStandardized(payload.accountNameOwner, payload.guid)) {
            is ServiceResult.Success -> {
                logger.info("Transaction account updated successfully for guid ${payload.guid}")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Transaction or account not found for guid ${payload.guid}")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error changing transaction account: ${result.errors}")
                ResponseEntity.badRequest().build()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error changing transaction account: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error changing transaction account: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT --data 'base64encodedimagedata' https://localhost:8443/transaction/update/receipt/image/da8a0a55-c4ef-44dc-9e5a-4cb7367a164f
    @Operation(summary = "Update receipt image for a transaction")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Receipt image updated"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @PutMapping("/update/receipt/image/{guid}", produces = ["application/json"])
    fun updateTransactionReceiptImageByGuid(
        @PathVariable("guid") guid: String,
        @RequestBody payload: String,
    ): ResponseEntity<ReceiptImage> {
        logger.info("Updating receipt image for transaction: $guid")
        return when (val result = transactionService.updateTransactionReceiptImageByGuidStandardized(guid, payload)) {
            is ServiceResult.Success -> {
                logger.info("Receipt image updated successfully for transaction: $guid")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Transaction not found for receipt image update: $guid")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.ValidationError -> {
                logger.error("Validation error updating receipt image for $guid: ${result.errors}")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update receipt image")
            }

            is ServiceResult.BusinessError -> {
                logger.error("Business error updating receipt image for $guid: ${result.message}")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update receipt image: ${result.message}")
            }

            is ServiceResult.SystemError -> {
                logger.error("System error updating receipt image for $guid: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update receipt image: ${result.exception.message}", result.exception)
            }
        }
    }

    // curl -k https://localhost:8443/transaction/category/ach
    @Operation(summary = "List transactions by category name")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Transactions returned"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @GetMapping("/category/{category_name}", produces = ["application/json"])
    fun selectTransactionsByCategory(
        @PathVariable("category_name") categoryName: String,
    ): ResponseEntity<List<Transaction>> =
        when (val result = transactionService.findTransactionsByCategoryStandardized(categoryName)) {
            is ServiceResult.Success -> {
                val transactions = result.data
                if (transactions.isEmpty()) {
                    logger.info("No transactions found for category: $categoryName")
                } else {
                    logger.info("Retrieved ${transactions.size} transactions for category: $categoryName")
                }
                ResponseEntity.ok(transactions)
            }

            is ServiceResult.NotFound -> {
                logger.info("No transactions found for category: $categoryName")
                ResponseEntity.ok(emptyList())
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error retrieving transactions for category $categoryName: ${result.errors}")
                ResponseEntity.ok(emptyList())
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error retrieving transactions for category $categoryName: ${result.message}")
                ResponseEntity.ok(emptyList())
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transactions for category $categoryName: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transactions by category: ${result.exception.message}", result.exception)
            }
        }

    // curl -k https://localhost:8443/transaction/description/amazon
    @Operation(summary = "List transactions by description name")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Transactions returned"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @GetMapping("/description/{description_name}", produces = ["application/json"])
    fun selectTransactionsByDescription(
        @PathVariable("description_name") descriptionName: String,
    ): ResponseEntity<List<Transaction>> =
        when (val result = transactionService.findTransactionsByDescriptionStandardized(descriptionName)) {
            is ServiceResult.Success -> {
                val transactions = result.data
                if (transactions.isEmpty()) {
                    logger.info("No transactions found for description: $descriptionName")
                } else {
                    logger.info("Retrieved ${transactions.size} transactions for description: $descriptionName")
                }
                ResponseEntity.ok(transactions)
            }

            is ServiceResult.NotFound -> {
                logger.info("No transactions found for description: $descriptionName")
                ResponseEntity.ok(emptyList())
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error retrieving transactions for description $descriptionName: ${result.errors}")
                ResponseEntity.ok(emptyList())
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error retrieving transactions for description $descriptionName: ${result.message}")
                ResponseEntity.ok(emptyList())
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transactions for description $descriptionName: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transactions by description: ${result.exception.message}", result.exception)
            }
        }
}
