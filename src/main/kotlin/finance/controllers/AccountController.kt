package finance.controllers

import finance.domain.Account
import finance.domain.TransactionState
import finance.services.AccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/api/account", "/account")
class AccountController @Autowired constructor(private var accountService: AccountService) : BaseController() {

    //http://localhost:8443/account/totals
    @GetMapping("totals", produces = ["application/json"])
    fun computeAccountTotals(): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        //TODO: 6/27/2021 - need to modify to 1 call from 3
        val totalsCleared = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Cleared)
        val totalsFuture = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Future)
        val totalsOutstanding = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Outstanding)

        logger.info("totalsOutstanding: $totalsOutstanding")
        logger.info("totalsCleared: $totalsCleared")
        logger.info("totalsFuture: $totalsFuture")

        response["totalsCleared"] = totalsCleared.toString()
        response["totalsFuture"] = totalsFuture.toString()
        response["totalsOutstanding"] = totalsOutstanding.toString()
        response["totals"] = (totalsCleared + totalsFuture + totalsOutstanding).toString()
        return response
    }

    //curl --header "Content-Type: application/json" https://hornsup:8443/account/payment/required
    @GetMapping("/payment/required", produces = ["application/json"])
    fun selectPaymentRequired(): ResponseEntity<List<Account>> {

        val accountNameOwners = accountService.findAccountsThatRequirePayment()
        if (accountNameOwners.isEmpty()) {
            logger.info("no accountNameOwners were found.")
        }
        return ResponseEntity.ok(accountNameOwners)
    }

    //http://localhost:8443/account/select/active
    @GetMapping("/select/active", produces = ["application/json"])
    fun accounts(): ResponseEntity<List<Account>> {
        //TODO: create a separate endpoint for the totals
        accountService.updateTotalsForAllAccounts()
        val accounts: List<Account> = accountService.accounts()
        if (accounts.isEmpty()) {
            logger.info("no accounts found.")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find any accounts.")
        }
        logger.info("select active accounts: ${accounts.size}")
        return ResponseEntity.ok(accounts)
    }

    //http://localhost:8443/account/select/test_brian
    @GetMapping("/select/{accountNameOwner}", produces = ["application/json"])
    fun account(@PathVariable accountNameOwner: String): ResponseEntity<Account> {
        val accountOptional: Optional<Account> = accountService.account(accountNameOwner)
        if (accountOptional.isPresent) {
            return ResponseEntity.ok(accountOptional.get())
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find this account.")
    }

    //curl -k --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": "true","moniker": "0000", "totals": 0.00, "totalsBalanced": 0.00, "dateClosed": 0, "dateUpdated": 0, "dateAdded": 0}' 'https://localhost:8080/account/insert'
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertAccount(@RequestBody account: Account): ResponseEntity<Account> {
        return try {
            val accountResponse = accountService.insertAccount(account)
            ResponseEntity.ok(accountResponse)
        } catch (ex: ResponseStatusException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert account: ${ex.message}", ex)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    //curl -k --header "Content-Type: application/json" --request DELETE 'https://localhost:8443/account/delete/test_brian'
    @DeleteMapping("/delete/{accountNameOwner}", produces = ["application/json"])
    fun deleteAccount(@PathVariable accountNameOwner: String): ResponseEntity<Account> {
        val accountOptional: Optional<Account> = accountService.account(accountNameOwner)

        if (accountOptional.isPresent) {
            accountService.deleteAccount(accountNameOwner)
            val account: Account = accountOptional.get()
            logger.info("Account deleted: ${account.accountNameOwner}")
            return ResponseEntity.ok(account)
        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not delete this account: $accountNameOwner.")
    }

    //curl -k --header "Content-Type: application/json" --request PUT 'https://localhost:8443/account/update/test_account' --data '{}'
    @PutMapping("/update/{accountNameOwner}", produces = ["application/json"])
    fun updateAccount(
        @PathVariable("accountNameOwner") guid: String,
        @RequestBody account: Map<String, Any>
    ): ResponseEntity<Account> {
        val accountToBeUpdated = mapper.convertValue(account, Account::class.java)
        val accountResponse = accountService.updateAccount(accountToBeUpdated)
        return ResponseEntity.ok(accountResponse)
    }

    //curl -k -X PUT 'https://hornsup:8443/account/rename?old=gap_kari&new=oldnavy_kari'
    //curl -k --header "Content-Type: application/json" --request PUT 'https://hornsup:8443/account/rename?old=test_brian&new=testnew_brian'
    @PutMapping("/rename", produces = ["application/json"])
    fun renameAccountNameOwner(
        @RequestParam(value = "old") oldAccountNameOwner: String,
        @RequestParam("new") newAccountNameOwner: String
    ): ResponseEntity<Account> {
        val accountResponse = accountService.renameAccountNameOwner(oldAccountNameOwner, newAccountNameOwner)
        return ResponseEntity.ok(accountResponse)
    }
}
