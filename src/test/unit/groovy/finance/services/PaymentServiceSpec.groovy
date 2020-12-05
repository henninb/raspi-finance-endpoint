package finance.services

import finance.domain.Parameter
import finance.domain.Payment
import finance.helpers.PaymentBuilder
import finance.repositories.ParameterRepository
import finance.repositories.PaymentRepository
import spock.lang.Specification

import javax.validation.Validator

class PaymentServiceSpec extends Specification {
    protected PaymentRepository mockPaymentRepository = GroovyMock(PaymentRepository)
    protected ParameterRepository mockParameterRepository = GroovyMock(ParameterRepository)
    protected TransactionService mockTransactionService = GroovyMock(TransactionService)
    protected MeterService mockMeterService = GroovyMock(MeterService)
    protected ParameterService mockParameterService = new ParameterService(mockParameterRepository, mockMeterService)
    protected Validator mockValidator = GroovyMock(Validator)
    protected MeterService mockkMeterService = GroovyMock(MeterService)
    protected PaymentService paymentService = new PaymentService(mockPaymentRepository, mockTransactionService, mockParameterService, mockValidator, mockMeterService)

    void 'test findAll payments empty'() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        List<Payment> payments = []
        payments.add(payment)

        when:
        List<Payment> results = paymentService.findAllPayments()

        then:
        results.size() == 1
        1 * mockPaymentRepository.findAll() >> payments
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
        2 * mockTransactionService.insertTransaction(_)
        1 * mockParameterRepository.findByParameterName('payment_account') >> Optional.of(parameter)
        1 * mockValidator.validate(_) >> new HashSet()
        1 * mockPaymentRepository.save(payment)
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
        1 * mockParameterRepository.findByParameterName('payment_account') >> Optional.empty()
        1 * mockValidator.validate(_) >> new HashSet()
        RuntimeException ex = thrown()
        ex.message.contains('failed to read the parm ')
        0 * _
    }
}
