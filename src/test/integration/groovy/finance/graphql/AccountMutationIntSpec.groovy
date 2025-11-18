package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.dto.AccountInputDto
import finance.controllers.graphql.GraphQLMutationController
import finance.domain.Account
import finance.domain.AccountType
import finance.services.AccountService
import org.springframework.beans.factory.annotation.Autowired
import jakarta.validation.ConstraintViolationException
import java.math.BigDecimal
import java.sql.Timestamp

class AccountMutationIntSpec extends BaseIntegrationSpec {

    @Autowired
    GraphQLMutationController mutationController

    @Autowired
    AccountService accountService

    def "createAccount mutation succeeds with valid input"() {
        given:
        withUserRole()
        def accountInput = new AccountInputDto(
                null,
                "testcreate_account",
                AccountType.Debit,
                true,
                "1234",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null
        )

        when:
        def result = mutationController.createAccount(accountInput)

        then:
        result != null
        result.accountId > 0
        result.accountNameOwner == "testcreate_account"
        result.accountType == AccountType.Debit
        result.activeStatus == true
        result.moniker == "1234"
    }

    def "createAccount mutation fails validation for empty account name"() {
        given:
        withUserRole()
        def accountInput = new AccountInputDto(
                null,
                "",                      // invalid: empty
                AccountType.Debit,
                true,
                "1234",
                null,
                null,
                null,
                null,
                null
        )

        when:
        mutationController.createAccount(accountInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createAccount mutation fails validation for account name too short"() {
        given:
        withUserRole()
        def accountInput = new AccountInputDto(
                null,
                "ab",                    // invalid: less than 3 characters
                AccountType.Debit,
                true,
                "1234",
                null,
                null,
                null,
                null,
                null
        )

        when:
        mutationController.createAccount(accountInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createAccount mutation fails validation for account name too long"() {
        given:
        withUserRole()
        def accountInput = new AccountInputDto(
                null,
                "a" * 41,                // invalid: exceeds 40 character limit
                AccountType.Debit,
                true,
                "1234",
                null,
                null,
                null,
                null,
                null
        )

        when:
        mutationController.createAccount(accountInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createAccount mutation fails validation for account name with spaces"() {
        given:
        withUserRole()
        def accountInput = new AccountInputDto(
                null,
                "invalid account",       // invalid: contains space
                AccountType.Debit,
                true,
                "1234",
                null,
                null,
                null,
                null,
                null
        )

        when:
        mutationController.createAccount(accountInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createAccount mutation fails validation for invalid moniker format"() {
        given:
        withUserRole()
        def accountInput = new AccountInputDto(
                null,
                "testacct_main",
                AccountType.Debit,
                true,
                "12",                    // invalid: not 4 digits
                null,
                null,
                null,
                null,
                null
        )

        when:
        mutationController.createAccount(accountInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "updateAccount mutation succeeds with valid input"() {
        given:
        withUserRole()
        def created = createTestAccount("testupdate_account", AccountType.Debit)
        def accountInput = new AccountInputDto(
                created.accountId,
                "testupdate_account",
                AccountType.Debit,
                false,                   // change active status
                "5678",                  // change moniker
                null,
                null,
                null,
                null,
                null
        )

        when:
        def result = mutationController.updateAccount(accountInput, null)

        then:
        result != null
        result.accountId == created.accountId
        result.accountNameOwner == "testupdate_account"
        result.activeStatus == false
        result.moniker == "5678"
    }

    def "updateAccount mutation fails for non-existent account"() {
        given:
        withUserRole()
        def accountInput = new AccountInputDto(
                999999L,                // non-existent ID
                "nonexist_acct",
                AccountType.Debit,
                true,
                "1234",
                null,
                null,
                null,
                null,
                null
        )

        when:
        mutationController.updateAccount(accountInput, null)

        then:
        thrown(RuntimeException)
    }

    def "deleteAccount mutation returns true for existing account"() {
        given:
        withUserRole()
        def created = createTestAccount("testdelete_account", AccountType.Debit)

        when:
        def deleted = mutationController.deleteAccount(created.accountNameOwner)

        then:
        deleted == true
    }

    def "deleteAccount mutation returns false for missing account"() {
        given:
        withUserRole()

        expect:
        mutationController.deleteAccount("nonexistent_account") == false
    }

    private Account createTestAccount(String accountNameOwner, AccountType accountType) {
        Account account = new Account()
        account.accountId = 0L
        account.accountNameOwner = accountNameOwner
        account.accountType = accountType
        account.activeStatus = true
        account.moniker = "0000"
        account.outstanding = BigDecimal.ZERO
        account.cleared = BigDecimal.ZERO
        account.future = BigDecimal.ZERO
        account.dateClosed = new Timestamp(0)
        account.validationDate = new Timestamp(System.currentTimeMillis())

        def result = accountService.save(account)
        return result.data
    }
}
