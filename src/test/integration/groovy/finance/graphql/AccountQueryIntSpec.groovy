package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLQueryController
import finance.domain.Account
import finance.domain.AccountType
import finance.services.StandardizedAccountService
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import java.math.BigDecimal
import java.sql.Timestamp

class AccountQueryIntSpec extends BaseIntegrationSpec {

    @Shared @Autowired
    StandardizedAccountService accountService

    @Shared @Autowired
    GraphQLQueryController queryController

    def "fetch all accounts via query controller"() {
        given:
        createTestAccount("checking_primary", AccountType.Debit)
        createTestAccount("savings_primary", AccountType.Debit)

        when:
        def accounts = queryController.accounts(null)

        then:
        accounts != null
        accounts.size() >= 2
        accounts.any { it.accountNameOwner == "checking_primary" }
        accounts.any { it.accountNameOwner == "savings_primary" }
    }

    def "fetch accounts by type via query controller"() {
        given:
        createTestAccount("credit_card", AccountType.Credit)
        createTestAccount("debit_account", AccountType.Debit)

        when:
        def creditAccounts = queryController.accounts(AccountType.Credit)

        then:
        creditAccounts != null
        creditAccounts.size() >= 1
        creditAccounts.every { it.accountType == AccountType.Credit }
        creditAccounts.any { it.accountNameOwner == "credit_card" }
    }

    def "fetch account by name via query controller"() {
        given:
        def savedAccount = createTestAccount("entertain_account", AccountType.Debit)

        when:
        def result = queryController.account("entertain_account")

        then:
        result != null
        result.accountId == savedAccount.accountId
        result.accountNameOwner == "entertain_account"
        result.accountType == AccountType.Debit
        result.activeStatus == true
    }

    def "handle account not found via query controller"() {
        expect:
        queryController.account("nonexist_account") == null
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
