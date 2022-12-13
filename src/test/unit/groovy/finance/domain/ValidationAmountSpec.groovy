package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.ValidationAmountBuilder
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation

import static finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import static finance.utils.Constants.FILED_MUST_BE_GREATER_THAN_ZERO_MESSAGE

class ValidationAmountSpec extends BaseDomainSpec {

    protected String jsonPayload = '{"accountNameOwner":"chase_brian", "amount":1.23, "activeStatus":true, "transactionState":"cleared"}'

    void 'test -- JSON serialization to ValidationAmount'() {

        when:
        ValidationAmount validationAmount = mapper.readValue(jsonPayload, ValidationAmount)

        then:
        validationAmount.amount == 1.23
        0 * _
    }

    @Unroll
    void 'test -- JSON deserialize to ValidationAmount with invalid payload'() {
        when:
        mapper.readValue(payload, ValidationAmount)

        then:
        Exception ex = thrown(exceptionThrown)
        ex.message.contains(message)
        0 * _

        where:
        payload                   | exceptionThrown          | message
        'non-jsonPayload'         | JsonParseException       | 'Unrecognized token'
        '[]'                      | MismatchedInputException | 'Cannot deserialize value of type'
        '{"amount": "123",}'      | JsonParseException       | 'was expecting double-quote to start field name'
        '{"activeStatus": "abc"}' | InvalidFormatException   | 'Cannot deserialize value of type'
    }

    void 'test validation valid validationAmount'() {
        given:
        ValidationAmount validationAmount = mapper.readValue(jsonPayload, ValidationAmount)

        when:
        Set<ConstraintViolation<ValidationAmount>> violations = validator.validate(validationAmount)

        then:
        violations.empty
    }

    @Unroll
    void 'test ValidationAmount validation invalid #invalidField has error expectedError'() {
        given:
        ValidationAmount validationAmount = new ValidationAmountBuilder().builder()
                .withAmount(amount)
                .withAccountId(accountId)
        //.withValidationDate(validationDate)
                .withTransactionState(transactionState)
                .withActiveStatus(activeStatus)
                .build()

        when:
        Set<ConstraintViolation<ValidationAmount>> violations = validator.validate(validationAmount)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().invalidValue == validationAmount.properties[invalidField]

        where:
        invalidField       | accountId | amount | transactionState         | activeStatus | expectedError                           | errorCount
        //'validationAmount' | 1         | 3.45155  | TransactionState.Cleared | true         | FIELD_MUST_BE_A_CURRENCY_MESSAGE        | 1
        'accountId'        | -1        | 3.45   | TransactionState.Cleared | true         | FILED_MUST_BE_GREATER_THAN_ZERO_MESSAGE | 1
    }
}