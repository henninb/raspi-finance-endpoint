package finance.domain

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import finance.helpers.PaymentBuilder
import javax.validation.ConstraintViolation
import javax.validation.Validator
import javax.validation.ValidatorFactory

class PaymentSpec extends Specification {
    ValidatorFactory validatorFactory
    Validator validator
    ObjectMapper mapper = new ObjectMapper()

    
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
