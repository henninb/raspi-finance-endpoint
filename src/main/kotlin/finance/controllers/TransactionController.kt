package finance.controllers

import finance.domain.*
import finance.services.MeterService
import finance.services.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/transaction", "/api/transaction")
class TransactionController(private val transactionService: TransactionService, private val meterService: MeterService) :
    BaseController() {

    // curl -k https://localhost:8443/transaction/account/select/chase_brian
    @GetMapping("/account/select/{accountNameOwner}", produces = ["application/json"])
    fun selectByAccountNameOwner(@PathVariable("accountNameOwner") accountNameOwner: String): ResponseEntity<List<Transaction>> {
        return try {
            logger.debug("Retrieving transactions for account: $accountNameOwner")
            val transactions: List<Transaction> =
                transactionService.findByAccountNameOwnerOrderByTransactionDate(accountNameOwner)
            
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
                transactionService.calculateActiveTotalsByAccountNameOwner(accountNameOwner)
            
            logger.info("Calculated totals for account $accountNameOwner: $results")
            ResponseEntity.ok(results)
        } catch (ex: Exception) {
            logger.error("Failed to calculate totals for account $accountNameOwner: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to calculate account totals: ${ex.message}", ex)
        }
    }


    // curl -k https://localhost:8443/transaction/select/340c315d-39ad-4a02-a294-84a74c1c7ddc
    @GetMapping("/select/{guid}", produces = ["application/json"])
    fun findTransaction(@PathVariable("guid") guid: String): ResponseEntity<Transaction> {
        logger.debug("findTransaction() - Searching for transaction with guid = $guid")

        // Use orElseThrow to avoid Optional.get() and throw a proper exception if not found
        val transaction = transactionService.findTransactionByGuid(guid)
            .orElseThrow {
                logger.error("Transaction not found, guid = $guid")
                meterService.incrementTransactionRestSelectNoneFoundCounter("unknown")
                ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found, guid: $guid")
            }

        // Return the transaction with 200 OK status
        return ResponseEntity.ok(transaction)
    }

    // curl -k --header "Content-Type: application/json" --request PUT --data '{"guid":"340c315d-39ad-4a02-a294-84a74c1c7ddc", "description":"updated description", "amount": 100.00}' https://localhost:8443/transaction/update/340c315d-39ad-4a02-a294-84a74c1c7ddc
    @PutMapping("/update/{guid}", consumes = ["application/json"], produces = ["application/json"])
    fun updateTransaction(
        @PathVariable("guid") guid: String,
        @RequestBody toBePatchedTransaction: Transaction
    ): ResponseEntity<Transaction> {
        return try {
            logger.info("Updating transaction: $guid")
            val transactionResponse = transactionService.updateTransaction(toBePatchedTransaction)
            logger.info("Transaction updated successfully: $guid")
            ResponseEntity.ok(transactionResponse)
        } catch (ex: Exception) {
            logger.error("Failed to update transaction $guid: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update transaction: ${ex.message}", ex)
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
                transactionService.updateTransactionState(guid, TransactionState.valueOf(newTransactionStateValue))
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

    // curl -k --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "description":"test transaction", "category":"misc", "amount": 10.00, "transactionDate":"2024-01-01", "transactionState":"cleared"}' https://localhost:8443/transaction/insert
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertTransaction(@RequestBody transaction: Transaction): ResponseEntity<Transaction> {
        return try {
            logger.info("Attempting to insert transaction: ${mapper.writeValueAsString(transaction)}")

            val transactionResponse = transactionService.insertTransaction(transaction)

            logger.info("Transaction inserted successfully: ${mapper.writeValueAsString(transactionResponse)}")
            ResponseEntity(transactionResponse, HttpStatus.CREATED)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to insert transaction due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate transaction found.")
        } catch (ex: ResponseStatusException) {
            logger.error("Failed to insert transaction: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert transaction: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error occurred while inserting transaction: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "description":"future transaction", "category":"misc", "amount": 15.00, "reoccurringType":"monthly"}' https://localhost:8443/transaction/future/insert
    @PostMapping("/future/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertFutureTransaction(@RequestBody transaction: Transaction): ResponseEntity<Transaction> {
        return try {
            logger.info("Inserting future transaction for account: ${transaction.accountNameOwner}")
            val futureTransaction = transactionService.createFutureTransaction(transaction)
            logger.debug("Created future transaction with date: ${futureTransaction.transactionDate}")
            val transactionResponse = transactionService.insertTransaction(futureTransaction)
            logger.info("Future transaction inserted successfully: ${transactionResponse.guid}")
            ResponseEntity(transactionResponse, HttpStatus.CREATED)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to insert future transaction due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate future transaction found.")
        } catch (ex: ResponseStatusException) {
            logger.error("Failed to insert future transaction: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert future transaction: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error inserting future transaction: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
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
            
            val transactionResponse = transactionService.changeAccountNameOwner(payload)
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
            val receiptImage = transactionService.updateTransactionReceiptImageByGuid(guid, payload)
            logger.info("Receipt image updated successfully for transaction: $guid")
            ResponseEntity.ok(receiptImage)
        } catch (ex: Exception) {
            logger.error("Failed to update receipt image for transaction $guid: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update receipt image: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request DELETE https://localhost:8443/transaction/delete/38739c5b-e2c6-41cc-82c2-d41f39a33f9a
    @DeleteMapping("/delete/{guid}", produces = ["application/json"])
    fun deleteTransaction(@PathVariable("guid") guid: String): ResponseEntity<Transaction> {
        val transaction = transactionService.findTransactionByGuid(guid)
            .orElseThrow {
                logger.error("Transaction not found for deletion, guid = $guid")
                ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found: $guid")
            }

        // Attempt to delete the transaction
        if (transactionService.deleteTransactionByGuid(guid)) {
            logger.info("Transaction deleted: ${transaction.guid}")
            return ResponseEntity.ok(transaction)
        }

        // In case the deletion fails
        logger.error("Transaction not deleted, guid = $guid")
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not deleted: $guid")
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
            val transactions = transactionService.findTransactionsByCategory(categoryName)
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
            val transactions = transactionService.findTransactionsByDescription(descriptionName)
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
