package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.services.AccountService
import org.slf4j.LoggerFactory
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
import java.util.*
import javax.validation.ConstraintViolationException

@CrossOrigin
@RestController
@RequestMapping("/account")
//@Validated
class AccountController @Autowired constructor(private var accountService: AccountService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    //http://localhost:8080/account/totals
    @GetMapping(path = ["totals"], produces = ["application/json"])
    fun computeAccountTotals(): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        response["totals"] = accountService.computeTheGrandTotalForAllTransactions().toString()
        response["totalsCleared"] = accountService.computeTheGrandTotalForAllClearedTransactions().toString()
        return response
    }

    //curl --header "Content-Type: application/json" https://hornsup:8080/account/payment/required
    @GetMapping(path = ["/payment/required"], produces = ["application/json"])
    fun selectPaymentRequired(): ResponseEntity<List<String>> {

        val accountNameOwners = accountService.findAccountsThatRequirePayment()
        if (accountNameOwners.isEmpty()) {
            logger.info("no accountNameOwners found.")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find any accountNameOwners.")
        }
        return ResponseEntity.ok(accountNameOwners)
    }


    //http://localhost:8080/account/select/active
    @GetMapping(path = ["/select/active"], produces = ["application/json"])
    fun selectAllActiveAccounts(): ResponseEntity<List<Account>> {
        //TODO: create a separate endpoint for the totals
        accountService.updateTheGrandTotalForAllClearedTransactions()
        val accounts: List<Account> = accountService.findByActiveStatusOrderByAccountNameOwner()
        if (accounts.isEmpty()) {
            logger.info("no accounts found.")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find any accounts.")
        }
        logger.info("select active accounts: ${accounts.size}")
        return ResponseEntity.ok(accounts)
    }

    //http://localhost:8080/account/select/test_brian
    @GetMapping(path = ["/select/{accountNameOwner}"], produces = ["application/json"])
    fun selectByAccountNameOwner(@PathVariable accountNameOwner: String): ResponseEntity<Account> {
        val accountOptional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)
        if (accountOptional.isPresent) {
            return ResponseEntity.ok(accountOptional.get())
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find this account.")
    }

    //curl --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": "true","moniker": "0000", "totals": 0.00, "totalsBalanced": 0.00, "dateClosed": 0, "dateUpdated": 0, "dateAdded": 0}' http://localhost:8080/account/insert
    //http://localhost:8080/account/insert
    @PostMapping(path = ["/insert"], produces = ["application/json"])
    fun insertAccount(@RequestBody account: Account): ResponseEntity<String> {
        accountService.insertAccount(account)
        return ResponseEntity.ok("account inserted")
    }

    //http://localhost:8080/account/delete/amex_brian
    //curl --header "Content-Type: application/json" --request DELETE http://localhost:8080/account/delete/test_brian
    @DeleteMapping(path = ["/delete/{accountNameOwner}"], produces = ["application/json"])
    fun deleteByAccountNameOwner(@PathVariable accountNameOwner: String): ResponseEntity<String> {
        val accountOptional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)

        if (accountOptional.isPresent) {
            accountService.deleteByAccountNameOwner(accountNameOwner)
            return ResponseEntity.ok("account deleted")
        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not delete this account: $accountNameOwner.")
    }

    //http://localhost:8080/account/update
    @PatchMapping(path = ["/update"], produces = ["application/json"])
    fun updateTransaction(@RequestBody account: Map<String, String>): ResponseEntity<String> {
        val toBePatchedTransaction = mapper.convertValue(account, Account::class.java)
        val updateStatus: Boolean = accountService.patchAccount(toBePatchedTransaction)
        if (updateStatus) {
            return ResponseEntity.ok("account updated")
        }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not update this account: ${toBePatchedTransaction.accountNameOwner}.")
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST) //400
    @ExceptionHandler(value = [ConstraintViolationException::class, NumberFormatException::class, EmptyResultDataAccessException::class,
        MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class, HttpMediaTypeNotSupportedException::class,
        IllegalArgumentException::class, DataIntegrityViolationException::class])
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
    }
}
