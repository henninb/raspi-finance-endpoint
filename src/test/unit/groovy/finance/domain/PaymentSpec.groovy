package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.PaymentBuilder
import finance.utils.Constants
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory
import java.sql.Date

class PaymentSpec extends Specification {
    protected ValidatorFactory validatorFactory
    protected Validator validator
    protected ObjectMapper mapper = new ObjectMapper()

    protected String jsonPayload = '{"accountNameOwner":"foo","amount":5.12, "guidSource":"abc", "guidDestination":"def", "transactionDate":"2020-11-12"}'

    void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup() {
        validatorFactory.close()
    }

    void 'test -- JSON deserialization to Payment'() {
        when:
        Payment payment = mapper.readValue(jsonPayload, Payment)

        then:
        payment.accountNameOwner == 'foo'
        payment.amount == 5.12
        payment.guidSource == 'abc'
        payment.guidDestination == 'def'
        0 * _
    }

    @Unroll
    void 'test -- JSON deserialize to Payment with invalid payload'() {
        when:
        mapper.readValue(payload, Payment)

        then:
        Exception ex = thrown(exceptionThrown)
        ex.message.contains(message)
        0 * _

        where:
        payload                | exceptionThrown          | message
        'non-jsonPayload'      | JsonParseException       | 'Unrecognized token'
        '[]'                   | MismatchedInputException | 'Cannot deserialize value of type'
        '{guidSource: "test"}' | JsonParseException       | 'was expecting double-quote to start field name'
        '{"amount": "123",}'   | JsonParseException       | 'was expecting double-quote to start field name'
    }

    void 'test validation valid payment'() {
        given:
        Payment payment = new PaymentBuilder().builder().accountNameOwner('new_brian').build()

        when:
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment)

        then:
        violations.empty
    }

    @Unroll
    void 'test validation invalid #invalidField has error expectedError'() {
        given:
        Payment payment = new PaymentBuilder().builder()
                .accountNameOwner(accountNameOwner)
                .transactionDate(transactionDate)
                .amount(amount)
                .guidDestination(guidDestination)
                .guidSource(guidSource)
                .build()

        when:
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().invalidValue == payment.properties[invalidField]

        where:
        invalidField       | accountNameOwner | transactionDate      | amount | guidDestination              | guidSource                   | expectedError                   | errorCount
        'accountNameOwner' | 'a_'             | new Date(1553645394) | 0.0    | UUID.randomUUID().toString() | UUID.randomUUID().toString() | 'size must be between 3 and 40' | 1
        'guidDestination'  | 'a_b'            | new Date(1553645394) | 0.0    | 'invalid'                    | UUID.randomUUID().toString() | Constants.MUST_BE_UUID_MESSAGE  | 1
        'guidSource'       | 'a_b'            | new Date(1553645394) | 0.0    | UUID.randomUUID().toString() | 'invalid'                    | Constants.MUST_BE_UUID_MESSAGE  | 1
    }
}
