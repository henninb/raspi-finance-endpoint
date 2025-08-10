package finance.repositories

import com.fasterxml.jackson.databind.ObjectMapper
import finance.Application
import finance.domain.Payment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ActiveProfiles("int")
@DataJpaTest
@ContextConfiguration(classes = [Application])
class PaymentJpaSpec extends Specification {

    @Autowired
    protected PaymentRepository paymentRepository

    @Autowired
    protected TestEntityManager entityManager

    protected ObjectMapper mapper = new ObjectMapper()

    protected String json = """
{"accountNameOwner": "test_brian", "amount":1.54, "transactionDate":"2020-12-02", "guidSource":"c8e5cd3c-3f70-473b-92bf-1c2e4fb338ab", "guidDestination":"e074436e-ed64-455d-be56-7421e04d467b", "sourceAccount":"source_test", "destinationAccount":"dest_test" }
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
        payment.guidSource = '11111111-1111-1111-1111-111111111111'
        payment.guidDestination = '22222222-2222-2222-2222-222222222222'
        Payment result = entityManager.persist(payment)

        when:
        paymentRepository.delete(result)

        then:
        paymentRepository.count() == 0L
    }
}
