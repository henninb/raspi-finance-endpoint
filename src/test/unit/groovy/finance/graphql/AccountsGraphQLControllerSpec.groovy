package finance.graphql

import finance.controllers.GraphQLQueryController
import finance.domain.Account
import finance.domain.AccountType
import finance.services.BaseServiceSpec

import java.sql.Timestamp

class AccountsGraphQLControllerSpec extends BaseServiceSpec {

    def transferServiceMock = GroovyMock(finance.services.StandardizedTransferService)
    def controller = new GraphQLQueryController(accountService, categoryService, descriptionService, paymentService, transferServiceMock)

    void setup() {
        accountService.meterService = meterService
        accountService.validator = validator
    }

    private static Account makeAccount(Long id = 1L, String owner = 'checking_primary') {
        new Account(
                id,
                owner,
                AccountType.Checking,
                true,
                '1234',
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new Timestamp(0),
                new Timestamp(System.currentTimeMillis())
        )
    }

    def "accounts query returns list from service"() {
        given:
        def accounts = [makeAccount(1L, 'checking_primary'), makeAccount(2L, 'savings_primary')]
        accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> accounts

        expect:
        controller.accounts(null) == accounts
    }

    def "account query returns single account when present"() {
        given:
        def acct = makeAccount(42L, 'checking_primary')
        accountRepositoryMock.findByAccountNameOwner('checking_primary') >> Optional.of(acct)

        expect:
        controller.account('checking_primary') == acct
    }

    def "account query returns null when optional empty"() {
        given:
        accountRepositoryMock.findByAccountNameOwner('nonexistent') >> Optional.empty()

        expect:
        controller.account('nonexistent') == null
    }

    def "accounts query filters by accountType when provided"() {
        given:
        def a2 = makeAccount(2L, 'savings_primary'); a2.accountType = AccountType.Debit
        def a3 = makeAccount(3L, 'credit_card'); a3.accountType = AccountType.Credit
        accountRepositoryMock.findByActiveStatusAndAccountType(true, AccountType.Credit) >> [a3]
        accountRepositoryMock.findByActiveStatusAndAccountType(true, AccountType.Debit) >> [a2]

        when:
        def onlyCredit = controller.accounts(AccountType.Credit)
        def onlyDebit = controller.accounts(AccountType.Debit)

        then:
        onlyCredit*.accountType == [AccountType.Credit]

        and:
        onlyDebit*.accountType == [AccountType.Debit]
    }
}
