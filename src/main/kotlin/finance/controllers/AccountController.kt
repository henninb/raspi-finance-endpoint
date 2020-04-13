package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.services.AccountService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/account")
//@Validated
open class AccountController @Autowired constructor(private var accountService: AccountService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping(path = ["/account/select/active"])
    fun selectAllActiveAccounts(): ResponseEntity<List<Account>> {
        val accounts : List<Account> = accountService.findAllActiveAccounts()
        if( accounts.isEmpty() ) {
            logger.info("no accounts found.")
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(accounts)
    }

    @GetMapping(path = ["/account/select/all"])
    fun selectAllAccounts(): ResponseEntity<List<Account>> {
        val accounts : List<Account> = accountService.findAllOrderByAccountNameOwner()
        if( accounts.isEmpty() ) {
            logger.info("no accounts found.")
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(accounts)
    }

    @GetMapping(path = ["/account/select/{accountNameOwner}"])
    fun selectByAccountNameOwner(@PathVariable accountNameOwner: String): ResponseEntity<Account> {
        val accountOptional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)
        if( accountOptional.isPresent) {
            return ResponseEntity.ok(accountOptional.get())
        }
        return ResponseEntity.notFound().build()
    }

    //curl --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": "true","moniker": "0000"' http://localhost:8080/insert_account
    //curl --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": "true","moniker": "0000", "totals": 0.00, "totalsBalanced": 0.00, "dateClosed": 0, "dateUpdated": 0, "dateAdded": 0}' http://localhost:8080/insert_account
    //http://localhost:8080/insert_account
    @PostMapping(path = ["/account/insert"])
    fun insertAccount(@RequestBody account: Account) : ResponseEntity<String> {
        accountService.insertAccount(account)
        return ResponseEntity.ok("account inserted")
    }

    //http://localhost:8080/delete_account/amex_brian
    //curl --header "Content-Type: application/json" --request DELETE http://localhost:8080/delete_account/test_brian
    @DeleteMapping(path = ["/account/delete/{accountNameOwner}"])
    fun deleteByAccountNameOwner(@PathVariable accountNameOwner: String): ResponseEntity<String> {
        val accountOptional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)

        if(accountOptional.isPresent ) {
            accountService.deleteByAccountNameOwner(accountNameOwner)
            return ResponseEntity.ok("account deleted")
        }
        return ResponseEntity.notFound().build() //404
    }

    @PatchMapping(path = ["/account/update"])
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
