package finance.controllers

import finance.domain.Account
import finance.domain.ServiceResult
import finance.domain.TransactionState
import finance.services.StandardizedAccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Account Management", description = "Operations for managing financial accounts")
@CrossOrigin
@RestController
@RequestMapping("/api/account")
class AccountController(
    private val standardizedAccountService: StandardizedAccountService,
) : StandardizedBaseController(),
    StandardRestController<Account, String> {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/account/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Account>> {
        standardizedAccountService.updateTotalsForAllAccounts()
        return when (val result = standardizedAccountService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} active accounts (standardized)")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.info("No active accounts found (standardized)")
                ResponseEntity.ok(emptyList())
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving active accounts: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/account/{accountNameOwner}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{accountNameOwner}", produces = ["application/json"])
    override fun findById(
        @PathVariable("accountNameOwner") id: String,
    ): ResponseEntity<Account> =
        when (val result = standardizedAccountService.findById(id)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved account: $id (standardized)")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Account not found: $id (standardized)")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving account $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized entity creation - POST /api/account
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: Account,
    ): ResponseEntity<Account> =
        when (val result = standardizedAccountService.save(entity)) {
            is ServiceResult.Success -> {
                logger.info("Account created successfully: ${entity.accountNameOwner} (standardized)")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating account: ${result.errors}")
                ResponseEntity.badRequest().build<Account>()
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating account: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<Account>()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error creating account: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Account>()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Account>()
            }
        }

    /**
     * Standardized entity update - PUT /api/account/{accountNameOwner}
     * Uses entity type instead of Map<String, Any>
     */
    @PutMapping("/{accountNameOwner}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("accountNameOwner") id: String,
        @Valid @RequestBody entity: Account,
    ): ResponseEntity<Account> =
        when (val result = standardizedAccountService.update(entity)) {
            is ServiceResult.Success -> {
                logger.info("Account updated successfully: $id (standardized)")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Account not found for update: $id (standardized)")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating account: ${result.errors}")
                ResponseEntity.badRequest().build<Account>()
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating account: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<Account>()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error updating account: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Account>()
            }
        }

    /**
     * Standardized entity deletion - DELETE /api/account/{accountNameOwner}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{accountNameOwner}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("accountNameOwner") id: String,
    ): ResponseEntity<Account> {
        // First get the account to return it
        val accountResult = standardizedAccountService.findById(id)
        if (accountResult !is ServiceResult.Success) {
            logger.warn("Account not found for deletion: $id")
            return ResponseEntity.notFound().build()
        }

        return when (val result = standardizedAccountService.deleteById(id)) {
            is ServiceResult.Success -> {
                logger.info("Account deleted successfully: $id")
                ResponseEntity.ok(accountResult.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Account not found for deletion: $id")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error deleting account: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Account>()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Account>()
            }
        }
    }

    // ===== BUSINESS LOGIC ENDPOINTS (SPECIALIZED) =====

    @Operation(
        summary = "Get account totals",
        description = "Computes the total amounts for all accounts by transaction state (cleared, outstanding, future)",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Account totals computed successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = Map::class),
                    ),
                ],
            ),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("totals", produces = ["application/json"])
    fun computeAccountTotals(): ResponseEntity<Map<String, String>> =
        try {
            logger.debug("Computing account totals")
            val response: MutableMap<String, String> = HashMap()
            // TODO: 6/27/2021 - need to modify to 1 call from 3
            val totalsCleared = standardizedAccountService.sumOfAllTransactionsByTransactionState(TransactionState.Cleared)
            val totalsFuture = standardizedAccountService.sumOfAllTransactionsByTransactionState(TransactionState.Future)
            val totalsOutstanding = standardizedAccountService.sumOfAllTransactionsByTransactionState(TransactionState.Outstanding)

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

    // GET /api/account/validation/refresh
    // Triggers a bulk refresh of account.validation_date from t_validation_amount
    @GetMapping("/validation/refresh")
    fun refreshValidationDates(): ResponseEntity<Void> =
        try {
            logger.info("Refreshing validation dates for all accounts from latest ValidationAmount rows")
            standardizedAccountService.updateValidationDatesForAllAccounts()
            ResponseEntity.noContent().build()
        } catch (ex: Exception) {
            logger.error("Failed to refresh validation dates: ${ex.message}", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }

    // curl -k https://localhost:8443/account/payment/required
    @GetMapping("/payment/required", produces = ["application/json"])
    fun selectPaymentRequired(): ResponseEntity<List<Account>> =
        try {
            logger.debug("Finding accounts that require payment")
            val accountNameOwners = standardizedAccountService.findAccountsThatRequirePayment()
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

    // curl -k --header "Content-Type: application/json" --request PUT https://localhost:8443/account/rename?old=test_brian&new=testnew_brian
    @PutMapping("/rename", produces = ["application/json"])
    fun renameAccountNameOwner(
        @RequestParam(value = "old") oldAccountNameOwner: String,
        @RequestParam("new") newAccountNameOwner: String,
    ): ResponseEntity<Account> =
        try {
            logger.info("Renaming account from $oldAccountNameOwner to $newAccountNameOwner")
            val accountResponse = standardizedAccountService.renameAccountNameOwner(oldAccountNameOwner, newAccountNameOwner)
            logger.info("Account renamed successfully from $oldAccountNameOwner to $newAccountNameOwner")
            ResponseEntity.ok(accountResponse)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to rename account due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Target account name already exists.")
        } catch (ex: Exception) {
            logger.error("Failed to rename account from $oldAccountNameOwner to $newAccountNameOwner: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to rename account: ${ex.message}", ex)
        }

    // curl -k --header "Content-Type: application/json" --request PUT https://localhost:8443/account/deactivate/test_brian
    @PutMapping("/deactivate/{accountNameOwner}", produces = ["application/json"])
    fun deactivateAccount(
        @PathVariable accountNameOwner: String,
    ): ResponseEntity<Account> =
        try {
            logger.info("Deactivating account: $accountNameOwner")
            val accountResponse = standardizedAccountService.deactivateAccount(accountNameOwner)
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

    // curl -k --header "Content-Type: application/json" --request PUT https://localhost:8443/account/activate/test_brian
    @PutMapping("/activate/{accountNameOwner}", produces = ["application/json"])
    fun activateAccount(
        @PathVariable accountNameOwner: String,
    ): ResponseEntity<Account> =
        try {
            logger.info("Activating account: $accountNameOwner")
            val accountResponse = standardizedAccountService.activateAccount(accountNameOwner)
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
