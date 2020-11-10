package finance.services

import finance.domain.Parm
import finance.domain.Payment
import finance.helpers.PaymentBuilder
import finance.repositories.ParmRepository
import finance.repositories.PaymentRepository
import spock.lang.Specification

import javax.validation.Validator

class PaymentServiceSpec extends Specification {
    PaymentRepository mockPaymentRepository = GroovyMock(PaymentRepository)
    ParmRepository mockParmRepository = GroovyMock(ParmRepository)
    TransactionService mockTransactionService = GroovyMock(TransactionService)
    ParmService mockParmService = new ParmService(mockParmRepository)
    Validator mockValidator = Mock(Validator)
    PaymentService paymentService = new PaymentService(mockPaymentRepository, mockTransactionService, mockParmService, mockValidator)

    def "test findAll payments empty"() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        List<Payment> payments = []
        payments.add(payment)

        when:
        def results = paymentService.findAllPayments()

        then:
        results.size() == 1
        1 * mockPaymentRepository.findAll() >> payments
        0 * _
    }

    def "test insertPayment - existing"() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        def parm = new Parm()
        parm.parmValue = 'val'
        parm.parmName = 'payment_account'

        when:
        def isInserted = paymentService.insertPayment(payment)

        then:
        isInserted.is(true)
        2 * mockTransactionService.insertTransaction(_)
        1 * mockParmRepository.findByParmName('payment_account') >> Optional.of(parm)
        1 * mockValidator.validate(_) >> new HashSet()
        1 * mockPaymentRepository.save(payment)
        0 * _
    }

    def "test insertPayment - findByParmName throws an exception"() {
        given:
        Payment payment = PaymentBuilder.builder().build()
        def parm = new Parm()
        parm.parmValue = 'val'
        parm.parmName = 'payment_account'

        when:
        paymentService.insertPayment(payment)

        then:
        1 * mockParmRepository.findByParmName('payment_account') >> Optional.empty()
        1 * mockValidator.validate(_) >> new HashSet()
        RuntimeException ex = thrown()
        ex.getMessage().contains('failed to read the parm ')
        0 * _
    }

}
