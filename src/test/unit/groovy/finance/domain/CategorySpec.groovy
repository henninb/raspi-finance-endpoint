package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import finance.helpers.CategoryBuilder
import spock.lang.Specification

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

class CategorySpec extends Specification {

    protected ValidatorFactory validatorFactory
    protected Validator validator
    protected ObjectMapper mapper = new ObjectMapper()

    void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup() {
        validatorFactory.close()
    }

    void 'test -- JSON serialization to Category'() {

        given:
        String jsonPayload = '{"category":"bar"}'

        when:
        Category category = mapper.readValue(jsonPayload, Category)

        then:
        category.category == "bar"
        0 * _
    }

    void 'test JSON serialization to Category object - bad payload'() {

        given:
        String jsonPayloadBad = 'badPayload'

        when:
        mapper.readValue(jsonPayloadBad, Category)

        then:
        JsonParseException ex = thrown()
        ex.message.contains('Unrecognized token')
        0 * _
    }

    void 'test JSON serialization to Category object - missing valid fields'() {

        given:
        String jsonPayloadBad = '{"categoryMissing":"bar"}'

        when:
        Category category = mapper.readValue(jsonPayloadBad, Category)

        then:
        category.category == ''
        0 * _

    }

    void 'test validation valid category'() {
        given:
        Category category = CategoryBuilder.builder().build()
        category.category = "foobar"

        when:
        Set<ConstraintViolation<Category>> violations = validator.validate(category)

        then:
        violations.empty
    }
}
