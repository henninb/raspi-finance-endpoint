package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Transaction
import finance.domain.Totals
import finance.services.TransactionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import java.util.*

//@CrossOrigin(origins = arrayOf("http://localhost:3000"))
@CrossOrigin
//Thymeleaf - @RestController is for JSON; @Controller is for HTML
@RestController
@RequestMapping("/transaction")
class TransactionController {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    lateinit var transactionService: TransactionService

    //curl http://localhost:8080/transaction/account/select/usbankcash_brian
    @GetMapping(path = [("/account/select/{accountNameOwner}")])
    fun selectByAccountNameOwner(@PathVariable accountNameOwner: String): ResponseEntity<List<Transaction>> {
        val transactions: List<Transaction> = transactionService.findByAccountNameOwnerIgnoreCaseOrderByTransactionDate(accountNameOwner)
        if( transactions.isEmpty() ) {
            ResponseEntity.notFound().build<List<Transaction>>()
        }
        return ResponseEntity.ok(transactions)
    }

    //curl http://localhost:8080/transaction/account/totals/chase_brian
    @GetMapping(path = [("/account/totals/{accountNameOwner}")])
    fun selectTotalsCleared(@PathVariable accountNameOwner: String): ResponseEntity<String> {
        val totals: Totals = transactionService.getTotalsByAccountNameOwner(accountNameOwner)

        return ResponseEntity.ok(mapper.writeValueAsString(totals))
    }

    //curl http://localhost:8080/transaction/select/340c315d-39ad-4a02-a294-84a74c1c7ddc
    @GetMapping(path = [("/select/{guid}")])
    fun findTransaction(@PathVariable guid: String): ResponseEntity<Transaction> {
        println("guid = $guid")
        val transactionOption: Optional<Transaction> = transactionService.findByGuid(guid)
        if( transactionOption.isPresent ) {
            val transaction: Transaction = transactionOption.get()
            return ResponseEntity.ok(transaction)
        }

        logger.info("guid not found = $guid")
        return ResponseEntity.notFound().build()  //404
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
        if (transactionService.insertTransaction(transaction) ) {
            return ResponseEntity.ok("transaction inserted")
        }
        return ResponseEntity.notFound().build()
    }

    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8080/transaction/delete/38739c5b-e2c6-41cc-82c2-d41f39a33f9a
    @DeleteMapping(path = [("/delete/{guid}")])
    fun deleteTransaction(@PathVariable guid: String): ResponseEntity<String> {
        val transactionOption: Optional<Transaction> = transactionService.findByGuid(guid)
        if( transactionOption.isPresent ) {
            if( transactionService.deleteByGuid(guid) ) {
                return ResponseEntity.ok("resource deleted")
            }
            return ResponseEntity.noContent().build()
        }
        return ResponseEntity.notFound().build() //404
    }

    companion object {
        private val mapper = ObjectMapper()
    }
}
