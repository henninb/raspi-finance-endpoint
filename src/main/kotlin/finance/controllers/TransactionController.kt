package finance.controllers

import finance.domain.Account
import finance.domain.ReceiptImage
import finance.domain.Transaction
import finance.domain.TransactionState
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
class TransactionController @Autowired constructor(private var transactionService: TransactionService) :
    BaseController() {

    @Autowired
    lateinit var meterService: MeterService

    @GetMapping("/account/select/{accountNameOwner}", produces = ["application/json"])
    fun selectByAccountNameOwner(@PathVariable("accountNameOwner") accountNameOwner: String): ResponseEntity<Any> {
        return try {
            val transactions: List<Transaction> =
                transactionService.findByAccountNameOwnerOrderByTransactionDate(accountNameOwner)

            // If the account exists but has no transactions, return an empty list with 200 OK
            ResponseEntity.ok(transactions)
        } catch (e: Exception) {
            // Handle case where the account doesn't exist
            logger.error("Account not found: $accountNameOwner")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found: $accountNameOwner")
        }
    }


    //transaction-management/
    //accounts/{accountNameOwner}/transactions/totals
    //curl -k https://hornsup:8443/transaction/account/totals/chase_brian
    @GetMapping("/account/totals/{accountNameOwner}", produces = ["application/json"])
    fun selectTotalsCleared(@PathVariable("accountNameOwner") accountNameOwner: String): ResponseEntity<String> {
        val results: Map<String, BigDecimal> =
            transactionService.calculateActiveTotalsByAccountNameOwner(accountNameOwner)

        logger.info("totals=${results}")

        return ResponseEntity.ok(mapper.writeValueAsString(results))
    }


    //curl -k https://hornsup:8443/transaction/select/340c315d-39ad-4a02-a294-84a74c1c7ddc
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

    @PutMapping("/update/{guid}", consumes = ["application/json"], produces = ["application/json"])
    fun updateTransaction(
        @PathVariable("guid") guid: String,
        @RequestBody toBePatchedTransaction: Transaction
    ): ResponseEntity<Transaction> {
        val transactionResponse = transactionService.updateTransaction(toBePatchedTransaction)
        return ResponseEntity.ok(transactionResponse)
    }

    //TODO: return the payload of the updated and the inserted
    @PutMapping(
        "/state/update/{guid}/{transactionStateValue}",
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    fun updateTransactionState(
        @PathVariable("guid") guid: String,
        @PathVariable("transactionStateValue") transactionStateValue: String
    ): ResponseEntity<Transaction> {
        val newTransactionStateValue = transactionStateValue.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val transactionResponse =
            transactionService.updateTransactionState(guid, TransactionState.valueOf(newTransactionStateValue))
        return ResponseEntity.ok(transactionResponse)
    }

    //curl -k --header "Content-Type: application/json" 'https://hornsup:8443/transaction/insert' -X POST -d ''
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertTransaction(@RequestBody transaction: Transaction): ResponseEntity<Transaction> {
        return try {
            logger.info("Attempting to insert transaction: ${mapper.writeValueAsString(transaction)}")

            val transactionResponse = transactionService.insertTransaction(transaction)

            logger.info("Transaction inserted successfully: ${mapper.writeValueAsString(transactionResponse)}")
            ResponseEntity.ok(transactionResponse)
        } catch (ex: ResponseStatusException) {
            logger.error("Failed to insert transaction: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert transaction: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error occurred while inserting transaction: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    //TODO: 7/1/2021 - Return the transaction from the database
    //TODO: 6/28/2021 - Should return a 201 CREATED?
    //curl -k --header "Content-Type: application/json" 'https://hornsup:8443/transaction/future/insert' -X POST -d ''
    @PostMapping("/future/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertFutureTransaction(@RequestBody transaction: Transaction): ResponseEntity<Transaction> {
        val futureTransaction = transactionService.createFutureTransaction(transaction)
        logger.info("insert future - futureTransaction.transactionDate: $futureTransaction")
        val transactionResponse = transactionService.insertTransaction(futureTransaction)
        return ResponseEntity.ok(transactionResponse)
    }

    // change the account name owner of a given transaction
    @PutMapping("/update/account", consumes = ["application/json"], produces = ["application/json"])
    fun changeTransactionAccountNameOwner(@RequestBody payload: Map<String, String>): ResponseEntity<Transaction> {
        //TODO: need to complete action
        logger.info("value of accountNameOwner: " + payload["accountNameOwner"])
        logger.info("value of guid: " + payload["guid"])
        val transactionResponse = transactionService.changeAccountNameOwner(payload)
        logger.info("transaction account updated")
        return ResponseEntity.ok(transactionResponse)
    }

    // curl -k -X PUT 'https://hornsup:8443/transaction/update/receipt/image/da8a0a55-c4ef-44dc-9e5a-4cb7367a164f'  --header "Content-Type: application/json" -d 'test'
    @PutMapping("/update/receipt/image/{guid}", produces = ["application/json"])
    fun updateTransactionReceiptImageByGuid(
        @PathVariable("guid") guid: String,
        @RequestBody payload: String
    ): ResponseEntity<ReceiptImage> {
        val receiptImage = transactionService.updateTransactionReceiptImageByGuid(guid, payload)
        logger.info("set transaction receipt image for guid = $guid")
        //return ResponseEntity.ok("transaction receipt image updated")
        return ResponseEntity.ok(receiptImage)
    }

    //curl -k --header "Content-Type: application/json" -X DELETE 'https://hornsup:8443/transaction/delete/38739c5b-e2c6-41cc-82c2-d41f39a33f9a'
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

    //curl --header "Content-Type: application/json" https://hornsup:8443/transaction/payment/required
    @GetMapping("/payment/required", produces = ["application/json"])
    fun selectPaymentRequired(): ResponseEntity<List<Account>> {

        val accountNameOwners = transactionService.findAccountsThatRequirePayment()
        if (accountNameOwners.isEmpty()) {
            logger.error("no accountNameOwners found.")
        }
        return ResponseEntity.ok(accountNameOwners)
    }

    // curl -s -k --header "Content-Type: application/json" https://finance.lan/api/transaction/category/ach | jq
    @GetMapping("/category/{category_name}", produces = ["application/json"])
    fun selectTransactionsByCategory(@PathVariable("category_name") categoryName: String): ResponseEntity<List<Transaction>> {

        val categories = transactionService.findTransactionsByCategory(categoryName)
        if (categories.isEmpty()) {
            logger.error("no category detail found.")
        }
        return ResponseEntity.ok(categories)
    }

    // curl -s -k --header "Content-Type: application/json" https://finance.lan/api/transaction/category/ach | jq
    @GetMapping("/description/{description_name}", produces = ["application/json"])
    fun selectTransactionsByDescription(@PathVariable("description_name") descriptionName: String): ResponseEntity<List<Transaction>> {

        val descriptions = transactionService.findTransactionsByDescription(descriptionName)
        if (descriptions.isEmpty()) {
            logger.error("no category detail found.")
        }
        return ResponseEntity.ok(descriptions)
    }
}
