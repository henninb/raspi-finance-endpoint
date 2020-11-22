package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.services.TransactionService
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.util.*
import javax.validation.ConstraintViolationException
import javax.validation.ValidationException


//@CrossOrigin(origins = arrayOf("http://localhost:3000"))
@CrossOrigin
@RestController
@RequestMapping("/transaction")
class TransactionController @Autowired constructor(private var transactionService: TransactionService) {
    //curl https://hornsup:8080/transaction/account/select/usbankcash_brian
    @GetMapping(path = ["/account/select/{accountNameOwner}"], produces = ["application/json"])
    fun selectByAccountNameOwner(@PathVariable("accountNameOwner") accountNameOwner: String): ResponseEntity<List<Transaction>> {
        val transactions: List<Transaction> = transactionService.findByAccountNameOwnerIgnoreCaseOrderByTransactionDate(accountNameOwner)
        if (transactions.isEmpty()) {
            //TODO: not found, should I take this action?
            ResponseEntity.notFound().build<List<Transaction>>()
        }
        return ResponseEntity.ok(transactions)
    }

    //curl https://hornsup:8080/transaction/account/totals/chase_brian
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

        logger.info("guid not found = $guid")
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not deleted: $guid")
    }

    @PutMapping(path = ["/update/{guid}"], consumes = ["application/json"], produces = ["application/json"])
    fun updateTransaction(@PathVariable("guid") guid: String, @RequestBody transaction: Map<String, String>): ResponseEntity<String> {
        val toBePatchedTransaction = mapper.convertValue(transaction, Transaction::class.java)
        val updateStatus: Boolean = transactionService.updateTransaction(toBePatchedTransaction)
        if (updateStatus) {
            return ResponseEntity.ok("transaction put")
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not found and thus not updated: $guid")
    }

    @PutMapping(path = ["/state/update/{guid}/{state}"], consumes = ["application/json"], produces = ["application/json"])
    fun updateTransactionState(@PathVariable("guid") guid: String, @PathVariable("state") state: TransactionState): ResponseEntity<String> {
        val updateStatus: Boolean = transactionService.updateTransactionState(guid, state)
        if (updateStatus) {
            return ResponseEntity.ok("transaction state updated")
        }
        throw ResponseStatusException(HttpStatus.NOT_MODIFIED, "could not updated transaction.")
    }

    @PutMapping(path = ["/reoccurring/update/{guid}/{reoccurring}"], consumes = ["application/json"], produces = ["application/json"])
    fun updateTransactionReoccurringState(@PathVariable("guid") guid: String, @PathVariable("reoccurring") reoccurring: Boolean): ResponseEntity<String> {
        val updateStatus: Boolean = transactionService.updateTransactionReoccurringState(guid, reoccurring)
        if (updateStatus) {
            return ResponseEntity.ok("transaction reoccurring updated")
        }
        throw ResponseStatusException(HttpStatus.NOT_MODIFIED, "could not updated transaction.")
    }

    //TODO: should return a 201 CREATED
    //curl --header "Content-Type: application/json" https://hornsup:8080/transaction/insert -X POST -d ''
    @PostMapping(path = ["/insert"], consumes = ["application/json"], produces = ["application/json"])
    fun insertTransaction(@RequestBody transaction: Transaction): ResponseEntity<String> {
        logger.info("insert - transaction.transactionDate: $transaction")
        if (transactionService.insertTransaction(transaction)) {
            logger.info(transaction.toString())
            return ResponseEntity.ok("transaction inserted")
        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not insert transaction.")
    }

    @PutMapping(path = ["/update/account"], consumes = ["application/json"], produces = ["application/json"])
    fun updateAccountByGuid(@RequestBody payload: Map<String, String>): ResponseEntity<String> {
        //TODO: need to complete action
        logger.info("value of accountNameOwner: " + payload["accountNameOwner"])
        logger.info("value of guid: " + payload["guid"])
        transactionService.changeAccountNameOwner(payload)
        logger.info("transaction account updated")

        return ResponseEntity.ok("transaction account updated")
    }

    // curl -k -X PUT 'https://hornsup:8080/transaction/update/receipt/image/da8a0a55-c4ef-44dc-9e5a-4cb7367a164f'  --header "Content-Type: application/json" -d 'test'
    @PutMapping(path = ["/update/receipt/image/{guid}"], produces = ["application/json"])
    fun updateTransactionReceiptImageByGuid(@PathVariable("guid") guid: String, @RequestBody payload: String): ResponseEntity<String> {
        transactionService.updateTransactionReceiptImageByGuid(guid, payload.toByteArray())
        logger.info("set transaction receipt image for guid = $guid")
        return ResponseEntity.ok("transaction receipt image updated")
    }

    //curl -s -X POST https://hornsup:8080/transaction/clone -d '{"guid":"458a619e-b035-4b43-b406-96b8b2ae7340", "transactionDate":"2020-11-30", "amount":0.00}' -H "Content-Type: application/json"
    @PostMapping(path = ["/clone"], consumes = ["application/json"], produces = ["application/json"])
    fun cloneTransaction(@RequestBody payload: Map<String, String>): ResponseEntity<String> {
        if (transactionService.cloneAsMonthlyTransaction(payload)) {
            return ResponseEntity.ok("transaction inserted")
        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not insert transaction.")
    }

    //curl --header "Content-Type: application/json" -X DELETE https://hornsup:8080/transaction/delete/38739c5b-e2c6-41cc-82c2-d41f39a33f9a
    //curl --header "Content-Type: application/json" -X DELETE https://hornsup:8080/transaction/delete/00000000-e2c6-41cc-82c2-d41f39a33f9a
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

    //curl --header "Content-Type: application/json" https://hornsup:8080/transaction/insert -X POST -d '{"accountType":"Credit"}'
    //curl --header "Content-Type: application/json" https://hornsup:8080/transaction/insert -X POST -d '{"amount":"abc"}'
    @ResponseStatus(HttpStatus.BAD_REQUEST) //400
    @ExceptionHandler(value = [ConstraintViolationException::class, NumberFormatException::class, EmptyResultDataAccessException::class,
        MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class, HttpMediaTypeNotSupportedException::class,
        IllegalArgumentException::class, DataIntegrityViolationException::class, ValidationException::class])
    fun handleBadHttpRequests(throwable: Throwable): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        logger.info("Bad Request: ", throwable)
        response["response"] = "BAD_REQUEST: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        logger.info(response.toString())
        return response
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = [ResponseStatusException::class])
    fun handleHttpNotFound(throwable: Throwable): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        logger.error("not found: ", throwable)
        response["response"] = "NOT_FOUND: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        return response
    }

    @ResponseStatus(HttpStatus.NOT_MODIFIED)
    //@ExceptionHandler(value = [EmptyTransactionException::class])
    fun handleHttpNotModified(throwable: Throwable): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        logger.error("not modified: ", throwable)
        response["response"] = "NOT_MODIFIED: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        return response
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = [Exception::class])
    fun handleHttpInternalError(throwable: Throwable): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        logger.error("internal server error: ", throwable)
        response["response"] = "INTERNAL_SERVER_ERROR: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        logger.info("response: $response")
        return response
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}
