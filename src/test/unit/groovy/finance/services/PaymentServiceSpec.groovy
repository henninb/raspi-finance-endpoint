package finance.services

import finance.domain.*
import finance.helpers.AccountBuilder
import finance.helpers.PaymentBuilder
import finance.helpers.TransactionBuilder
import finance.utils.Constants
import spock.lang.Ignore
import jakarta.validation.ConstraintViolation

@SuppressWarnings("GroovyAccessibility")
class PaymentServiceSpec extends BaseServiceSpec {

    void setup() {
        paymentService.validator = validatorMock
        paymentService.meterService = meterService
    }

    void 'test findAll payments empty'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        List<Payment> payments = []
        payments.add(payment)

        when:
        List<Payment> results = paymentService.findAllPayments()

        then:
        results.size() == 1
        1 * paymentRepositoryMock.findAll() >> payments
        0 * _
    }

    @Ignore("needs to be fixed")
    void 'test insertPayment - existing'() {
        given:
        Payment payment = PaymentBuilder.builder().withAmount(5.0).build()
        Transaction transaction = TransactionBuilder.builder().build()
        Account account = AccountBuilder.builder().build()
        Parameter parameter = new Parameter()
        parameter.parameterValue = 'val'
        parameter.parameterName = 'payment_account'
        Set<ConstraintViolation<Payment>> constraintViolations = validator.validate(payment)

        when:
        Payment paymentInserted = paymentService.insertPaymentNew(payment)

        then:
        //thrown(RuntimeException)
        paymentInserted.destinationAccount == payment.destinationAccount
        1 * accountRepositoryMock.findByAccountNameOwner(payment.destinationAccount) >> Optional.of(account)
        1 * validatorMock.validate(_ as Payment) >> constraintViolations
        1 * validatorMock.validate({ Transaction transactionDebit ->
            assert transactionDebit.category == 'bill_pay'
            assert transactionDebit.description == 'payment'
            assert transactionDebit.notes == 'to ' + payment.destinationAccount
            assert transactionDebit.amount == (payment.amount * -1.0)
            assert transactionDebit.accountType == AccountType.Debit
        }) >> [].toSet()
        1 * validatorMock.validate({ Transaction transactionCredit ->
            assert transactionCredit.category == 'bill_pay'
            assert transactionCredit.description == 'payment'
            assert transactionCredit.notes == 'from ' + payment.sourceAccount
            assert transactionCredit.amount == (payment.amount * -1.0)
            assert transactionCredit.accountType == AccountType.Credit
        }) >> [].toSet()
        2 * transactionRepositoryMock.findByGuid(_ as String) >> Optional.of(transaction)
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER, transaction.accountNameOwner)) >> counter
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER, transaction.accountNameOwner)) >> counter
        2 * counter.increment()
        1 * paymentRepositoryMock.saveAndFlush(payment) >> payment
        0 * _
    }

    void 'test insertPayment - findByParameterName throws an exception'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        Set<ConstraintViolation<Payment>> constraintViolations = [].toSet()

        when:
        paymentService.insertPaymentNew(payment)

        then:
        thrown(RuntimeException)
        1 * validatorMock.validate(payment) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(payment.destinationAccount) >> Optional.empty()
        _ * _  // Allow any other interactions (logging, etc.)
    }
}
