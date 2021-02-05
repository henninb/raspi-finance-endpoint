package finance.controllers

import finance.domain.Account
import finance.services.AccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/account")
class AccountController @Autowired constructor(private var accountService: AccountService) : BaseController() {

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
            //throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find any accountNameOwners.")
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

    //curl -k --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": "true","moniker": "0000", "totals": 0.00, "totalsBalanced": 0.00, "dateClosed": 0, "dateUpdated": 0, "dateAdded": 0}' 'https://localhost:8080/account/insert'
    @PostMapping(path = ["/insert"], produces = ["application/json"])
    fun insertAccount(@RequestBody account: Account): ResponseEntity<String> {
        accountService.insertAccount(account)
        return ResponseEntity.ok("account inserted")
    }

    //curl -k --header "Content-Type: application/json" --request DELETE 'https://localhost:8080/account/delete/test_brian'
    @DeleteMapping(path = ["/delete/{accountNameOwner}"], produces = ["application/json"])
    fun deleteByAccountNameOwner(@PathVariable accountNameOwner: String): ResponseEntity<String> {
        val accountOptional: Optional<Account> = accountService.findByAccountNameOwner(accountNameOwner)

        if (accountOptional.isPresent) {
            accountService.deleteByAccountNameOwner(accountNameOwner)
            return ResponseEntity.ok("account deleted")
        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not delete this account: $accountNameOwner.")
    }

    //curl -k --header "Content-Type: application/json" --request PUT 'https://localhost:8080/account/update/test_account' --data '{}'
    @PutMapping(path = ["/update/{accountNameOwner}"], produces = ["application/json"])
    fun updateAccount(
        @PathVariable("accountNameOwner") guid: String,
        @RequestBody account: Map<String, Any>
    ): ResponseEntity<String> {
        val accountToBeUpdated = mapper.convertValue(account, Account::class.java)
        val updateStatus: Boolean = accountService.updateAccount(accountToBeUpdated)
        if (updateStatus) {
            return ResponseEntity.ok("account updated")
        }

        throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "could not update this account: ${accountToBeUpdated.accountNameOwner}."
        )
    }

    //curl -k --header "Content-Type: application/json" --request PUT 'https://localhost:8080/account/rename?old=test_brian&new=testnew_brian'
    @PutMapping(path = ["/rename"], produces = ["application/json"])
    fun renameAccountNameOwner(
        @RequestParam(value = "old")  oldAccountNameOwner: String,
        @RequestParam("new")  newAccountNameOwner: String
    ): ResponseEntity<String> {
        val updateStatus: Boolean = accountService.renameAccountNameOwner(oldAccountNameOwner, newAccountNameOwner)
        if (updateStatus) {
            return ResponseEntity.ok("accountNameOwner renamed")
        }

        throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "could not rename this account: ${oldAccountNameOwner}."
        )
    }
}
