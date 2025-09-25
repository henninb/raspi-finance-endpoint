package finance.controllers

import finance.domain.*
import finance.services.MeterService
import finance.services.StandardizedTransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import java.math.BigDecimal
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/api/transaction")
class TransactionController(private val standardizedTransactionService: StandardizedTransactionService, private val meterService: MeterService) :
    StandardizedBaseController(), StandardRestController<Transaction, String> {

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/transaction/active
     * Returns empty list (standardized behavior) - use business endpoints for meaningful queries
     * Note: Transactions are typically queried by account, category, or other criteria
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Transaction>> {
        return handleCrudOperation("Find all active transactions", null) {
            logger.debug("Retrieving all active transactions (standardized endpoint)")
            // For standardization compliance, return empty list
            // Business logic endpoints like /account/select/{account} should be used for actual queries
            val transactions: List<Transaction> = emptyList()
            logger.info("Standardized endpoint - returning empty list. Use business endpoints for data.")
            transactions
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/transaction/{guid}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{guid}", produces = ["application/json"])
    override fun findById(@PathVariable guid: String): ResponseEntity<Transaction> {
        return when (val result = standardizedTransactionService.findById(guid)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved transaction: $guid")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Transaction not found: $guid")
                meterService.incrementTransactionRestSelectNoneFoundCounter("unknown")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transaction $guid: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity creation - POST /api/transaction
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(@Valid @RequestBody transaction: Transaction): ResponseEntity<Transaction> {
        return when (val result = standardizedTransactionService.save(transaction)) {
            is ServiceResult.Success -> {
                logger.info("Transaction created successfully: ${transaction.guid}")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
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
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity update - PUT /api/transaction/{guid}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{guid}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(@PathVariable guid: String, @Valid @RequestBody transaction: Transaction): ResponseEntity<Transaction> {
        // Ensure the guid matches the path parameter
        val updatedTransaction = transaction.copy(guid = guid)

        return when (val result = standardizedTransactionService.update(updatedTransaction)) {
            is ServiceResult.Success -> {
                logger.info("Transaction updated successfully: $guid")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Transaction not found for update: $guid")
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
                logger.error("System error updating transaction $guid: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Transaction>()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/transaction/{guid}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{guid}", produces = ["application/json"])
    override fun deleteById(@PathVariable guid: String): ResponseEntity<Transaction> {
        // First find the entity to return it after deletion
        val entityResult = standardizedTransactionService.findById(guid)
        if (entityResult !is ServiceResult.Success) {
            logger.warn("Transaction not found for deletion: $guid")
            return ResponseEntity.notFound().build()
        }

        return when (val deleteResult = standardizedTransactionService.deleteById(guid)) {
            is ServiceResult.Success -> {
                logger.info("Transaction deleted successfully: $guid")
                ResponseEntity.ok(entityResult.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Transaction not found for deletion: $guid")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error deleting transaction $guid: ${deleteResult.exception.message}", deleteResult.exception)
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
     * Legacy business logic endpoint - GET /api/transaction/account/select/{accountNameOwner}
     * Returns transactions for specific account (business logic preserved)
     */
    @GetMapping("/account/select/{accountNameOwner}", produces = ["application/json"])
    fun selectByAccountNameOwner(@PathVariable("accountNameOwner") accountNameOwner: String): ResponseEntity<List<Transaction>> {
        return try {
            logger.debug("Retrieving transactions for account: $accountNameOwner")
            val transactions: List<Transaction> =
                standardizedTransactionService.findByAccountNameOwnerOrderByTransactionDate(accountNameOwner)

            if (transactions.isEmpty()) {
                logger.info("No transactions found for account: $accountNameOwner")
            } else {
                logger.info("Retrieved ${transactions.size} transactions for account: $accountNameOwner")
            }
            ResponseEntity.ok(transactions)
        } catch (ex: Exception) {
            logger.error("Failed to retrieve transactions for account $accountNameOwner: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transactions: ${ex.message}", ex)
        }
    }


    // curl -k https://localhost:8443/transaction/account/totals/chase_brian
    @GetMapping("/account/totals/{accountNameOwner}", produces = ["application/json"])
    fun selectTotalsCleared(@PathVariable("accountNameOwner") accountNameOwner: String): ResponseEntity<Totals> {
        return try {
            logger.debug("Calculating totals for account: $accountNameOwner")
            val results: Totals =
                standardizedTransactionService.calculateActiveTotalsByAccountNameOwner(accountNameOwner)

            logger.info("Calculated totals for account $accountNameOwner: $results")
            ResponseEntity.ok(results)
        } catch (ex: Exception) {
            logger.error("Failed to calculate totals for account $accountNameOwner: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to calculate account totals: ${ex.message}", ex)
        }
    }


    /**
     * Legacy CRUD endpoint - GET /api/transaction/select/{guid}
     * Original method name preserved for backward compatibility
     */
    @GetMapping("/select/{guid}", produces = ["application/json"])
    fun findTransaction(@PathVariable("guid") guid: String): ResponseEntity<Transaction> {
        logger.debug("findTransaction() - Searching for transaction with guid = $guid (legacy endpoint)")

        return when (val result = standardizedTransactionService.findById(guid)) {
            is ServiceResult.Success -> {
                logger.debug("Transaction found, guid = $guid")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.error("Transaction not found, guid = $guid")
                meterService.incrementTransactionRestSelectNoneFoundCounter("unknown")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found, guid: $guid")
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transaction $guid: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transaction: ${result.exception.message}", result.exception)
            }
            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred")
            }
        }
    }

    /**
     * Legacy CRUD endpoint - PUT /api/transaction/update/{guid}
     * Original method name preserved for backward compatibility
     */
    @PutMapping("/update/{guid}", consumes = ["application/json"], produces = ["application/json"])
    fun updateTransaction(
        @PathVariable("guid") guid: String,
        @RequestBody toBePatchedTransaction: Transaction
    ): ResponseEntity<Transaction> {
        logger.info("Updating transaction: $guid (legacy endpoint)")

        // First validate transaction exists and get existing data
        val existingTransaction = when (val findResult = standardizedTransactionService.findById(guid)) {
            is ServiceResult.Success -> findResult.data
            is ServiceResult.NotFound -> {
                logger.warn("Transaction not found for update: $guid")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found: $guid")
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transaction for update $guid: ${findResult.exception.message}", findResult.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transaction: ${findResult.exception.message}", findResult.exception)
            }
            else -> {
                logger.error("Unexpected result type: $findResult")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred")
            }
        }

        // Preserve existing transaction ID and ensure guid matches path parameter
        val updatedTransaction = toBePatchedTransaction.copy(
            transactionId = existingTransaction.transactionId,
            guid = guid
        )

        return when (val result = standardizedTransactionService.update(updatedTransaction)) {
            is ServiceResult.Success -> {
                logger.info("Transaction updated successfully: $guid")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Transaction not found for update: $guid")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found: $guid")
            }
            is ServiceResult.ValidationError -> {
                logger.error("Validation error updating transaction $guid: ${result.errors}")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${result.errors}")
            }
            is ServiceResult.BusinessError -> {
                logger.error("Business error updating transaction $guid: ${result.message}")
                throw ResponseStatusException(HttpStatus.CONFLICT, result.message)
            }
            is ServiceResult.SystemError -> {
                logger.error("Failed to update transaction $guid: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update transaction: ${result.exception.message}", result.exception)
            }
            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred")
            }
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT https://localhost:8443/transaction/state/update/340c315d-39ad-4a02-a294-84a74c1c7ddc/cleared
    @PutMapping(
        "/state/update/{guid}/{transactionStateValue}",
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    fun updateTransactionState(
        @PathVariable("guid") guid: String,
        @PathVariable("transactionStateValue") transactionStateValue: String
    ): ResponseEntity<Transaction> {
        return try {
            logger.info("Updating transaction state for $guid to $transactionStateValue")
            val newTransactionStateValue = transactionStateValue.lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            val transactionResponse =
                standardizedTransactionService.updateTransactionState(guid, TransactionState.valueOf(newTransactionStateValue))
            logger.info("Transaction state updated successfully for $guid to $newTransactionStateValue")
            ResponseEntity.ok(transactionResponse)
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid transaction state value: $transactionStateValue for transaction $guid", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transaction state: $transactionStateValue", ex)
        } catch (ex: Exception) {
            logger.error("Failed to update transaction state for $guid: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update transaction state: ${ex.message}", ex)
        }
    }

    /**
     * Legacy CRUD endpoint - POST /api/transaction/insert
     * Original method name preserved for backward compatibility
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertTransaction(@RequestBody transaction: Transaction): ResponseEntity<Transaction> {
        logger.info("Attempting to insert transaction: ${mapper.writeValueAsString(transaction)} (legacy endpoint)")

        return when (val result = standardizedTransactionService.save(transaction)) {
            is ServiceResult.Success -> {
                logger.info("Transaction inserted successfully: ${mapper.writeValueAsString(result.data)}")
                ResponseEntity(result.data, HttpStatus.CREATED)
            }
            is ServiceResult.ValidationError -> {
                logger.error("Validation error inserting transaction: ${result.errors}")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${result.errors}")
            }
            is ServiceResult.BusinessError -> {
                logger.error("Failed to insert transaction due to data integrity violation: ${result.message}")
                throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate transaction found.")
            }
            is ServiceResult.SystemError -> {
                logger.error("Unexpected error occurred while inserting transaction: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${result.exception.message}", result.exception)
            }
            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred")
            }
        }
    }

    // curl -k --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "description":"future transaction", "category":"misc", "amount": 15.00, "reoccurringType":"monthly"}' https://localhost:8443/transaction/future/insert
    @PostMapping("/future/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertFutureTransaction(@RequestBody transaction: Transaction): ResponseEntity<Transaction> {
        logger.info("Inserting future transaction for account: ${transaction.accountNameOwner} (legacy endpoint)")
        val futureTransaction = standardizedTransactionService.createFutureTransaction(transaction)
        logger.debug("Created future transaction with date: ${futureTransaction.transactionDate}")

        return when (val result = standardizedTransactionService.save(futureTransaction)) {
            is ServiceResult.Success -> {
                logger.info("Future transaction inserted successfully: ${result.data.guid}")
                ResponseEntity(result.data, HttpStatus.CREATED)
            }
            is ServiceResult.ValidationError -> {
                logger.error("Validation error inserting future transaction: ${result.errors}")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${result.errors}")
            }
            is ServiceResult.BusinessError -> {
                logger.error("Failed to insert future transaction due to data integrity violation: ${result.message}")
                throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate future transaction found.")
            }
            is ServiceResult.SystemError -> {
                logger.error("Unexpected error inserting future transaction: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${result.exception.message}", result.exception)
            }
            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred")
            }
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT --data '{"guid":"340c315d-39ad-4a02-a294-84a74c1c7ddc", "accountNameOwner":"new_account"}' https://localhost:8443/transaction/update/account
    @PutMapping("/update/account", consumes = ["application/json"], produces = ["application/json"])
    fun changeTransactionAccountNameOwner(@RequestBody payload: Map<String, String>): ResponseEntity<Transaction> {
        return try {
            val accountNameOwner = payload["accountNameOwner"]
            val guid = payload["guid"]
            logger.info("Changing transaction account for guid $guid to accountNameOwner $accountNameOwner")

            if (accountNameOwner.isNullOrBlank() || guid.isNullOrBlank()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Both accountNameOwner and guid are required")
            }

            val transactionResponse = standardizedTransactionService.changeAccountNameOwner(payload)
            logger.info("Transaction account updated successfully for guid $guid")
            ResponseEntity.ok(transactionResponse)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to change transaction account: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update transaction account: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT --data 'base64encodedimagedata' https://localhost:8443/transaction/update/receipt/image/da8a0a55-c4ef-44dc-9e5a-4cb7367a164f
    @PutMapping("/update/receipt/image/{guid}", produces = ["application/json"])
    fun updateTransactionReceiptImageByGuid(
        @PathVariable("guid") guid: String,
        @RequestBody payload: String
    ): ResponseEntity<ReceiptImage> {
        return try {
            logger.info("Updating receipt image for transaction: $guid")
            val receiptImage = standardizedTransactionService.updateTransactionReceiptImageByGuid(guid, payload)
            logger.info("Receipt image updated successfully for transaction: $guid")
            ResponseEntity.ok(receiptImage)
        } catch (ex: Exception) {
            logger.error("Failed to update receipt image for transaction $guid: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update receipt image: ${ex.message}", ex)
        }
    }

    /**
     * Legacy CRUD endpoint - DELETE /api/transaction/delete/{guid}
     * Original method name preserved for backward compatibility
     */
    @DeleteMapping("/delete/{guid}", produces = ["application/json"])
    fun deleteTransaction(@PathVariable("guid") guid: String): ResponseEntity<Transaction> {
        logger.debug("deleteTransaction() - Deleting transaction with guid = $guid (legacy endpoint)")

        // First get the transaction to return it
        val transaction = when (val findResult = standardizedTransactionService.findById(guid)) {
            is ServiceResult.Success -> findResult.data
            is ServiceResult.NotFound -> {
                logger.error("Transaction not found for deletion, guid = $guid")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found: $guid")
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving transaction for deletion $guid: ${findResult.exception.message}", findResult.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transaction: ${findResult.exception.message}", findResult.exception)
            }
            else -> {
                logger.error("Unexpected result type: $findResult")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred")
            }
        }

        return when (val result = standardizedTransactionService.deleteById(guid)) {
            is ServiceResult.Success -> {
                logger.info("Transaction deleted: ${transaction.guid}")
                ResponseEntity.ok(transaction)
            }
            is ServiceResult.NotFound -> {
                logger.error("Transaction not found for deletion, guid = $guid")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found: $guid")
            }
            is ServiceResult.SystemError -> {
                logger.error("Transaction not deleted, guid = $guid: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete transaction: ${result.exception.message}", result.exception)
            }
            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred")
            }
        }
    }

//    //curl --header "Content-Type: application/json" https://hornsup:8443/transaction/payment/required
//    @GetMapping("/payment/required", produces = ["application/json"])
//    fun selectPaymentRequired(): ResponseEntity<List<Account>> {
//
//        val accountNameOwners = transactionService.findAccountsThatRequirePayment()
//        if (accountNameOwners.isEmpty()) {
//            logger.error("no accountNameOwners found.")
//        }
//        return ResponseEntity.ok(accountNameOwners)
//    }

    // curl -k https://localhost:8443/transaction/category/ach
    @GetMapping("/category/{category_name}", produces = ["application/json"])
    fun selectTransactionsByCategory(@PathVariable("category_name") categoryName: String): ResponseEntity<List<Transaction>> {
        return try {
            logger.debug("Retrieving transactions for category: $categoryName")
            val transactions = standardizedTransactionService.findTransactionsByCategory(categoryName)
            if (transactions.isEmpty()) {
                logger.info("No transactions found for category: $categoryName")
            } else {
                logger.info("Retrieved ${transactions.size} transactions for category: $categoryName")
            }
            ResponseEntity.ok(transactions)
        } catch (ex: Exception) {
            logger.error("Failed to retrieve transactions for category $categoryName: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transactions by category: ${ex.message}", ex)
        }
    }

    // curl -k https://localhost:8443/transaction/description/amazon
    @GetMapping("/description/{description_name}", produces = ["application/json"])
    fun selectTransactionsByDescription(@PathVariable("description_name") descriptionName: String): ResponseEntity<List<Transaction>> {
        return try {
            logger.debug("Retrieving transactions for description: $descriptionName")
            val transactions = standardizedTransactionService.findTransactionsByDescription(descriptionName)
            if (transactions.isEmpty()) {
                logger.info("No transactions found for description: $descriptionName")
            } else {
                logger.info("Retrieved ${transactions.size} transactions for description: $descriptionName")
            }
            ResponseEntity.ok(transactions)
        } catch (ex: Exception) {
            logger.error("Failed to retrieve transactions for description $descriptionName: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve transactions by description: ${ex.message}", ex)
        }
    }
}
