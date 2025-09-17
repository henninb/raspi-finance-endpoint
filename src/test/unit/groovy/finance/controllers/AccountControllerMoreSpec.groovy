package finance.controllers

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.TransactionState
import finance.services.AccountService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class AccountControllerMoreSpec extends Specification {

    def service = Mock(AccountService)
    def controller = new AccountController(service)

    void "findAllActive returns list and calls updateTotals"() {
        given:
        def a = new Account(accountNameOwner: 'acct', accountType: AccountType.Credit, activeStatus: true)

        when:
        def resp = controller.findAllActive()

        then:
        1 * service.updateTotalsForAllAccounts() >> true
        1 * service.accounts() >> [a]
        resp.statusCode == HttpStatus.OK
        resp.body*.accountNameOwner == ['acct']
    }

    void "findById returns 404 when missing (standardized)"() {
        when:
        controller.findById('missing')

        then:
        1 * service.account('missing') >> Optional.empty()
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    void "save creates account and returns 201"() {
        given:
        def a = new Account(accountNameOwner: 'new', accountType: AccountType.Credit)

        when:
        def resp = controller.save(a)

        then:
        1 * service.insertAccount(a) >> a
        resp.statusCode == HttpStatus.CREATED
        resp.body.accountNameOwner == 'new'
    }

    void "update returns 404 when target missing (standardized)"() {
        given:
        def a = new Account(accountNameOwner: 'x')

        when:
        controller.update('x', a)

        then:
        1 * service.account('x') >> Optional.empty()
        thrown(ResponseStatusException)
    }

    void "update success when found (standardized)"() {
        given:
        def a = new Account(accountNameOwner: 'x')

        when:
        def resp = controller.update('x', a)

        then:
        1 * service.account('x') >> Optional.of(a)
        1 * service.updateAccount(a) >> a
        resp.statusCode == HttpStatus.OK
    }

    void "deleteById returns 404 when entity missing (standardized)"() {
        when:
        controller.deleteById('gone')

        then:
        1 * service.account('gone') >> Optional.empty()
        thrown(ResponseStatusException)
    }

    void "selectPaymentRequired returns list and 200"() {
        when:
        def resp = controller.selectPaymentRequired()

        then:
        1 * service.findAccountsThatRequirePayment() >> []
        resp.statusCode == HttpStatus.OK
    }

    void "computeAccountTotals maps exception to 500"() {
        when:
        controller.computeAccountTotals()

        then:
        1 * service.sumOfAllTransactionsByTransactionState(TransactionState.Cleared) >> { throw new RuntimeException('boom') }
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    void "activateAccount returns 404 for EntityNotFoundException"() {
        when:
        controller.activateAccount('ghost')

        then:
        1 * service.activateAccount('ghost') >> { throw new jakarta.persistence.EntityNotFoundException('nope') }
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    void "activateAccount returns 500 for other exceptions"() {
        when:
        controller.activateAccount('err')

        then:
        1 * service.activateAccount('err') >> { throw new RuntimeException('x') }
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}

