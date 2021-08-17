package finance.services


import finance.domain.Description
import finance.helpers.DescriptionBuilder

import javax.validation.ConstraintViolation
import javax.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class DescriptionServiceSpec extends BaseServiceSpec {

    void setup() {
        descriptionService.validator = validatorMock
        descriptionService.meterService = meterService
    }

    void 'test - insert description'() {
        given:
        Description description = DescriptionBuilder.builder().build()
        Set<ConstraintViolation<Description>> constraintViolations = validator.validate(description)

        when:
        Description descriptionInserted = descriptionService.insertDescription(description)

        then:
        descriptionInserted.descriptionName == description.descriptionName
        1 * validatorMock.validate(description) >> constraintViolations
        1 * descriptionRepositoryMock.saveAndFlush(description) >> description
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

    void 'test - delete description'() {
        given:
        Description description = DescriptionBuilder.builder().build()

        when:
        descriptionService.deleteByDescriptionName(description.descriptionName)

        then:
        1 * descriptionRepositoryMock.findByDescriptionName(description.descriptionName) >> Optional.of(description)
        1 * descriptionRepositoryMock.delete(description)
        0 * _
    }
}
