package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.ValidationAmountBuilder
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation
import java.sql.Timestamp

import static finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import static finance.utils.Constants.FILED_MUST_BE_GREATER_THAN_ZERO_MESSAGE

class ValidationAmountSpec extends BaseDomainSpec {
    protected String jsonPayload = '{"validationId":1,"accountId":1,"amount":1.23,"activeStatus":true,"transactionState":"cleared","validationDate":1700000000000}'

    void 'test -- JSON serialization to ValidationAmount'() {

        when:
        ValidationAmount validationAmount = mapper.readValue(jsonPayload, ValidationAmount)

        then:
        validationAmount.amount == 1.23
        validationAmount.validationDate.time == 1700000000000L
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

    def "test equals and hashCode"() {
        given:
        Timestamp fixedDate = new Timestamp(1700000000000L)
        ValidationAmount va1 = new ValidationAmountBuilder().withAccountId(1L).withAmount(10.0G).withValidationDate(fixedDate).withDateAdded(fixedDate).withDateUpdated(fixedDate).build()
        ValidationAmount va2 = new ValidationAmountBuilder().withAccountId(1L).withAmount(10.0G).withValidationDate(fixedDate).withDateAdded(fixedDate).withDateUpdated(fixedDate).build()

        va1.validationId = 1L
        va2.validationId = 1L
        ValidationAmount va3 = new ValidationAmountBuilder().withAccountId(2L).build()
        va3.validationId = 2L

        expect:
        va1 == va2
        va1.hashCode() == va2.hashCode()
        va1 != va3
        va1 != null
    }

    def "test toString"() {
        given:
        ValidationAmount va = new ValidationAmountBuilder().withAccountId(123L).withAmount(50.50G).build()

        when:
        String result = va.toString()

        then:
        result.contains('"accountId":123')
        result.contains('"amount":50.5')
    }

    def "test ValidationAmount dateAdded and dateUpdated getters"() {
        given:
        def ts = new Timestamp(System.currentTimeMillis())
        ValidationAmount va = new ValidationAmountBuilder().withDateAdded(ts).withDateUpdated(ts).build()

        expect:
        va.dateAdded == ts
        va.dateUpdated == ts
    }

    def "test ValidationAmount default constructor field defaults"() {
        when:
        ValidationAmount va = new ValidationAmount()

        then:
        va.validationId == 0L
        va.owner == ''
        va.accountId == 0L
        va.activeStatus == true
        va.transactionState == TransactionState.Undefined
        va.dateAdded != null
        va.dateUpdated != null
        0 * _
    }

    void 'test ValidationAmount rejects amount with too many fraction digits'() {
        given:
        ValidationAmount validationAmount = new ValidationAmountBuilder()
                .withAmount(new BigDecimal("3.45155"))
                .withAccountId(1L)
                .withTransactionState(TransactionState.Cleared)
                .build()

        when:
        Set<ConstraintViolation<ValidationAmount>> violations = validator.validate(validationAmount)

        then:
        !violations.empty
        violations.any { it.message == FIELD_MUST_BE_A_CURRENCY_MESSAGE }
        0 * _
    }

    void 'test ValidationAmount rejects owner exceeding 100 characters'() {
        given:
        ValidationAmount validationAmount = new ValidationAmountBuilder()
                .withOwner('a' * 101)
                .withAmount(new BigDecimal("1.00"))
                .withAccountId(1L)
                .build()

        when:
        Set<ConstraintViolation<ValidationAmount>> violations = validator.validate(validationAmount)

        then:
        !violations.empty
        violations.any { it.propertyPath.toString() == 'owner' }
        0 * _
    }

    void 'test ValidationAmount rejects negative validationId'() {
        given:
        ValidationAmount validationAmount = new ValidationAmount()
        validationAmount.validationId = -1L
        validationAmount.accountId = 1L
        validationAmount.amount = new BigDecimal("1.00")
        validationAmount.transactionState = TransactionState.Cleared
        validationAmount.validationDate = new Timestamp(System.currentTimeMillis())

        when:
        Set<ConstraintViolation<ValidationAmount>> violations = validator.validate(validationAmount)

        then:
        !violations.empty
        violations.any { it.propertyPath.toString() == 'validationId' }
        0 * _
    }
}
