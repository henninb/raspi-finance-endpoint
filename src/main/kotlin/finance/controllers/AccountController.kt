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
class AccountController(private val accountService: AccountService) : BaseController() {

    // curl -k https://localhost:8443/account/totals
    @GetMapping("totals", produces = ["application/json"])
    fun computeAccountTotals(): ResponseEntity<Map<String, String>> {
        return try {
            logger.debug("Computing account totals")
            val response: MutableMap<String, String> = HashMap()
            //TODO: 6/27/2021 - need to modify to 1 call from 3
            val totalsCleared = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Cleared)
            val totalsFuture = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Future)
            val totalsOutstanding = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Outstanding)

            logger.debug("Account totals computed - Outstanding: $totalsOutstanding, Cleared: $totalsCleared, Future: $totalsFuture")

            response["totalsCleared"] = totalsCleared.toString()
            response["totalsFuture"] = totalsFuture.toString()
            response["totalsOutstanding"] = totalsOutstanding.toString()
            response["totals"] = (totalsCleared + totalsFuture + totalsOutstanding).toString()
            ResponseEntity.ok(response)
        } catch (ex: Exception) {
            logger.error("Failed to compute account totals: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to compute account totals: ${ex.message}", ex)
        }
    }

    // curl -k https://localhost:8443/account/payment/required
    @GetMapping("/payment/required", produces = ["application/json"])
    fun selectPaymentRequired(): ResponseEntity<List<Account>> {
        return try {
            logger.debug("Finding accounts that require payment")
            val accountNameOwners = accountService.findAccountsThatRequirePayment()
            if (accountNameOwners.isEmpty()) {
                logger.info("No accounts requiring payment found")
            } else {
                logger.info("Found ${accountNameOwners.size} accounts requiring payment")
            }
            ResponseEntity.ok(accountNameOwners)
        } catch (ex: Exception) {
            logger.error("Failed to find accounts requiring payment: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve payment required accounts: ${ex.message}", ex)
        }
    }

    // curl -k https://localhost:8443/account/select/active
    @GetMapping("/select/active", produces = ["application/json"])
    fun accounts(): ResponseEntity<List<Account>> {
        return try {
            logger.debug("Retrieving active accounts")
            //TODO: create a separate endpoint for the totals
            accountService.updateTotalsForAllAccounts()
            val accounts: List<Account> = accountService.accounts()
            if (accounts.isEmpty()) {
                logger.warn("No active accounts found")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "No active accounts found")
            }
            logger.info("Retrieved ${accounts.size} active accounts")
            ResponseEntity.ok(accounts)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to retrieve active accounts: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve active accounts: ${ex.message}", ex)
        }
    }

    // curl -k https://localhost:8443/account/select/test_brian
    @GetMapping("/select/{accountNameOwner}", produces = ["application/json"])
    fun account(@PathVariable accountNameOwner: String): ResponseEntity<Account> {
        return try {
            logger.debug("Retrieving account: $accountNameOwner")
            val account = accountService.account(accountNameOwner)
                .orElseThrow { 
                    logger.warn("Account not found: $accountNameOwner")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: $accountNameOwner")
                }
            logger.info("Retrieved account: $accountNameOwner")
            ResponseEntity.ok(account)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to retrieve account $accountNameOwner: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve account: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": true, "moniker": "0000", "totals": 0.00, "totalsBalanced": 0.00}' https://localhost:8443/account/insert
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertAccount(@RequestBody account: Account): ResponseEntity<Account> {
        return try {
            logger.info("Inserting account: ${account.accountNameOwner}")
            val accountResponse = accountService.insertAccount(account)
            logger.info("Account inserted successfully: ${accountResponse.accountNameOwner}")
            ResponseEntity(accountResponse, HttpStatus.CREATED)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to insert account due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate account found.")
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error inserting account ${account.accountNameOwner}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}", ex)
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid input inserting account ${account.accountNameOwner}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error inserting account ${account.accountNameOwner}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request DELETE https://localhost:8443/account/delete/test_brian
    @DeleteMapping("/delete/{accountNameOwner}", produces = ["application/json"])
    fun deleteAccount(@PathVariable accountNameOwner: String): ResponseEntity<Account> {
        return try {
            logger.info("Attempting to delete account: $accountNameOwner")
            val account = accountService.account(accountNameOwner)
                .orElseThrow { 
                    logger.warn("Account not found for deletion: $accountNameOwner")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: $accountNameOwner")
                }
            
            accountService.deleteAccount(accountNameOwner)
            logger.info("Account deleted successfully: $accountNameOwner")
            ResponseEntity.ok(account)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to delete account $accountNameOwner: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete account: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT --data '{"accountNameOwner":"test_brian", "accountType": "credit", "activeStatus": true}' https://localhost:8443/account/update/test_brian
    @PutMapping("/update/{accountNameOwner}", produces = ["application/json"])
    fun updateAccount(
        @PathVariable("accountNameOwner") accountNameOwner: String,
        @RequestBody account: Map<String, Any>
    ): ResponseEntity<Account> {
        return try {
            logger.info("Updating account: $accountNameOwner")
            val accountToBeUpdated = mapper.convertValue(account, Account::class.java)
            val accountResponse = accountService.updateAccount(accountToBeUpdated)
            logger.info("Account updated successfully: $accountNameOwner")
            ResponseEntity.ok(accountResponse)
        } catch (ex: Exception) {
            logger.error("Failed to update account $accountNameOwner: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update account: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT https://localhost:8443/account/rename?old=test_brian&new=testnew_brian
    @PutMapping("/rename", produces = ["application/json"])
    fun renameAccountNameOwner(
        @RequestParam(value = "old") oldAccountNameOwner: String,
        @RequestParam("new") newAccountNameOwner: String
    ): ResponseEntity<Account> {
        return try {
            logger.info("Renaming account from $oldAccountNameOwner to $newAccountNameOwner")
            val accountResponse = accountService.renameAccountNameOwner(oldAccountNameOwner, newAccountNameOwner)
            logger.info("Account renamed successfully from $oldAccountNameOwner to $newAccountNameOwner")
            ResponseEntity.ok(accountResponse)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to rename account due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Target account name already exists.")
        } catch (ex: Exception) {
            logger.error("Failed to rename account from $oldAccountNameOwner to $newAccountNameOwner: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to rename account: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT https://localhost:8443/account/deactivate/test_brian
    @PutMapping("/deactivate/{accountNameOwner}", produces = ["application/json"])
    fun deactivateAccount(@PathVariable accountNameOwner: String): ResponseEntity<Account> {
        return try {
            logger.info("Deactivating account: $accountNameOwner")
            val accountResponse = accountService.deactivateAccount(accountNameOwner)
            logger.info("Account deactivated successfully: $accountNameOwner")
            ResponseEntity.ok(accountResponse)
        } catch (ex: jakarta.persistence.EntityNotFoundException) {
            logger.warn("Account not found for deactivation: $accountNameOwner")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: $accountNameOwner")
        } catch (ex: java.util.concurrent.ExecutionException) {
            // Handle wrapped exceptions from resilience4j
            val cause = ex.cause
            if (cause is jakarta.persistence.EntityNotFoundException) {
                logger.warn("Account not found for deactivation: $accountNameOwner")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: $accountNameOwner")
            } else {
                logger.error("Failed to deactivate account $accountNameOwner: ${ex.message}", ex)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to deactivate account: ${ex.message}", ex)
            }
        } catch (ex: Exception) {
            logger.error("Failed to deactivate account $accountNameOwner: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to deactivate account: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT https://localhost:8443/account/activate/test_brian
    @PutMapping("/activate/{accountNameOwner}", produces = ["application/json"])
    fun activateAccount(@PathVariable accountNameOwner: String): ResponseEntity<Account> {
        return try {
            logger.info("Activating account: $accountNameOwner")
            val accountResponse = accountService.activateAccount(accountNameOwner)
            logger.info("Account activated successfully: $accountNameOwner")
            ResponseEntity.ok(accountResponse)
        } catch (ex: jakarta.persistence.EntityNotFoundException) {
            logger.warn("Account not found for activation: $accountNameOwner")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: $accountNameOwner")
        } catch (ex: java.util.concurrent.ExecutionException) {
            // Handle wrapped exceptions from resilience4j
            val cause = ex.cause
            if (cause is jakarta.persistence.EntityNotFoundException) {
                logger.warn("Account not found for activation: $accountNameOwner")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: $accountNameOwner")
            } else {
                logger.error("Failed to activate account $accountNameOwner: ${ex.message}", ex)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to activate account: ${ex.message}", ex)
            }
        } catch (ex: Exception) {
            logger.error("Failed to activate account $accountNameOwner: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to activate account: ${ex.message}", ex)
        }
    }
}
