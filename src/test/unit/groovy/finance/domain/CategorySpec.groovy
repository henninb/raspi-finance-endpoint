package finance.domain

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
        def jsonPayload = "{\"category\":\"test\"}"

        when:
        Category category = mapper.readValue(jsonPayload, Category.class)

        then:
        category.category == "test"
    }

    def "test validation valid category"() {
        given:
        Category category = CategoryBuilder.builder().build()
        category.category = "test1234"

        when:
        Set<ConstraintViolation<Category>> violations = validator.validate(category)

        then:
        violations.isEmpty()
    }
}
