package finance.services

import finance.domain.Description
import finance.helpers.DescriptionBuilder

import javax.validation.ConstraintViolation
import javax.validation.ValidationException

class DescriptionServiceSpec extends BaseServiceSpec {
    protected DescriptionService descriptionService = new DescriptionService(descriptionRepositoryMock, validatorMock, meterService)

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
        //1 * meterService.incrementExceptionThrownCounter('ValidationException')
        1 * meterRegistryMock.counter(_) >> counter
        1 * counter.increment()
        0 * _
    }
}
