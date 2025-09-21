package finance.controllers

import finance.domain.Account
import finance.domain.TransactionState
import finance.services.IAccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import java.util.*

@Tag(name = "Account Management", description = "Operations for managing financial accounts")
@CrossOrigin
@RestController
@RequestMapping("/api/account")
class AccountController(private val accountService: IAccountService) :
    StandardizedBaseController(), StandardRestController<Account, String> {

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/account/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Account>> {
        return handleCrudOperation("Find all active accounts", null) {
            logger.debug("Retrieving all active accounts (standardized)")
            accountService.updateTotalsForAllAccounts()
            val accounts: List<Account> = accountService.accounts()
            logger.info("Retrieved ${accounts.size} active accounts (standardized)")
            accounts
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/account/{accountNameOwner}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{accountNameOwner}", produces = ["application/json"])
    override fun findById(@PathVariable accountNameOwner: String): ResponseEntity<Account> {
        return handleCrudOperation("Find account by name", accountNameOwner) {
            logger.debug("Retrieving account: $accountNameOwner (standardized)")
            val account = accountService.account(accountNameOwner)
                .orElseThrow {
                    logger.warn("Account not found: $accountNameOwner (standardized)")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: $accountNameOwner")
                }
            logger.info("Retrieved account: $accountNameOwner (standardized)")
            account
        }
    }

    /**
     * Standardized entity creation - POST /api/account
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(@Valid @RequestBody account: Account): ResponseEntity<Account> {
        return handleCreateOperation("Account", account.accountNameOwner) {
            logger.info("Creating account: ${account.accountNameOwner} (standardized)")
            val result = accountService.insertAccount(account)
            logger.info("Account created successfully: ${result.accountNameOwner} (standardized)")
            result
        }
    }

    /**
     * Standardized entity update - PUT /api/account/{accountNameOwner}
     * Uses entity type instead of Map<String, Any>
     */
    @PutMapping("/{accountNameOwner}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(@PathVariable accountNameOwner: String, @Valid @RequestBody account: Account): ResponseEntity<Account> {
        return handleCrudOperation("Update account", accountNameOwner) {
            logger.info("Updating account: $accountNameOwner (standardized)")
            // Validate account exists first
            accountService.account(accountNameOwner)
                .orElseThrow {
                    logger.warn("Account not found for update: $accountNameOwner (standardized)")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: $accountNameOwner")
                }
            val result = accountService.updateAccount(account)
            logger.info("Account updated successfully: $accountNameOwner (standardized)")
            result
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/account/{accountNameOwner}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{accountNameOwner}", produces = ["application/json"])
    override fun deleteById(@PathVariable accountNameOwner: String): ResponseEntity<Account> {
        return handleDeleteOperation(
            "Account",
            accountNameOwner,
            { accountService.account(accountNameOwner) },
            { accountService.deleteAccount(accountNameOwner) }
        )
    }

    // ===== BUSINESS LOGIC ENDPOINTS (SPECIALIZED) =====

    @Operation(
        summary = "Get account totals",
        description = "Computes the total amounts for all accounts by transaction state (cleared, outstanding, future)"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Account totals computed successfully",
            content = [Content(mediaType = "application/json",
            schema = Schema(implementation = Map::class))]),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
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

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/account/select/active
     * Maintains original behavior including 404 when empty
     */
    @Operation(
        summary = "Get active accounts",
        description = "Retrieves all active financial accounts with updated totals"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Active accounts retrieved successfully"),
        ApiResponse(responseCode = "404", description = "No active accounts found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
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

    /**
     * Legacy endpoint - GET /api/account/select/{accountNameOwner}
     * Maintains original behavior
     */
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

    /**
     * Legacy endpoint - POST /api/account/insert
     * Maintains original behavior
     */
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

    /**
     * Legacy endpoint - DELETE /api/account/delete/{accountNameOwner}
     * Maintains original behavior
     */
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

    /**
     * Legacy endpoint - PUT /api/account/update/{accountNameOwner}
     * Maintains original behavior using Map<String, Any>
     */
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
