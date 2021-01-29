package finance.services

import finance.domain.*
import finance.helpers.AccountBuilder
import finance.helpers.PaymentBuilder
import finance.helpers.TransactionBuilder
import finance.utils.Constants

import javax.validation.ConstraintViolation

@SuppressWarnings("GroovyAccessibility")
class PaymentServiceSpec extends BaseServiceSpec {

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
        Boolean isInserted = paymentService.insertPayment(payment)

        then:
        isInserted.is(true)
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * parameterRepositoryMock.findByParameterName(parameter.parameterName) >> Optional.of(parameter)
        1 * validatorMock.validate(_ as Payment) >> constraintViolations
        1 * validatorMock.validate({ Transaction transactionDebit ->
            assert transactionDebit.category == 'bill_pay'
            assert transactionDebit.description == 'payment'
            assert transactionDebit.notes == 'to ' + payment.accountNameOwner
            assert transactionDebit.amount == (payment.amount * -1.0)
            assert transactionDebit.accountType == AccountType.Debit
        }) >> [].toSet()
        1 * validatorMock.validate({ Transaction transactionCredit ->
            assert transactionCredit.category == 'bill_pay'
            assert transactionCredit.description == 'payment'
            assert transactionCredit.notes == 'from ' + parameter.parameterValue
            assert transactionCredit.amount == (payment.amount * -1.0)
            assert transactionCredit.accountType == AccountType.Credit
        }) >> [].toSet()
        2 * transactionRepositoryMock.findByGuid(_ as String) >> Optional.of(transaction)
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER, transaction.accountNameOwner)) >> counter
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER, transaction.accountNameOwner)) >> counter
        2 * counter.increment()
        1 * paymentRepositoryMock.saveAndFlush(payment)
        0 * _
    }

    void 'test insertPayment - findByParameterName throws an exception'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        Account account = AccountBuilder.builder().build()
        Parameter parameter = new Parameter()
        parameter.parameterValue = 'val'
        parameter.parameterName = 'payment_account'
        Set<ConstraintViolation<Payment>> constraintViolations = validator.validate(payment)

        when:
        paymentService.insertPayment(payment)

        then:
        thrown(RuntimeException)
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * parameterRepositoryMock.findByParameterName(parameter.parameterName) >> Optional.empty()
        1 * validatorMock.validate(payment) >> constraintViolations
        0 * _
    }
}
