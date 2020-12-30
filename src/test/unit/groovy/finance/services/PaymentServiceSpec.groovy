package finance.services

import finance.domain.AccountType
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.Transaction
import finance.helpers.PaymentBuilder
import finance.repositories.ParameterRepository
import finance.repositories.PaymentRepository
import spock.lang.Specification

import javax.validation.ConstraintViolation
import javax.validation.Validator

class PaymentServiceSpec extends BaseServiceSpec {
    protected ParameterService mockParameterService = new ParameterService(parameterRepositoryMock, validatorMock, meterServiceMock)
    protected PaymentService paymentService = new PaymentService(paymentRepositoryMock, transactionServiceMock, mockParameterService, validatorMock, meterServiceMock)

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
        Payment payment = PaymentBuilder.builder().amount(5.0).build()
        Parameter parameter = new Parameter()
        parameter.parameterValue = 'val'
        parameter.parameterName = 'payment_account'
        Set<ConstraintViolation<Payment>> constraintViolations = validator.validate(payment)

        when:
        Boolean isInserted = paymentService.insertPayment(payment)

        then:
        isInserted.is(true)
        1 * transactionServiceMock.insertTransaction({ Transaction transactionDebit ->
            assert transactionDebit.category == 'bill_pay'
            assert transactionDebit.description == 'payment'
            assert transactionDebit.notes == 'to ' + payment.accountNameOwner
            assert transactionDebit.amount == (payment.amount * -1.0)
            assert transactionDebit.accountType == AccountType.Debit
        })
        1 * transactionServiceMock.insertTransaction({ Transaction transactionCredit ->
            assert transactionCredit.category == 'bill_pay'
            assert transactionCredit.description == 'payment'
            assert transactionCredit.notes == 'from ' + parameter.parameterValue
            assert transactionCredit.amount ==  (payment.amount * -1.0)
            assert transactionCredit.accountType == AccountType.Credit
        })
        1 * parameterRepositoryMock.findByParameterName(parameter.parameterName) >> Optional.of(parameter)
        1 * validatorMock.validate(_ as Payment) >> constraintViolations
        1 * paymentRepositoryMock.saveAndFlush(payment)
        0 * _
    }

    void 'test insertPayment - findByParameterName throws an exception'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        Parameter parameter = new Parameter()
        parameter.parameterValue = 'val'
        parameter.parameterName = 'payment_account'
        Set<ConstraintViolation<Payment>> constraintViolations = validator.validate(payment)

        when:
        paymentService.insertPayment(payment)

        then:
        1 * parameterRepositoryMock.findByParameterName(parameter.parameterName) >> Optional.empty()
        1 * validatorMock.validate(_) >> constraintViolations
        RuntimeException ex = thrown()
        ex.message.contains('failed to read the parameter ')
        0 * _
    }
}
