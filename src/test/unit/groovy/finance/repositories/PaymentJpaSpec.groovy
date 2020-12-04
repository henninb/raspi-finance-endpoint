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
    protected PaymentRepository paymentRepository

    @Autowired
    protected TestEntityManager entityManager

    protected ObjectMapper mapper = new ObjectMapper()

    String json = """
{"accountNameOwner": "test_brian", "amount":1.54, "transactionDate":1593981072000, "guidSource":"c8e5cd3c-3f70-473b-92bf-1c2e4fb338ab", "guidDestination":"e074436e-ed64-455d-be56-7421e04d467b" }
"""

    void 'test payment to JSON - valid insert'() {

        given:
        Payment payment = mapper.readValue(json, Payment)

        when:
        Payment result = entityManager.persist(payment)

        then:
        paymentRepository.count() == 1L
        result == payment
    }

    void 'test payment to JSON - valid insert and delete'() {

        given:
        Payment payment = mapper.readValue(json, Payment)
        Payment result = entityManager.persist(payment)

        when:
        paymentRepository.deleteByPaymentId(result.paymentId)

        then:
        paymentRepository.count() == 0L
    }
}
