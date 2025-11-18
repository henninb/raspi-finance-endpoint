package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.KotlinInvalidNullException
import finance.helpers.DescriptionBuilder
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation

import static finance.utils.Constants.FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE

class DescriptionSpec extends BaseDomainSpec {
    protected String jsonPayload = '{"descriptionName":"bar", "activeStatus":true}'

    void 'test -- JSON serialization to Description'() {

        when:
        Description description = mapper.readValue(jsonPayload, Description)

        then:
        description.descriptionName == "bar"
        0 * _
    }

    @Unroll
    void 'test -- JSON deserialize to Description with invalid payload'() {
        when:
        mapper.readValue(payload, Description)

        then:
        Exception ex = thrown(exceptionThrown)
        ex.message.contains(message)
        0 * _

        where:
        payload                     | exceptionThrown          | message
        'non-jsonPayload'           | JsonParseException       | 'Unrecognized token'
        '[]'                        | MismatchedInputException | 'Cannot deserialize value of type'
        '{descriptionName: "test"}' | JsonParseException       | 'was expecting double-quote to start field name'
        '{"activeStatus": "abc"}'   | InvalidFormatException   | 'Cannot deserialize value of type'
    }

    void 'test JSON deserialization to Description object - description is empty'() {
        given:
        String jsonPayloadBad = '{"descriptionMissing":"bar"}'

        when:
        mapper.readValue(jsonPayloadBad, Description)

        then:
        thrown(KotlinInvalidNullException)
        0 * _
    }

    void 'test validation valid description'() {
        given:
        Description description = DescriptionBuilder.builder().build()
        description.descriptionName = "foobar"

        when:
        Set<ConstraintViolation<Description>> violations = validator.validate(description)

        then:
        violations.empty
    }

    @Unroll
    void 'test Description validation invalid #invalidField has error expectedError'() {
        given:
        Description description = new DescriptionBuilder().builder()
                .withDescription(descriptionName)
                .withActiveStatus(activeStatus)
                .build()

        when:
        Set<ConstraintViolation<Description>> violations = validator.validate(description)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().invalidValue == description.properties[invalidField]

        where:
        invalidField      | descriptionName                                          | activeStatus | expectedError                               | errorCount
        'descriptionName' | ''                                                       | true         | FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE | 1
        'descriptionName' | 'ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot' | true         | FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE | 1
    }
}
