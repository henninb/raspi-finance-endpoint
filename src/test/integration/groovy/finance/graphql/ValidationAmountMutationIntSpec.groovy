package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.dto.ValidationAmountInputDto
import finance.controllers.graphql.GraphQLMutationController
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.repositories.AccountRepository
import finance.services.ValidationAmountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import jakarta.validation.ConstraintViolationException

import java.sql.Timestamp

class ValidationAmountMutationIntSpec extends BaseIntegrationSpec {

    @Autowired
    GraphQLMutationController mutationController

    @Autowired
    ValidationAmountService validationAmountService

    @Autowired
    AccountRepository accountRepository

    def "createValidationAmount mutation succeeds with valid input"() {
        given:
        withUserRole()
        def account = createTestAccount("valtestchecking_create")
        def validationInput = new ValidationAmountInputDto(
            null,                                    // validationId
            account.accountId,                       // accountId
            new Timestamp(System.currentTimeMillis()), // validationDate
            true,                                    // activeStatus
            TransactionState.Cleared,                // transactionState
            new BigDecimal("100.00")                 // amount
        )

        when:
        def result = mutationController.createValidationAmount(validationInput)

        then:
        result != null
        result.validationId > 0
        result.accountId == account.accountId
        result.amount == new BigDecimal("100.00")
        result.transactionState == TransactionState.Cleared
        result.activeStatus == true
    }

    def "createValidationAmount mutation fails validation for zero amount"() {
        given:
        withUserRole()
        def account = createTestAccount("valtestchecking_zero")
        def validationInput = new ValidationAmountInputDto(
            null,
            account.accountId,
            new Timestamp(System.currentTimeMillis()),
            true,
            TransactionState.Cleared,
            new BigDecimal("0.00")                   // invalid: zero amount
        )

        when:
        mutationController.createValidationAmount(validationInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createValidationAmount mutation fails validation for negative amount"() {
        given:
        withUserRole()
        def account = createTestAccount("valtestchecking_negative")
        def validationInput = new ValidationAmountInputDto(
            null,
            account.accountId,
            new Timestamp(System.currentTimeMillis()),
            true,
            TransactionState.Cleared,
            new BigDecimal("-50.00")                 // invalid: negative amount
        )

        when:
        mutationController.createValidationAmount(validationInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "updateValidationAmount mutation succeeds with valid input"() {
        given:
        withUserRole()
        def account = createTestAccount("valtestchecking_update")
        def created = createTestValidationAmount(account, new BigDecimal("100.00"), TransactionState.Cleared)
        def validationInput = new ValidationAmountInputDto(
            created.validationId,                    // validationId for update
            account.accountId,
            new Timestamp(System.currentTimeMillis()),
            false,                                   // change active status
            TransactionState.Outstanding,             // change transaction state
            new BigDecimal("200.00")                 // change amount
        )

        when:
        def result = mutationController.updateValidationAmount(validationInput)

        then:
        result != null
        result.validationId == created.validationId
        result.accountId == account.accountId
        result.amount == new BigDecimal("200.00")
        result.transactionState == TransactionState.Outstanding
        result.activeStatus == false
    }

    def "updateValidationAmount mutation fails for non-existent validationId"() {
        given:
        withUserRole()
        def account = createTestAccount("valtestchecking_notfound")
        def validationInput = new ValidationAmountInputDto(
            999999L,                                 // non-existent ID
            account.accountId,
            new Timestamp(System.currentTimeMillis()),
            true,
            TransactionState.Cleared,
            new BigDecimal("100.00")
        )

        when:
        mutationController.updateValidationAmount(validationInput)

        then:
        thrown(RuntimeException)
    }

    def "deleteValidationAmount mutation returns true for existing validation amount"() {
        given:
        withUserRole()
        def account = createTestAccount("valtestchecking_delete")
        def created = createTestValidationAmount(account, new BigDecimal("100.00"), TransactionState.Cleared)

        when:
        def deleted = mutationController.deleteValidationAmount(created.validationId)

        then:
        deleted == true
    }

    def "deleteValidationAmount mutation returns false for missing validation amount"() {
        given:
        withUserRole()

        expect:
        mutationController.deleteValidationAmount(999999L) == false
    }

    def "createValidationAmount mutation with different transaction states"() {
        given:
        withUserRole()
        def account = createTestAccount("valtestchecking_states")
        def validationInput = new ValidationAmountInputDto(
            null,
            account.accountId,
            new Timestamp(System.currentTimeMillis()),
            true,
            transactionState,
            new BigDecimal("100.00")
        )

        when:
        def result = mutationController.createValidationAmount(validationInput)

        then:
        result != null
        result.transactionState == transactionState

        where:
        transactionState << [TransactionState.Cleared, TransactionState.Outstanding, TransactionState.Future]
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
        // owner must match TenantContext.getCurrentOwner() which reads from SecurityContext
        String currentOwner = SecurityContextHolder.getContext().authentication?.principal?.toString()?.toLowerCase() ?: testOwner
        account.owner = currentOwner
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
