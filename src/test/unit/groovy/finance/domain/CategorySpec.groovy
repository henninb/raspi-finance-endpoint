package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.CategoryBuilder
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation

import static finance.utils.Constants.FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE

class CategorySpec extends BaseDomainSpec {


    protected String jsonPayload = '{"categoryName":"bar", "activeStatus":true}'

    void 'test -- JSON serialization to Category'() {

        when:
        Category category = mapper.readValue(jsonPayload, Category)

        then:
        category.categoryName == "bar"
        0 * _
    }

    @Unroll
    void 'test -- JSON deserialize to Category with invalid payload'() {
        when:
        mapper.readValue(payload, Category)

        then:
        Exception ex = thrown(exceptionThrown)
        ex.message.contains(message)
        0 * _

        where:
        payload                   | exceptionThrown          | message
        'non-jsonPayload'         | JsonParseException       | 'Unrecognized token'
        '[]'                      | MismatchedInputException | 'Cannot deserialize value of type'
        '{categoryName: "test"}'      | JsonParseException       | 'was expecting double-quote to start field name'
        '{"activeStatus": "abc"}' | InvalidFormatException   | 'Cannot deserialize value of type'
    }

    void 'test JSON deserialization to Category object - category is empty'() {
        given:
        // With @JsonCreator on no-arg constructor and @JsonIgnoreProperties(ignoreUnknown = true),
        // unknown properties are ignored and defaults are used
        String jsonPayloadBad = '{"categoryMissing":"bar"}'

        when:
        Category category = mapper.readValue(jsonPayloadBad, Category)

        then:
        category.categoryName == ""
        0 * _
    }

    void 'test validation valid category'() {
        given:
        Category category = CategoryBuilder.builder().build()
        category.categoryName = "foobar"

        when:
        Set<ConstraintViolation<Category>> violations = validator.validate(category)

        then:
        violations.empty
    }

    @Unroll
    void 'test Category validation invalid #invalidField has error expectedError'() {
        given:
        Category category = new CategoryBuilder().builder()
                .withCategory(categoryName)
                .withActiveStatus(activeStatus)
                .build()

        when:
        Set<ConstraintViolation<Category>> violations = validator.validate(category)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().invalidValue == category.properties[invalidField]

        where:
        invalidField   | categoryName                                               | activeStatus | expectedError                               | errorCount
        'categoryName' | ''                                                         | true         | FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE | 1
        'categoryName' | 'ynotynotynotynotynotynotynotynotynotynotynotynotynotynot' | true         | FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE | 1
    }
}
