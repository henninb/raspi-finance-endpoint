package finance.controllers

import finance.domain.*
import finance.services.*
import spock.lang.Specification

class GraphQLQueryControllerSpec extends Specification {

    def accountService = Mock(AccountService)
    def categoryService = Mock(ICategoryService)
    def descriptionService = Mock(IDescriptionService)
    def paymentService = Mock(PaymentService)
    def transferService = Mock(TransferService)

    def controller = new GraphQLQueryController(accountService, categoryService, descriptionService, paymentService, transferService)

    void "accounts returns list from service"() {
        given:
        def a = new Account(accountNameOwner: 'acct')

        when:
        def result = controller.accounts()

        then:
        1 * accountService.accounts() >> [a]
        result*.accountNameOwner == ['acct']
    }

    void "account returns item or null"() {
        when:
        def found = controller.account('acct')
        def missing = controller.account('missing')

        then:
        1 * accountService.account('acct') >> Optional.of(new Account(accountNameOwner: 'acct'))
        1 * accountService.account('missing') >> Optional.empty()
        found.accountNameOwner == 'acct'
        missing == null
    }

    void "payments and transfers proxy to services"() {
        when:
        controller.payments()
        controller.transfers()

        then:
        1 * paymentService.findAllPayments() >> []
        1 * transferService.findAllTransfers() >> []
    }

    void "category and description return null when missing"() {
        when:
        def c = controller.category('x')
        def d = controller.description('y')

        then:
        1 * categoryService.findByCategoryName('x') >> Optional.empty()
        1 * descriptionService.findByDescriptionName('y') >> Optional.empty()
        c == null
        d == null
    }

    void "payment and transfer by id return null when missing"() {
        when:
        def p = controller.payment(1L)
        def t = controller.transfer(2L)

        then:
        1 * paymentService.findByPaymentId(1L) >> Optional.empty()
        1 * transferService.findByTransferId(2L) >> Optional.empty()
        p == null
        t == null
    }
}
