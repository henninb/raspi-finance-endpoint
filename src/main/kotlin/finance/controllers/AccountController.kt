package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.exceptions.EmptyAccountException
import finance.services.AccountService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.*
import javax.validation.ConstraintViolationException

@CrossOrigin
@RestController
@RequestMapping("/account")
//@Validated
open class AccountController @Autowired constructor(private var accountService: AccountService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping(path = ["/select/active"])
    fun selectAllActiveAccounts(): ResponseEntity<List<Account>> {
        val accounts : List<Account> = accountService.findAllActiveAccounts()
        if( accounts.isEmpty() ) {
            logger.info("no accounts found.")
            return ResponseEntity.notFound().build()
        }
        logger.info("select active accounts: ${accounts.size}")
        return ResponseEntity.ok(accounts)
    }

//    @GetMapping(path = ["/select/all"])
//    fun selectAllAccounts(): ResponseEntity<List<Account>> {
//        val accounts : List<Account> = accountService.findAllOrderByAccountNameOwner()
//        if( accounts.isEmpty() ) {
//            logger.info("no accounts found.")
//            return ResponseEntity.notFound().build()
//        }
//        logger.info("count of all accounts: ${accounts.size}")
//        logger.info("json of all accounts: ${accounts}")
//        return ResponseEntity.ok(accounts)
//    }

    @GetMapping(path = ["/select/{accountNameOwner}"])
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
    @PostMapping(path = ["/insert"])
    fun insertAccount(@RequestBody account: Account) : ResponseEntity<String> {
        accountService.insertAccount(account)
        return ResponseEntity.ok("account inserted")
    }

    //http://localhost:8080/delete_account/amex_brian
    //curl --header "Content-Type: application/json" --request DELETE http://localhost:8080/delete_account/test_brian
    @DeleteMapping(path = ["/delete/{accountNameOwner}"])
    fun deleteByAccountNameOwner(@PathVariable accountNameOwner: String): ResponseEntity<String> {
        val accountOptional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)

        if(accountOptional.isPresent ) {
            accountService.deleteByAccountNameOwner(accountNameOwner)
            return ResponseEntity.ok("account deleted")
        }
        throw EmptyAccountException("account not deleted.")
    }

    @PatchMapping(path = ["/update"])
    fun updateTransaction(@RequestBody account: Map<String, String>): ResponseEntity<String> {
        val toBePatchedTransaction = mapper.convertValue(account, Account::class.java)
        val updateStatus: Boolean = accountService.patchAccount(toBePatchedTransaction)
        if( updateStatus ) {
            return ResponseEntity.ok("account updated")
        }

        throw EmptyAccountException("account not updated.")
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST) //400
    @ExceptionHandler(value = [ConstraintViolationException::class, NumberFormatException::class, MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class])
    fun handleBadHttpRequests(throwable: Throwable): Map<String, String>? {
        val response: MutableMap<String, String> = HashMap()
        logger.error("Bad Request", throwable)
        response["response"] = "BAD_REQUEST: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        return response
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = [EmptyAccountException::class])
    open fun handleHttpNotFound(throwable: Throwable): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        logger.error("not found: ", throwable)
        response["response"] = "NOT_FOUND: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        return response
    }

    companion object {
        private val mapper = ObjectMapper()
    }
}
