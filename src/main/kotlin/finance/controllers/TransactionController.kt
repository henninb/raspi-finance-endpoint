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
@RequestMapping("/transaction")
class TransactionController @Autowired constructor(private var transactionService: TransactionService) :
    BaseController() {

    @Autowired
    lateinit var meterService: MeterService

    //curl https://hornsup:8080/transaction/account/select/usbankcash_brian
    @GetMapping(path = ["/account/select/{accountNameOwner}"], produces = ["application/json"])
    fun selectByAccountNameOwner(@PathVariable("accountNameOwner") accountNameOwner: String): ResponseEntity<List<Transaction>> {
        val transactions: List<Transaction> =
            transactionService.findByAccountNameOwnerOrderByTransactionDate(accountNameOwner)
        if (transactions.isEmpty()) {
            logger.error("transactions.size=${transactions.size}")
            //TODO: not found, should I take this action?
            ResponseEntity.notFound().build<List<Transaction>>()
        }
        return ResponseEntity.ok(transactions)
    }

    //transaction-management/
    //accounts/{accountNameOwner}/transactions/totals
    //curl -k https://hornsup:8080/transaction/account/totals/chase_brian
    @GetMapping(path = ["/account/totals/{accountNameOwner}"], produces = ["application/json"])
    fun selectTotalsCleared(@PathVariable("accountNameOwner") accountNameOwner: String): ResponseEntity<String> {
        val results: Map<String, BigDecimal> = transactionService.fetchTotalsByAccountNameOwner(accountNameOwner)

        logger.info("totals=${results}")

        return ResponseEntity.ok(mapper.writeValueAsString(results))
    }

    //curl -k https://hornsup:8080/transaction/select/340c315d-39ad-4a02-a294-84a74c1c7ddc
    @GetMapping(path = ["/select/{guid}"], produces = ["application/json"])
    fun findTransaction(@PathVariable("guid") guid: String): ResponseEntity<Transaction> {
        logger.debug("findTransaction() guid = $guid")
        val transactionOption: Optional<Transaction> = transactionService.findTransactionByGuid(guid)
        if (transactionOption.isPresent) {
            val transaction: Transaction = transactionOption.get()
            return ResponseEntity.ok(transaction)
        }

        logger.error("Transaction not found, guid = $guid")
        meterService.incrementTransactionRestSelectNoneFoundCounter("unknown")
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found, guid: $guid")
    }

    //TODO: 2021-01-10, return the payload of the updated and the inserted
    //TODO: 2021-01-10, consumes JSON should be turned back on
    @PutMapping(path = ["/update/{guid}"], produces = ["application/json"])
    //@PutMapping(path = ["/update/{guid}"], consumes = ["application/json"], produces = ["application/json"])
    fun updateTransaction(
        @PathVariable("guid") guid: String,
        @RequestBody transaction: Map<String, Any>
    ): ResponseEntity<String> {
        val toBePatchedTransaction = mapper.convertValue(transaction, Transaction::class.java)
        val updateStatus: Boolean = transactionService.updateTransaction(toBePatchedTransaction)
        if (updateStatus) {
            return ResponseEntity.ok("transaction updated")
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not found and thus not updated: $guid")
    }

    //TODO: return the payload of the updated and the inserted
    @PutMapping(
        path = ["/state/update/{guid}/{state}"],
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    fun updateTransactionState(
        @PathVariable("guid") guid: String,
        @PathVariable("state") state: TransactionState
    ): ResponseEntity<String> {
        val transactions = transactionService.updateTransactionState(guid, state)
        if (transactions.isNotEmpty()) {
            val response: MutableMap<String, String> = HashMap()
            response["message"] = "updated transactionState"
            response["transactions"] = transactions.toString()
            return ResponseEntity.ok(mapper.writeValueAsString(response))
        }
        logger.error("The transaction guid = $guid could not be updated for transaction state.")
        meterService.incrementTransactionRestTransactionStateUpdateFailureCounter("unknown")
        throw ResponseStatusException(
            HttpStatus.NOT_MODIFIED,
            "The transaction guid = $guid could not be updated for transaction state."
        )
    }

    @PutMapping(
        path = ["/reoccurring/update/{guid}/{reoccurring}"],
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    fun updateTransactionReoccurringState(
        @PathVariable("guid") guid: String,
        @PathVariable("reoccurring") reoccurring: Boolean
    ): ResponseEntity<String> {
        val updateStatus: Boolean = transactionService.updateTransactionReoccurringFlag(guid, reoccurring)
        if (updateStatus) {
            return ResponseEntity.ok("transaction reoccurring updated")
        }
        logger.error("The transaction guid = $guid could not be updated for reoccurring state.")
        meterService.incrementTransactionRestReoccurringStateUpdateFailureCounter("unknown")
        throw ResponseStatusException(HttpStatus.NOT_MODIFIED, "could not updated transaction for reoccurring state.")
    }

    //TODO: should return a 201 CREATED
    //curl -k --header "Content-Type: application/json" 'https://hornsup:8080/transaction/insert' -X POST -d ''
    @PostMapping(path = ["/insert"], consumes = ["application/json"], produces = ["application/json"])
    fun insertTransaction(@RequestBody transaction: Transaction): ResponseEntity<String> {
        logger.info("insert - transaction.transactionDate: $transaction")
        if (transactionService.insertTransaction(transaction)) {
            logger.info(transaction.toString())
            return ResponseEntity.ok("transaction inserted")
        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not insert transaction.")
    }

    // change the account name owner of a given transaction
    @PutMapping(path = ["/update/account"], consumes = ["application/json"], produces = ["application/json"])
    fun changeTransactionAccountNameOwner(@RequestBody payload: Map<String, String>): ResponseEntity<String> {
        //TODO: need to complete action
        logger.info("value of accountNameOwner: " + payload["accountNameOwner"])
        logger.info("value of guid: " + payload["guid"])
        transactionService.changeAccountNameOwner(payload)
        logger.info("transaction account updated")

        return ResponseEntity.ok("transaction account updated")
    }

    // curl -k -X PUT 'https://hornsup:8080/transaction/update/receipt/image/da8a0a55-c4ef-44dc-9e5a-4cb7367a164f'  --header "Content-Type: application/json" -d 'test'
    @PutMapping(path = ["/update/receipt/image/{guid}"], produces = ["application/json"])
    fun updateTransactionReceiptImageByGuid(
        @PathVariable("guid") guid: String,
        @RequestBody payload: String
    ): ResponseEntity<ReceiptImage> {
        val receiptImage = transactionService.updateTransactionReceiptImageByGuid(guid, payload)
        logger.info("set transaction receipt image for guid = $guid")
        //return ResponseEntity.ok("transaction receipt image updated")
        return ResponseEntity.ok(receiptImage)
    }

    //curl -k --header "Content-Type: application/json" -X DELETE 'https://hornsup:8080/transaction/delete/38739c5b-e2c6-41cc-82c2-d41f39a33f9a'
    @DeleteMapping(path = ["/delete/{guid}"], produces = ["application/json"])
    fun deleteTransaction(@PathVariable("guid") guid: String): ResponseEntity<String> {
        val transactionOption: Optional<Transaction> = transactionService.findTransactionByGuid(guid)
        if (transactionOption.isPresent) {
            if (transactionService.deleteTransactionByGuid(guid)) {
                return ResponseEntity.ok("resource deleted")
            }
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not deleted: $guid")
            //return ResponseEntity.badRequest("")
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not deleted: $guid")
    }

    //curl --header "Content-Type: application/json" https://hornsup:8080/transaction/payment/required
    @GetMapping(path = ["/payment/required"], produces = ["application/json"])
    fun selectPaymentRequired(): ResponseEntity<List<Account>> {

        val accountNameOwners = transactionService.findAccountsThatRequirePayment()
        if (accountNameOwners.isEmpty()) {
            logger.error("no accountNameOwners found.")
        }
        return ResponseEntity.ok(accountNameOwners)
    }
}
