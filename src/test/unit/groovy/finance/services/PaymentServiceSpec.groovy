package finance.services

import finance.domain.*
import finance.helpers.AccountBuilder
import finance.helpers.PaymentBuilder
import finance.helpers.TransactionBuilder
import finance.repositories.PaymentRepository
import finance.utils.Constants
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Ignore
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException
import java.math.BigDecimal
import java.sql.Timestamp

@SuppressWarnings("GroovyAccessibility")
class PaymentServiceSpec extends BaseServiceSpec {
    // Define missing mocks used in setup
    protected TransactionService transactionServiceMock = GroovyMock(TransactionService)
    protected ParameterService parameterServiceMock = GroovyMock(ParameterService)

    void setup() {
        paymentService.validator = validatorMock
        paymentService.meterService = meterService
        paymentService.paymentRepository = paymentRepositoryMock
        paymentService.transactionService = transactionServiceMock
        paymentService.accountService = accountServiceMock
        paymentService.parameterService = parameterServiceMock
    }

    void 'test findAllPayments - success'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        List<Payment> payments = [payment]

        when:
        List<Payment> results = paymentService.findAllPayments()

        then:
        1 * paymentRepositoryMock.findAll() >> payments
        results.size() == 1
        results[0].paymentId == payment.paymentId
    }

    void 'test findAllPayments - empty'() {
        when:
        List<Payment> results = paymentService.findAllPayments()

        then:
        1 * paymentRepositoryMock.findAll() >> []
        results.isEmpty()
    }

    void 'test findByPaymentId - success'() {
        given:
        Payment payment = PaymentBuilder.builder().build()

        when:
        Optional<Payment> result = paymentService.findByPaymentId(payment.paymentId)

        then:
        1 * paymentRepositoryMock.findByPaymentId(payment.paymentId) >> Optional.of(payment)
        result.isPresent()
        result.get().paymentId == payment.paymentId
    }

    void 'test findByPaymentId - not found'() {
        given:
        long paymentId = 1L

        when:
        Optional<Payment> result = paymentService.findByPaymentId(paymentId)

        then:
        1 * paymentRepositoryMock.findByPaymentId(paymentId) >> Optional.empty()
        !result.isPresent()
    }

    void 'test deleteByPaymentId - success'() {
        given:
        Payment payment = PaymentBuilder.builder().build()

        when:
        boolean result = paymentService.deleteByPaymentId(payment.paymentId)

        then:
        1 * paymentRepositoryMock.findByPaymentId(payment.paymentId) >> Optional.of(payment)
        1 * paymentRepositoryMock.delete(payment)
        result
    }

    void 'test deleteByPaymentId - not found'() {
        given:
        long paymentId = 1L

        when:
        boolean result = paymentService.deleteByPaymentId(paymentId)

        then:
        1 * paymentRepositoryMock.findByPaymentId(paymentId) >> Optional.empty()
        0 * paymentRepositoryMock.delete(_)
        !result
    }

    void 'test updatePayment - success'() {
        given:
        Payment existingPayment = PaymentBuilder.builder().withAmount(100.0).build()
        existingPayment.paymentId = 1L
        Payment patch = PaymentBuilder.builder().withAmount(200.0).build()
        Transaction sourceTx = TransactionBuilder.builder().withGuid(existingPayment.guidSource).build()
        Transaction destTx = TransactionBuilder.builder().withGuid(existingPayment.guidDestination).build()

        when:
        Payment result = paymentService.updatePayment(existingPayment.paymentId, patch)

        then:
        1 * paymentRepositoryMock.findByPaymentId(existingPayment.paymentId) >> Optional.of(existingPayment)
        1 * transactionServiceMock.findTransactionByGuid(existingPayment.guidSource) >> Optional.of(sourceTx)
        1 * transactionServiceMock.findTransactionByGuid(existingPayment.guidDestination) >> Optional.of(destTx)
        1 * paymentRepositoryMock.findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot(
                existingPayment.destinationAccount, existingPayment.transactionDate, patch.amount, existingPayment.paymentId) >> Optional.empty()
        2 * transactionServiceMock.updateTransaction(_ as Transaction)
        1 * paymentRepositoryMock.saveAndFlush(_ as Payment) >> { Payment p -> p }
        result.amount == patch.amount
    }

    void 'test updatePayment - payment not found'() {
        given:
        long paymentId = 1L
        Payment patch = PaymentBuilder.builder().build()

        when:
        paymentService.updatePayment(paymentId, patch)

        then:
        1 * paymentRepositoryMock.findByPaymentId(paymentId) >> Optional.empty()
        thrown(ValidationException)
    }

    void 'test updatePayment - no changes'() {
        given:
        Payment existingPayment = PaymentBuilder.builder().build()
        existingPayment.paymentId = 1L
        Payment patch = new Payment() // Empty patch with no changes

        when:
        Payment result = paymentService.updatePayment(existingPayment.paymentId, patch)

        then:
        1 * paymentRepositoryMock.findByPaymentId(existingPayment.paymentId) >> Optional.of(existingPayment)
        result == existingPayment
    }

    void 'test updatePayment - duplicate payment conflict'() {
        given:
        Payment existingPayment = PaymentBuilder.builder().withAmount(100.0).build()
        existingPayment.paymentId = 1L
        Payment patch = PaymentBuilder.builder().withAmount(200.0).build()
        Payment duplicatePayment = PaymentBuilder.builder().build()
        duplicatePayment.paymentId = 2L

        when:
        paymentService.updatePayment(existingPayment.paymentId, patch)

        then:
        1 * paymentRepositoryMock.findByPaymentId(existingPayment.paymentId) >> Optional.of(existingPayment)
        1 * paymentRepositoryMock.findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot(
                existingPayment.destinationAccount, existingPayment.transactionDate, patch.amount, existingPayment.paymentId) >> Optional.of(duplicatePayment)
        thrown(DataIntegrityViolationException)
    }

    void 'test insertPayment - success'() {
        given:
        Payment payment = PaymentBuilder.builder().withAmount(50.0).build()
        Account account = AccountBuilder.builder().withAccountType(AccountType.Credit).build()
        Set<ConstraintViolation<Payment>> constraintViolations = [] as Set

        when:
        Payment result = paymentService.insertPayment(payment)

        then:
        1 * validatorMock.validate(payment) >> constraintViolations
        2 * accountServiceMock.account(payment.destinationAccount) >> Optional.of(account)
        1 * accountServiceMock.account(payment.sourceAccount) >> Optional.of(account)
        2 * transactionServiceMock.insertTransaction(_ as Transaction)
        1 * paymentRepositoryMock.saveAndFlush(payment) >> payment
        result.paymentId == payment.paymentId
    }

    void 'test insertPayment - validation failure'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        Set<ConstraintViolation<Payment>> constraintViolations = [Mock(ConstraintViolation)] as Set

        when:
        paymentService.insertPayment(payment)

        then:
        1 * validatorMock.validate(payment) >> constraintViolations
        thrown(ValidationException)
    }

    void 'test insertPayment - destination is debit account'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        Account account = AccountBuilder.builder().withAccountType(AccountType.Debit).build()
        Set<ConstraintViolation<Payment>> constraintViolations = [] as Set

        when:
        paymentService.insertPayment(payment)

        then:
        1 * validatorMock.validate(payment) >> constraintViolations
        2 * accountServiceMock.account(payment.destinationAccount) >> Optional.of(account)
        1 * accountServiceMock.account(payment.sourceAccount) >> Optional.of(account)
        thrown(ValidationException)
    }

    void 'test populateDebitTransaction - positive amount'() {
        given:
        Transaction transaction = new Transaction()
        Payment payment = PaymentBuilder.builder().withAmount(100.0).build()

        when:
        paymentService.populateDebitTransaction(transaction, payment, "test_account")

        then:
        transaction.amount == -100.0
        transaction.accountType == AccountType.Debit
        transaction.description == "payment"
    }

    void 'test populateCreditTransaction - positive amount'() {
        given:
        Transaction transaction = new Transaction()
        Payment payment = PaymentBuilder.builder().withAmount(100.0).build()

        when:
        paymentService.populateCreditTransaction(transaction, payment, "test_account")

        then:
        transaction.amount == -100.0
        transaction.accountType == AccountType.Credit
        transaction.description == "payment"
    }
}
