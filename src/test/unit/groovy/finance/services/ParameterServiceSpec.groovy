package finance.services

import finance.domain.Parameter
import finance.helpers.ParameterBuilder

import javax.validation.ConstraintViolation
import javax.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class ParameterServiceSpec extends BaseServiceSpec {

    void setup() {
        parameterService.validator = validatorMock
        parameterService.meterService = meterService
    }

    void 'test - insert parameter'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()
        Set<ConstraintViolation<Parameter>> constraintViolations = validator.validate(parameter)

        when:
        Parameter parameterInserted = parameterService.insertParameter(parameter)

        then:
        parameterInserted.parameterName == parameter.parameterName
        1 * validatorMock.validate(parameter) >> constraintViolations
        1 * parameterRepositoryMock.saveAndFlush(parameter) >> parameter
        0 * _
    }

    void 'test - insert parameter - parameter is not valid'() {
        given:
        Parameter parameter = ParameterBuilder.builder().withParameterName('').withActiveStatus(true).build()
        Set<ConstraintViolation<Parameter>> constraintViolations = validator.validate(parameter)

        when:
        parameterService.insertParameter(parameter)

        then:
        thrown(ValidationException)
        constraintViolations.size() == 1
        1 * validatorMock.validate(parameter) >> constraintViolations
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }
}
