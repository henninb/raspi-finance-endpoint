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

import java.sql.Date

@ActiveProfiles("func")
@DataJpaTest
@ContextConfiguration(classes = [Application])
class PaymentJpaSpec extends Specification {

    @Autowired
    protected PaymentRepository paymentRepository

    @Autowired
    protected TestEntityManager entityManager

    protected ObjectMapper mapper = new ObjectMapper()

    protected String json = """
{"accountNameOwner": "referenced_brian", "amount":12.99, "transactionDate":"2020-12-30", "guidSource":"ba665bc2-22b6-4123-a566-6f5ab3d796dh", "guidDestination":"ba665bc2-22b6-4123-a566-6f5ab3d796di", "sourceAccount":"referenced_brian", "destinationAccount":"bank_brian" }
"""

    void 'test payment to JSON - valid insert'() {

        given:
        Payment payment = mapper.readValue(json, Payment)

        when:
        Payment result = entityManager.persist(payment)

        then:
        paymentRepository.count() == 2L  // 1 existing payment in data.sql + 1 new payment
        result == payment
    }

    void 'test payment to JSON - valid insert and delete'() {

        given:
        Payment payment = mapper.readValue(json, Payment)
        // Use the same existing GUIDs that are already in the test data
        payment.guidSource = 'ba665bc2-22b6-4123-a566-6f5ab3d796dh'
        payment.guidDestination = 'ba665bc2-22b6-4123-a566-6f5ab3d796di'
        // Create unique values to avoid constraint violations
        payment.accountNameOwner = 'different_brian'
        payment.amount = 15.50
        payment.transactionDate = Date.valueOf('2020-12-30')
        Payment result = entityManager.persist(payment)

        when:
        paymentRepository.delete(result)

        then:
        paymentRepository.count() == 1L  // Should be back to 1 existing payment after delete
    }
}
