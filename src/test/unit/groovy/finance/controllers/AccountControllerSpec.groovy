package finance.controllers

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.TransactionState
import finance.services.AccountService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject

class AccountControllerSpec extends Specification {

    AccountService accountService = GroovyMock(AccountService)

    @Subject
    AccountController controller = new AccountController(accountService)

    def "accounts returns list when found"() {
        given:
        List<Account> accounts = [new Account(accountId: 1L, accountNameOwner: 'test', accountType: AccountType.Credit, activeStatus: true)]

        when:
        ResponseEntity<List<Account>> response = controller.accounts()

        then:
        1 * accountService.updateTotalsForAllAccounts()
        1 * accountService.accounts() >> accounts
        response.statusCode == HttpStatus.OK
        response.body == accounts
    }

    def "accounts returns NOT_FOUND when empty"() {
        when:
        controller.accounts()

        then:
        1 * accountService.updateTotalsForAllAccounts()
        1 * accountService.accounts() >> []
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == 'No active accounts found'
    }

    def "computeAccountTotals returns aggregated totals"() {
        when:
        ResponseEntity<Map<String,String>> response = controller.computeAccountTotals()

        then:
        1 * accountService.sumOfAllTransactionsByTransactionState(TransactionState.Cleared) >> 10.00G
        1 * accountService.sumOfAllTransactionsByTransactionState(TransactionState.Future) >> 1.50G
        1 * accountService.sumOfAllTransactionsByTransactionState(TransactionState.Outstanding) >> 3.25G
        response.statusCode == HttpStatus.OK
        response.body.totalsCleared == '10.00'
        response.body.totalsFuture == '1.50'
        response.body.totalsOutstanding == '3.25'
        response.body.totals == '14.75'
    }

    def "account returns account when found"() {
        given:
        Account account = new Account(accountId: 1L, accountNameOwner: 'test', accountType: AccountType.Credit, activeStatus: true)

        when:
        ResponseEntity<Account> response = controller.account('test')

        then:
        1 * accountService.account('test') >> Optional.of(account)
        response.statusCode == HttpStatus.OK
        response.body == account
    }

    def "account returns NOT_FOUND when service empty"() {
        when:
        controller.account('unknown')

        then:
        1 * accountService.account('unknown') >> Optional.empty()
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == 'Account not found: unknown'
    }
}

