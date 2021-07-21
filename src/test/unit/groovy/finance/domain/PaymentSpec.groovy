package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.PaymentBuilder
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import java.sql.Date

import static finance.utils.Constants.FIELD_MUST_BE_UUID_MESSAGE
import static finance.utils.Constants.FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE

class PaymentSpec extends BaseDomainSpec {
    protected String jsonPayload = '{"accountNameOwner":"foo_test","amount":5.12, "guidSource":"78f65481-f351-4142-aff6-73e99d2a286d", "guidDestination":"0db56665-0d47-414e-93c5-e5ae4c5e4299", "transactionDate":"2020-11-12"}'

    void 'test -- JSON deserialization to Payment'() {
        when:
        Payment payment = mapper.readValue(jsonPayload, Payment)

        then:
        payment.accountNameOwner == 'foo_test'
        payment.amount == 5.12
        payment.guidSource == '78f65481-f351-4142-aff6-73e99d2a286d'
        payment.guidDestination == '0db56665-0d47-414e-93c5-e5ae4c5e4299'
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
        Payment payment = mapper.readValue(jsonPayload, Payment)

        when:
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment)

        then:
        violations.empty
    }

    @Unroll
    void 'test validation invalid #invalidField has error expectedError'() {
        given:
        Payment payment = new PaymentBuilder().builder()
                .withAccountNameOwner(accountNameOwner)
                .withTransactionDate(transactionDate)
                .withAmount(amount)
                .withGuidDestination(guidDestination)
                .withGuidSource(guidSource)
                .build()

        when:
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().invalidValue == payment.properties[invalidField]

        where:
        invalidField       | accountNameOwner | transactionDate            | amount | guidDestination              | guidSource                   | expectedError                                 | errorCount
        'accountNameOwner' | 'a_'             | Date.valueOf('2020-10-15') | 0.0    | UUID.randomUUID().toString() | UUID.randomUUID().toString() | FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE | 1
        'guidDestination'  | 'a_b'            | Date.valueOf('2020-10-16') | 0.0    | 'invalid'                    | UUID.randomUUID().toString() | FIELD_MUST_BE_UUID_MESSAGE                    | 1
        'guidSource'       | 'a_b'            | Date.valueOf('2020-10-17') | 0.0    | UUID.randomUUID().toString() | 'invalid'                    | FIELD_MUST_BE_UUID_MESSAGE                    | 1
    }
}
