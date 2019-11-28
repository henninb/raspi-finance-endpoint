package controllers


import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.services.AccountService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

//@CrossOrigin(origins = arrayOf("http://localhost:3000"))
@CrossOrigin
@RestController
//@RequestMapping("/account")
class AccountController {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    lateinit var accountService: AccountService

    @GetMapping(path = [("/select_accounts")])
    fun selectAllActiveAccounts(): ResponseEntity<List<Account>> {
        val accounts : List<Account> = accountService.findAllActiveAccounts()
        if( accounts.isEmpty() ) {
            logger.info("no accounts found.")
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(accounts)
    }

    @GetMapping(path = [("/select_all")])
    fun select_all_accounts(): ResponseEntity<List<Account>> {
        val accounts : List<Account> = accountService.findAllOrderByAccountNameOwner()
        if( accounts.isEmpty() ) {
            logger.info("no accounts found.")
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(accounts)
    }

    @GetMapping(path = [("/select_account/{accountNameOwner}")])
    fun select_account(@PathVariable accountNameOwner: String): ResponseEntity<Account> {
        val accountOptional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)
        if( accountOptional.isPresent) {
            return ResponseEntity.ok(accountOptional.get())
        }
        return ResponseEntity.notFound().build()
    }

    //curl --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": "true","moniker": "0000"' http://localhost:8080/insert_account
    //curl --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": "true","moniker": "0000", "totals": 0.00, "totalsBalanced": 0.00, "dateClosed": 0, "dateUpdated": 0, "dateAdded": 0}' http://localhost:8080/insert_account
    //http://localhost:8080/insert_account
    @PostMapping(path = [("/insert_account")], consumes = [("application/json")], produces = [("application/json")])
    fun insert_account(@RequestBody account: Account) : ResponseEntity<String> {
        accountService.insertAccount(account)
        return ResponseEntity.ok("account inserted")
    }

    //http://localhost:8080/delete_account/amex_brian
    //curl --header "Content-Type: application/json" --request DELETE http://localhost:8080/delete_account/test_brian
    @DeleteMapping(path = [("/delete_account/{accountNameOwner}")])
    fun delete_account(@PathVariable accountNameOwner: String): ResponseEntity<String> {
        val accountOptional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)

        if(accountOptional.isPresent ) {
            accountService.deleteByAccountNameOwner(accountNameOwner)
            return ResponseEntity.ok("account deleted")
        }
        return ResponseEntity.notFound().build() //404
    }

    @PatchMapping(path = [("/update_account/{accountNameOwner}")], consumes = [("application/json-patch+json")], produces = [("application/json")])
    fun updateTransaction(@RequestBody account: Map<String, String>): ResponseEntity<String> {
        val toBePatchedTransaction = mapper.convertValue(account, Account::class.java)
        val updateStatus: Boolean = accountService.patchAccount(toBePatchedTransaction)
        if( updateStatus ) {
            return ResponseEntity.ok("account updated")
        }
        return ResponseEntity.notFound().build()
    }

    companion object {
        private val mapper = ObjectMapper()
    }
}
