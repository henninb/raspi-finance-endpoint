package finance.services


import finance.domain.Description
import finance.helpers.DescriptionBuilder
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException

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

        // Create mock constraint violation
        ConstraintViolation<Description> violation = Mock(ConstraintViolation)
        violation.invalidValue >> ""
        violation.message >> "size must be between 3 and 40"

        Set<ConstraintViolation<Description>> constraintViolations = [violation] as Set

        when:
        descriptionService.insertDescription(description)

        then:
        constraintViolations.size() == 1
        thrown(ValidationException)
        1 * validatorMock.validate(description) >> constraintViolations
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        _ * _  // Allow any other interactions (logging, etc.)
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
