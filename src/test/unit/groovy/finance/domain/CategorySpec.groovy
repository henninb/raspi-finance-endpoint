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

    ValidatorFactory validatorFactory
    Validator validator
    private ObjectMapper mapper = new ObjectMapper()

    void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup() {
        validatorFactory.close()
    }

    def "test JSON serialization to Category object"() {

        given:
        def jsonPayload = '{"category":"bar"}'

        when:
        Category category = mapper.readValue(jsonPayload, Category.class)

        then:
        category.category == "bar"
        0 * _
    }

    def "test JSON serialization to Category object - bad payload"() {

        given:
        def jsonPayloadBad = 'badPayload'

        when:
        mapper.readValue(jsonPayloadBad, Category.class)

        then:
        JsonParseException ex = thrown()
        ex.getMessage().contains('Unrecognized token')
        0 * _
    }


    def "test JSON serialization to Category object - missing valid fields"() {

        given:
        def jsonPayloadBad = '{"categoryMissing":"bar"}'

        when:
        Category category = mapper.readValue(jsonPayloadBad, Category.class)

        then:
        category.category == ''
        0 * _

    }

    def "test validation valid category"() {
        given:
        Category category = CategoryBuilder.builder().build()
        category.category = "foobar"

        when:
        Set<ConstraintViolation<Category>> violations = validator.validate(category)

        then:
        violations.isEmpty()
    }
}
