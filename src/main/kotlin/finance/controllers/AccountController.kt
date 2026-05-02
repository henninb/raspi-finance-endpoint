package finance.controllers

import finance.domain.Account
import finance.domain.TransactionState
import finance.domain.toCreatedResponse
import finance.domain.toListOkResponse
import finance.domain.toOkResponse
import finance.domain.toPagedOkResponse
import finance.services.AccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Account Management", description = "Operations for managing financial accounts")
@RestController
@RequestMapping("/api/account")
@PreAuthorize("hasAuthority('USER')")
class AccountController(
    private val accountService: AccountService,
) : StandardizedBaseController(),
    StandardRestController<Account, String> {
    @Operation(summary = "Get all active accounts")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active accounts retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Account>> = accountService.findAllActive().toListOkResponse()

    @Operation(summary = "Get all active accounts (paginated)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of accounts returned"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active/paged", produces = ["application/json"])
    override fun findAllActivePaged(pageable: Pageable): ResponseEntity<Page<Account>> = accountService.findAllActive(pageable).toPagedOkResponse(pageable)

    @Operation(summary = "Get account by accountNameOwner")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Account retrieved"),
            ApiResponse(responseCode = "404", description = "Account not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/{accountNameOwner}", produces = ["application/json"])
    override fun findById(
        @PathVariable("accountNameOwner") id: String,
    ): ResponseEntity<Account> = accountService.findById(id).toOkResponse()

    @Operation(summary = "Create account")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Account created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: Account,
    ): ResponseEntity<Account> = accountService.save(entity).toCreatedResponse()

    @Operation(summary = "Update account by accountNameOwner")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Account updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Account not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PutMapping("/{accountNameOwner}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("accountNameOwner") id: String,
        @Valid @RequestBody entity: Account,
    ): ResponseEntity<Account> {
        entity.accountNameOwner = id
        return accountService.update(entity).toOkResponse()
    }

    @Operation(summary = "Delete account by accountNameOwner")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Account deleted"),
            ApiResponse(responseCode = "404", description = "Account not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @DeleteMapping("/{accountNameOwner}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("accountNameOwner") id: String,
    ): ResponseEntity<Account> = accountService.deleteById(id).toOkResponse()

    // ===== BUSINESS LOGIC ENDPOINTS =====

    @Operation(
        summary = "Get account totals",
        description = "Computes the total amounts for all accounts by transaction state (cleared, outstanding, future)",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Account totals computed successfully",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = Map::class))],
            ),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("totals", produces = ["application/json"])
    fun computeAccountTotals(): ResponseEntity<Map<String, String>> {
        val totalsCleared = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Cleared)
        val totalsFuture = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Future)
        val totalsOutstanding = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Outstanding)
        return ResponseEntity.ok(
            mapOf(
                "totalsCleared" to totalsCleared.toString(),
                "totalsFuture" to totalsFuture.toString(),
                "totalsOutstanding" to totalsOutstanding.toString(),
                "totals" to (totalsCleared + totalsFuture + totalsOutstanding).toString(),
            ),
        )
    }

    @Operation(summary = "Refresh validation dates for all accounts")
    @ApiResponses(value = [ApiResponse(responseCode = "204", description = "Refresh triggered"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @GetMapping("/validation/refresh")
    fun refreshValidationDates(): ResponseEntity<Void> {
        accountService.updateValidationDatesForAllAccounts()
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "List accounts that require payment")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Accounts retrieved"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @GetMapping("/payment/required", produces = ["application/json"])
    fun selectPaymentRequired(): ResponseEntity<List<Account>> = ResponseEntity.ok(accountService.findAccountsThatRequirePayment())

    @Operation(summary = "Rename accountNameOwner")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Account renamed"), ApiResponse(responseCode = "409", description = "Target exists"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @PutMapping("/rename", produces = ["application/json"])
    fun renameAccountNameOwner(
        @RequestParam(value = "old") oldAccountNameOwner: String,
        @RequestParam("new") newAccountNameOwner: String,
    ): ResponseEntity<Account> = ResponseEntity.ok(accountService.renameAccountNameOwner(oldAccountNameOwner, newAccountNameOwner))

    @Operation(summary = "Deactivate account")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Account deactivated"), ApiResponse(responseCode = "404", description = "Not found"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @PutMapping("/deactivate/{accountNameOwner}", produces = ["application/json"])
    fun deactivateAccount(
        @PathVariable accountNameOwner: String,
    ): ResponseEntity<Account> = ResponseEntity.ok(accountService.deactivateAccount(accountNameOwner))

    @Operation(summary = "Activate account")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Account activated"), ApiResponse(responseCode = "404", description = "Not found"), ApiResponse(responseCode = "500", description = "Internal server error")])
    @PutMapping("/activate/{accountNameOwner}", produces = ["application/json"])
    fun activateAccount(
        @PathVariable accountNameOwner: String,
    ): ResponseEntity<Account> = ResponseEntity.ok(accountService.activateAccount(accountNameOwner))
}
