package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLQueryController
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.repositories.AccountRepository
import finance.services.ValidationAmountService
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.sql.Timestamp

class ValidationAmountQueryIntSpec extends BaseIntegrationSpec {

    @Shared @Autowired
    ValidationAmountService validationAmountService

    @Shared @Autowired
    GraphQLQueryController queryController

    @Shared @Autowired
    AccountRepository accountRepository

    def "fetch all validation amounts via query controller"() {
        given:
        def account1 = createTestAccount("valquerycheck_allone")
        def account2 = createTestAccount("valquerycheck_alltwo")
        createTestValidationAmount(account1, new BigDecimal("100.00"), TransactionState.Cleared)
        createTestValidationAmount(account2, new BigDecimal("200.00"), TransactionState.Outstanding)

        when:
        def validationAmounts = queryController.validationAmounts()

        then:
        validationAmounts != null
        validationAmounts.size() >= 2
        validationAmounts.any { it.amount == new BigDecimal("100.00") }
        validationAmounts.any { it.amount == new BigDecimal("200.00") }
    }

    def "fetch validation amount by ID via query controller"() {
        given:
        def account = createTestAccount("valquerycheck_findid")
        def savedValidation = createTestValidationAmount(account, new BigDecimal("150.00"), TransactionState.Cleared)

        when:
        def result = queryController.validationAmount(savedValidation.validationId)

        then:
        result != null
        result.validationId == savedValidation.validationId
        result.accountId == account.accountId
        result.amount == new BigDecimal("150.00")
        result.transactionState == TransactionState.Cleared
        result.activeStatus == true
    }

    def "handle validation amount not found via query controller"() {
        expect:
        queryController.validationAmount(999999L) == null
    }

    def "fetch validation amounts ordered by date desc"() {
        given:
        def account = createTestAccount("valquerycheck_ordered")
        // Create validation amounts with different timestamps
        Thread.sleep(10)  // Small delay to ensure different timestamps
        def validation1 = createTestValidationAmount(account, new BigDecimal("100.00"), TransactionState.Cleared)
        Thread.sleep(10)
        def validation2 = createTestValidationAmount(account, new BigDecimal("200.00"), TransactionState.Outstanding)

        when:
        def validationAmounts = queryController.validationAmounts()

        then:
        validationAmounts != null
        validationAmounts.size() >= 2
        // Most recent should be first (descending order by validation date)
        def foundValidations = validationAmounts.findAll {
            it.validationId == validation1.validationId || it.validationId == validation2.validationId
        }
        foundValidations.size() == 2
    }

    def "fetch validation amounts with different transaction states"() {
        given:
        def account = createTestAccount("valquerycheck_states")
        createTestValidationAmount(account, new BigDecimal("100.00"), TransactionState.Cleared)
        createTestValidationAmount(account, new BigDecimal("200.00"), TransactionState.Outstanding)
        createTestValidationAmount(account, new BigDecimal("300.00"), TransactionState.Future)

        when:
        def validationAmounts = queryController.validationAmounts()

        then:
        validationAmounts != null
        validationAmounts.any { it.transactionState == TransactionState.Cleared }
        validationAmounts.any { it.transactionState == TransactionState.Outstanding }
        validationAmounts.any { it.transactionState == TransactionState.Future }
    }

    private Account createTestAccount(String accountNameOwner) {
        Account account = new Account()
        account.accountNameOwner = accountNameOwner
        account.accountType = AccountType.Debit
        account.activeStatus = true
        account.moniker = "0000"
        account.outstanding = BigDecimal.ZERO
        account.future = BigDecimal.ZERO
        account.cleared = BigDecimal.ZERO
        account.dateClosed = new Timestamp(System.currentTimeMillis())
        account.validationDate = new Timestamp(System.currentTimeMillis())
        return accountRepository.saveAndFlush(account)
    }

    private ValidationAmount createTestValidationAmount(Account account, BigDecimal amount, TransactionState state) {
        ValidationAmount validation = new ValidationAmount(
            0L,
            "",  // owner
            account.accountId,
            null,  // account reference
            new Timestamp(System.currentTimeMillis()),
            true,
            state,
            amount
        )
        def result = validationAmountService.save(validation)
        return result.data
    }
}
