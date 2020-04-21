package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Transaction
import finance.exceptions.EmptyTransactionException
import finance.services.TransactionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.math.BigDecimal
import java.util.*
import javax.validation.ConstraintViolationException
import javax.validation.constraints.Max
import javax.validation.constraints.Min


//@CrossOrigin(origins = arrayOf("http://localhost:3000"))
@CrossOrigin
//Thymeleaf - @RestController is for JSON; @Controller is for HTML
@RestController
@RequestMapping("/transaction")
open class TransactionController @Autowired constructor(private var transactionService: TransactionService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping(path = [("/all")])
    fun findAllTransactions() : ResponseEntity<List<Transaction>> {
        val transactions: List<Transaction> = transactionService.findAllTransactions()
        if( transactions.isEmpty() ) {
            ResponseEntity.notFound().build<List<Transaction>>()
        }
        return ResponseEntity.ok(transactions)
    }

    //curl http://localhost:8080/transaction/page/all?page=1&per_page=1
    @GetMapping(path = [("/page/all")])
    fun findAllTransactionsPageable( @RequestParam @Min(value = 1, message = "Page number must be an integer greater than 1.")
                                          page: Int,
                                     @RequestParam(value = "per_page") @Max(value = 1000, message = "Page size limit is 1000, change page size value")
                                          perPage: Int) : ResponseEntity<List<Transaction>> {

        val transactions: List<Transaction> = transactionService.findAllTransactions(PageRequest.of(page -1, perPage))
        if( transactions.isEmpty() ) {
            ResponseEntity.notFound().build<List<Transaction>>()
        }
        return ResponseEntity.ok(transactions)
    }

    //curl http://localhost:8080/transaction/account/select/usbankcash_brian
    @GetMapping(path = ["/account/select/{accountNameOwner}"])
    fun selectByAccountNameOwner(@PathVariable("accountNameOwner") accountNameOwner: String): ResponseEntity<List<Transaction>> {
        val transactions: List<Transaction> = transactionService.findByAccountNameOwnerIgnoreCaseOrderByTransactionDate(accountNameOwner)
        if( transactions.isEmpty() ) {
            ResponseEntity.notFound().build<List<Transaction>>()
        }
        return ResponseEntity.ok(transactions)
    }

    //curl http://localhost:8080/transaction/account/totals/chase_brian
    @GetMapping(path = ["/account/totals/{accountNameOwner}"])
    fun selectTotalsCleared(@PathVariable("accountNameOwner") accountNameOwner: String): ResponseEntity<String> {
        val totals: Map<String, BigDecimal> = transactionService.getTotalsByAccountNameOwner(accountNameOwner)

        //val response : Map<String, String> = totals
        logger.info("totals=${totals}")

        return ResponseEntity.ok(mapper.writeValueAsString(totals))
    }

    //curl http://localhost:8080/transaction/select/340c315d-39ad-4a02-a294-84a74c1c7ddc
    @GetMapping(path = ["/select/{guid}"])
    fun findTransaction(@PathVariable("guid") guid: String): ResponseEntity<Transaction> {
        logger.debug("guid = $guid")
        val transactionOption: Optional<Transaction> = transactionService.findByGuid(guid)
        if( transactionOption.isPresent ) {
            val transaction: Transaction = transactionOption.get()
            return ResponseEntity.ok(transaction)
        }

        logger.info("guid not found = $guid")
        //return ResponseEntity.notFound().build()  //404
        throw EmptyTransactionException("Cannot find transaction.")
    }

    //curl --header "Content-Type: application/json-patch+json" -X PATCH -d '{"guid":"9b9aea08-0dc2-4720-b20c-00b0df6af8ce", "description":"new"}' http://localhost:8080/transaction/update/9b9aea08-0dc2-4720-b20c-00b0df6af8ce
    //curl --header "Content-Type: application/json-patch+json" -X PATCH -d '{"guid":"a064b942-1e78-4913-adb3-b992fc1b4dd3","sha256":"","accountType":"credit","accountNameOwner":"discover_brian","description":"Last Updated","category":"","notes":"","cleared":0,"reoccurring":false,"amount":"0.00","transactionDate":1512730594,"dateUpdated":1487332021,"dateAdded":1487332021}' http://localhost:8080/transaction/update/a064b942-1e78-4913-adb3-b992fc1b4dd3
    @PatchMapping(path = [("/update/{guid}")], consumes = [("application/json-patch+json")], produces = [("application/json")])
    fun updateTransaction(@RequestBody transaction: Map<String, String>): ResponseEntity<String> {
        val toBePatchedTransaction = mapper.convertValue(transaction, Transaction::class.java)
        val updateStatus: Boolean = transactionService.patchTransaction(toBePatchedTransaction)
        if( updateStatus ) {
            return ResponseEntity.ok("transaction updated")
        }
        return ResponseEntity.notFound().build()
    }

    //curl --header "Content-Type: application/json" http://localhost:8080/transaction/insert -X POST -d ''
    @PostMapping(path = [("/insert")], consumes = [("application/json")], produces = [("application/json")])
    fun insertTransaction(@RequestBody transaction: Transaction) : ResponseEntity<String> {
        logger.info("insert - transaction.transactionDate: ${transaction.toString()}")
        if (transactionService.insertTransaction(transaction) ) {
            logger.info(transaction.toString())
            return ResponseEntity.ok("transaction inserted")
        }
        return ResponseEntity.notFound().build()
    }

    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8080/transaction/delete/38739c5b-e2c6-41cc-82c2-d41f39a33f9a
    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8080/transaction/delete/00000000-e2c6-41cc-82c2-d41f39a33f9a
    @DeleteMapping(path = ["/delete/{guid}"])
    fun deleteTransaction(@PathVariable("guid") guid: String): ResponseEntity<String> {
        val transactionOption: Optional<Transaction> = transactionService.findByGuid(guid)
        if( transactionOption.isPresent ) {
            if( transactionService.deleteByGuid(guid) ) {
                return ResponseEntity.ok("resource deleted")
            }
            throw EmptyTransactionException("transaction not deleted.")
        }
        throw EmptyTransactionException("Cannot find transaction during delete.")
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST) //400
    @ExceptionHandler(value = [ConstraintViolationException::class, NumberFormatException::class, MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class])
    fun handleBadHttpRequests(throwable: Throwable): Map<String, String>? {
        val response: MutableMap<String, String> = HashMap()
        logger.error("Bad Request", throwable)
        response["response"] = "BAD_REQUEST: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        return response
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = [Exception::class])
    open fun handleHttpInternalError(throwable: Throwable): Map<String, String>? {
        val response: MutableMap<String, String> = HashMap()
        logger.error("internal server error: ", throwable)
        response["response"] = "INTERNAL_SERVER_ERROR: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        return response
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = [EmptyTransactionException::class])
    open fun handleHttpNotFound(throwable: Throwable): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        logger.error("not found: ", throwable)
        response["response"] = "NOT_FOUND: " + throwable.javaClass.simpleName  + " , message: " + throwable.message
        return response
    }

    companion object {
        private val mapper = ObjectMapper()
    }
}
