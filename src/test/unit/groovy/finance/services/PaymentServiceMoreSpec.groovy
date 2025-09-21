package finance.services

import finance.domain.*
import finance.helpers.AccountBuilder
import finance.helpers.PaymentBuilder
import finance.helpers.TransactionBuilder

class PaymentServiceMoreSpec extends BaseServiceSpec {

    TransactionService transactionServiceMock = GroovyMock(TransactionService)
    IParameterService parameterServiceMock = GroovyMock(IParameterService)

    void setup() {
        paymentService.validator = validatorMock
        paymentService.meterService = meterService
        paymentService.paymentRepository = paymentRepositoryMock
        paymentService.transactionService = transactionServiceMock
        paymentService.accountService = accountServiceMock
        paymentService.parameterService = parameterServiceMock
    }

    void "findAllPayments returns sorted descending by date"() {
        given:
        def older = PaymentBuilder.builder().withTransactionDate(java.sql.Date.valueOf('2023-01-01')).build()
        def newer = PaymentBuilder.builder().withTransactionDate(java.sql.Date.valueOf('2024-01-01')).build()

        when:
        def list = paymentService.findAllPayments()

        then:
        1 * paymentRepositoryMock.findAll() >> [older, newer]
        list[0].transactionDate.after(list[1].transactionDate)
    }

    void "updatePayment with date-only change updates transactions and not amount"() {
        given:
        def existing = PaymentBuilder.builder().build()
        existing.paymentId = 9L
        def patch = new Payment(transactionDate: java.sql.Date.valueOf('2025-05-05'))

        def src = TransactionBuilder.builder().withGuid(existing.guidSource).build()
        def dst = TransactionBuilder.builder().withGuid(existing.guidDestination).build()

        when:
        def result = paymentService.updatePayment(9L, patch)

        then:
        1 * paymentRepositoryMock.findByPaymentId(9L) >> Optional.of(existing)
        1 * transactionServiceMock.findTransactionByGuid(existing.guidSource) >> Optional.of(src)
        1 * transactionServiceMock.findTransactionByGuid(existing.guidDestination) >> Optional.of(dst)
        1 * paymentRepositoryMock.findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot(existing.destinationAccount, patch.transactionDate, existing.amount, 9L) >> Optional.empty()
        2 * transactionServiceMock.updateTransaction({ Transaction t -> t.transactionDate == patch.transactionDate })
        1 * paymentRepositoryMock.saveAndFlush(_ as Payment) >> { it[0] }
        result.transactionDate == patch.transactionDate
        result.amount == existing.amount
    }

    void "updatePayment throws when missing GUIDs"() {
        given:
        def existing = PaymentBuilder.builder().build()
        existing.paymentId = 11L
        existing.guidSource = null
        existing.guidDestination = null
        def patch = PaymentBuilder.builder().withAmount(existing.amount.add(new java.math.BigDecimal('1'))).build()

        when:
        paymentService.updatePayment(11L, patch)

        then:
        1 * paymentRepositoryMock.findByPaymentId(11L) >> Optional.of(existing)
        1 * paymentRepositoryMock.findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot(existing.destinationAccount, existing.transactionDate, patch.amount, 11L) >> Optional.empty()
        thrown(jakarta.validation.ValidationException)
    }

    void "updatePayment throws when source transaction missing"() {
        given:
        def existing = PaymentBuilder.builder().withGuidSource('src-guid').withGuidDestination('dst-guid').build()
        existing.paymentId = 12L
        def patch = PaymentBuilder.builder().withAmount(existing.amount.add(new java.math.BigDecimal('1'))).build()

        when:
        paymentService.updatePayment(12L, patch)

        then:
        1 * paymentRepositoryMock.findByPaymentId(12L) >> Optional.of(existing)
        1 * paymentRepositoryMock.findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot(existing.destinationAccount, existing.transactionDate, patch.amount, 12L) >> Optional.empty()
        1 * transactionServiceMock.findTransactionByGuid('src-guid') >> Optional.empty()
        thrown(jakarta.validation.ValidationException)
    }

    void "updatePayment throws when destination transaction missing"() {
        given:
        def existing = PaymentBuilder.builder().withGuidSource('src-guid').withGuidDestination('dst-guid').build()
        existing.paymentId = 13L
        def patch = PaymentBuilder.builder().withAmount(existing.amount.add(new java.math.BigDecimal('1'))).build()
        def src = TransactionBuilder.builder().withGuid('src-guid').build()

        when:
        paymentService.updatePayment(13L, patch)

        then:
        1 * paymentRepositoryMock.findByPaymentId(13L) >> Optional.of(existing)
        1 * paymentRepositoryMock.findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot(existing.destinationAccount, existing.transactionDate, patch.amount, 13L) >> Optional.empty()
        1 * transactionServiceMock.findTransactionByGuid('src-guid') >> Optional.of(src)
        1 * transactionServiceMock.findTransactionByGuid('dst-guid') >> Optional.empty()
        thrown(jakarta.validation.ValidationException)
    }

    void "insertPayment creates accounts when missing"() {
        given:
        def payment = PaymentBuilder.builder().withAmount(25.0).build()

        when:
        def result = paymentService.insertPayment(payment)

        then:
        1 * validatorMock.validate(payment) >> ([] as Set)
        // destination account missing then created
        1 * accountServiceMock.account(payment.destinationAccount) >> Optional.empty()
        1 * accountServiceMock.insertAccount({ it.accountNameOwner == payment.destinationAccount && it.accountType == AccountType.Credit }) >> { Account a -> a }
        // source account missing then created
        1 * accountServiceMock.account(payment.sourceAccount) >> Optional.empty()
        1 * accountServiceMock.insertAccount({ it.accountNameOwner == payment.sourceAccount && it.accountType == AccountType.Credit }) >> { Account a -> a }
        // destination check again after creation
        1 * accountServiceMock.account(payment.destinationAccount) >> Optional.of(AccountBuilder.builder().withAccountType(AccountType.Credit).build())
        2 * transactionServiceMock.insertTransaction(_ as Transaction)
        1 * paymentRepositoryMock.saveAndFlush(payment) >> payment
        result.is(payment)
    }

    void "updatePayment amount change flips both transaction amounts negative when positive"() {
        given:
        def existing = PaymentBuilder.builder().withAmount(10.0).build()
        existing.paymentId = 33L
        def patch = PaymentBuilder.builder().withAmount(20.0).build()
        def src = TransactionBuilder.builder().withGuid(existing.guidSource).build()
        def dst = TransactionBuilder.builder().withGuid(existing.guidDestination).build()

        when:
        def result = paymentService.updatePayment(33L, patch)

        then:
        1 * paymentRepositoryMock.findByPaymentId(33L) >> Optional.of(existing)
        1 * paymentRepositoryMock.findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot(existing.destinationAccount, existing.transactionDate, patch.amount, 33L) >> Optional.empty()
        1 * transactionServiceMock.findTransactionByGuid(existing.guidSource) >> Optional.of(src)
        1 * transactionServiceMock.findTransactionByGuid(existing.guidDestination) >> Optional.of(dst)
        2 * transactionServiceMock.updateTransaction({ Transaction t -> t.amount == new java.math.BigDecimal('-20.0') })
        1 * paymentRepositoryMock.saveAndFlush(_ as Payment) >> { it[0] }
        result.amount == patch.amount
    }
}
