package finance.services

import finance.domain.Description
import finance.helpers.DescriptionBuilder

import javax.validation.ConstraintViolation
import javax.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class DescriptionServiceSpec extends BaseServiceSpec {
    void setup() {
    }

    void 'test - insert description'() {
        given:
        Description description = DescriptionBuilder.builder().build()
        Set<ConstraintViolation<Description>> constraintViolations = validator.validate(description)

        when:
        descriptionService.insertDescription(description)

        then:
        1 * validatorMock.validate(description) >> constraintViolations
        1 * descriptionRepositoryMock.saveAndFlush(description)
        0 * _
    }

    void 'test - insert description - empty descriptionName'() {
        given:
        Description description = DescriptionBuilder.builder().withDescription('').build()
        Set<ConstraintViolation<Description>> constraintViolations = validator.validate(description)

        when:
        descriptionService.insertDescription(description)

        then:
        constraintViolations.size() == 1
        thrown(ValidationException)
        1 * validatorMock.validate(description) >> constraintViolations
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }
}
