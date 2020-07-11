package finance.repositories

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.domain.Payment
import finance.domain.Transaction
import finance.helpers.AccountBuilder
import finance.helpers.TransactionBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import spock.lang.Specification

@DataJpaTest
class PaymentJpaSpec extends Specification {

    @Autowired
    PaymentRepository paymentRepository

    @Autowired
    TestEntityManager entityManager

    private ObjectMapper mapper = new ObjectMapper()

    def json = """
{"accountNameOwner": "test_brian", "amount":1.54, "transactionDate":1593981072000 }
"""

    def "test payment to JSON - valid insert"() {

        given:
        Payment payment = mapper.readValue(json, Payment.class)

        when:
        entityManager.persist(payment)
        then:
        paymentRepository.count() == 1L
    }

    def "test payment to JSON - valid insert and delete"() {

        given:
        Payment payment = mapper.readValue(json, Payment.class)
        def result = entityManager.persist(payment)

        when:
        paymentRepository.deleteByPaymentId(result.paymentId)

        then:
        paymentRepository.count() == 0L
    }
}
