package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.domain.Payment
import finance.helpers.AccountBuilder
import finance.helpers.PaymentBuilder
import finance.repositories.AccountRepository
import finance.repositories.PaymentRepository
import spock.lang.Specification

import javax.validation.Validator

class PaymentServiceSpec extends Specification {
    PaymentRepository mockPaymentRepository = Mock(PaymentRepository)
    //Validator mockValidator = Mock(Validator)
    TransactionService mockTransactionService = Mock(TransactionService)
    PaymentService paymentService = new PaymentService(mockPaymentRepository, mockTransactionService)
    private ObjectMapper mapper = new ObjectMapper()


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

    def "test insertPayment- existing"() {
        given:
        Payment payment = PaymentBuilder.builder().build()

        when:
        def isInserted = paymentService.insertPayment(payment)

        then:
        isInserted.is(true)
        2 * mockTransactionService.insertTransaction(_)
        1 * mockPaymentRepository.save(payment)
        //1 * mockValidator.validate(account) >> new HashSet()
        //1 * mockAccountRepository.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        0 * _
    }

}
