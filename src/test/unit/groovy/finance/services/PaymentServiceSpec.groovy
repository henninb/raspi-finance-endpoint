package finance.services

import finance.domain.Parameter
import finance.domain.Payment
import finance.helpers.PaymentBuilder
import finance.repositories.ParameterRepository
import finance.repositories.PaymentRepository
import spock.lang.Specification

import javax.validation.Validator

class PaymentServiceSpec extends BaseServiceSpec {
    protected ParameterService mockParameterService = new ParameterService(parameterRepositoryMock, meterServiceMock)
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
        Payment payment = PaymentBuilder.builder().build()
        Parameter parameter = new Parameter()
        parameter.parameterValue = 'val'
        parameter.parameterName = 'payment_account'

        when:
        Boolean isInserted = paymentService.insertPayment(payment)

        then:
        isInserted.is(true)
        2 * transactionServiceMock.insertTransaction(_)
        1 * parameterRepositoryMock.findByParameterName(parameter.parameterName) >> Optional.of(parameter)
        1 * validatorMock.validate(_) >> ([] as Set)
        1 * paymentRepositoryMock.saveAndFlush(payment)
        0 * _
    }

    void 'test insertPayment - findByParameterName throws an exception'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        Parameter parameter = new Parameter()
        parameter.parameterValue = 'val'
        parameter.parameterName = 'payment_account'

        when:
        paymentService.insertPayment(payment)

        then:
        1 * parameterRepositoryMock.findByParameterName(parameter.parameterName) >> Optional.empty()
        1 * validatorMock.validate(_) >> ([] as Set)
        RuntimeException ex = thrown()
        ex.message.contains('failed to read the parameter ')
        0 * _
    }
}
