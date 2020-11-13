package finance.domain

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

class PaymentSpec extends Specification {
    ValidatorFactory validatorFactory
    Validator validator
    private ObjectMapper mapper = new ObjectMapper ()

    def jsonPayload = """
{"accountNameOwner":"foo","amount":5.12, "guidSource":"abc", "guidDestination":"def", "transactionDate":"2020-11-12"}
"""
    void setup () {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup () {
        validatorFactory.close()
    }

    def "test -- JSON serialization to Payment"() {
        when:
        Payment payment = mapper.readValue(jsonPayload, Payment.class)

        then:
        payment.accountNameOwner == "foo"
        payment.amount == 5.12
        payment.guidSource == "abc"
        payment.guidDestination == "def"
        //payment.transactionDate
        0 * _
    }

//    def "test validation valid payment"() {
//        given:
//        Payment payment = new PaymentBuilder().accountNameOwner("new_brian").build()
//
//        when:
//        Set<ConstraintViolation<Payment>> violations = validator.validate(payment)
//
//        then:
//        violations.isEmpty()
//    }
}
