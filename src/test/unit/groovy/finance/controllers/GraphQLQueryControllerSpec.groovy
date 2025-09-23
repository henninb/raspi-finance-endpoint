package finance.controllers

import finance.domain.*
import finance.services.*
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.DescriptionRepository
import finance.repositories.PaymentRepository
import finance.repositories.TransactionRepository
import finance.repositories.TransferRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.validation.Validation
import spock.lang.Specification

class GraphQLQueryControllerSpec extends Specification {

    // Create meter service and validator like BaseServiceSpec
    def meterService = new MeterService(new SimpleMeterRegistry())
    def validator = Validation.buildDefaultValidatorFactory().getValidator()

    // Use real StandardizedAccountService with repository mock (final Kotlin class)
    def accountRepositoryMock = Mock(AccountRepository)
    def accountService = new StandardizedAccountService(accountRepositoryMock)
    // Use a real StandardizedCategoryService with repository mocks (final Kotlin class)
    def categoryRepositoryMock = Mock(CategoryRepository)
    def categoryTxRepositoryMock = Mock(TransactionRepository)
    def categoryService = new StandardizedCategoryService(categoryRepositoryMock, categoryTxRepositoryMock)
    def descriptionRepositoryMock = Mock(DescriptionRepository)
    def descriptionTxRepositoryMock = Mock(TransactionRepository)
    def descriptionService = new StandardizedDescriptionService(descriptionRepositoryMock, descriptionTxRepositoryMock)
    // Use real StandardizedPaymentService with repository mocks (final Kotlin class)
    def paymentRepositoryMock = Mock(PaymentRepository)
    def paymentTxServiceMock = Mock(ITransactionService)
    def paymentAccountServiceMock = GroovyMock(StandardizedAccountService)
    def paymentService = new StandardizedPaymentService(paymentRepositoryMock, paymentTxServiceMock, paymentAccountServiceMock)
    // Use real StandardizedTransferService with repository mocks (final Kotlin class)
    def transferRepositoryMock = Mock(TransferRepository)
    def transferTxServiceMock = Mock(ITransactionService)
    def transferAccountServiceMock = GroovyMock(StandardizedAccountService)
    def transferService = new StandardizedTransferService(transferRepositoryMock, transferTxServiceMock, transferAccountServiceMock)

    def controller = new GraphQLQueryController(accountService, categoryService, descriptionService, paymentService, transferService)

    void setup() {
        // Initialize services with required dependencies
        accountService.meterService = meterService
        accountService.validator = validator
        paymentService.meterService = meterService
        paymentService.validator = validator
        transferService.meterService = meterService
        transferService.validator = validator
    }

    void "accounts returns list from service"() {
        given:
        def a = new Account(accountNameOwner: 'acct')

        when:
        def result = controller.accounts()

        then:
        1 * accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> [a]
        result*.accountNameOwner == ['acct']
    }

    void "account returns item or null"() {
        when:
        def found = controller.account('acct')
        def missing = controller.account('missing')

        then:
        1 * accountRepositoryMock.findByAccountNameOwner('acct') >> Optional.of(new Account(accountNameOwner: 'acct'))
        1 * accountRepositoryMock.findByAccountNameOwner('missing') >> Optional.empty()
        found.accountNameOwner == 'acct'
        missing == null
    }

    void "payments and transfers proxy to services"() {
        when:
        controller.payments()
        controller.transfers()

        then:
        1 * paymentRepositoryMock.findAll() >> []
        1 * transferRepositoryMock.findAll() >> []
    }

    void "category and description return null when missing"() {
        when:
        def c = controller.category('x')
        def d = controller.description('y')

        then:
        // Category service delegates to repository; empty should yield NotFound -> null from controller
        1 * categoryRepositoryMock.findByCategoryName('x') >> Optional.empty()
        1 * descriptionRepositoryMock.findByDescriptionName('y') >> Optional.empty()
        c == null
        d == null
    }

    void "payment and transfer by id return null when missing"() {
        when:
        def p = controller.payment(1L)
        def t = controller.transfer(2L)

        then:
        1 * paymentRepositoryMock.findByPaymentId(1L) >> Optional.empty()
        1 * transferRepositoryMock.findByTransferId(2L) >> Optional.empty()
        p == null
        t == null
    }
}
