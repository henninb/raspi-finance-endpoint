package finance.repositories

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Payment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles("unit")
@DataJpaTest
class PaymentJpaSpec extends Specification {

    @Autowired
    PaymentRepository paymentRepository

    @Autowired
    TestEntityManager entityManager

    private ObjectMapper mapper = new ObjectMapper()

    def json = """
{"accountNameOwner": "test_brian", "amount":1.54, "transactionDate":1593981072000, "guidSource":"c8e5cd3c-3f70-473b-92bf-1c2e4fb338ab", "guidDestination":"e074436e-ed64-455d-be56-7421e04d467b" }
"""

    def setupSpec() {

    }

    def "test payment to JSON - valid insert"() {

        given:
        Payment payment = mapper.readValue(json, Payment.class)

        when:
        def result = entityManager.persist(payment)

        then:
        paymentRepository.count() == 1L
        result == payment
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
