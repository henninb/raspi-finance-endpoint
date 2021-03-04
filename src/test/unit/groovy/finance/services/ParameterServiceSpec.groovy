package finance.services

import finance.domain.Parameter
import finance.helpers.ParameterBuilder

import javax.validation.ConstraintViolation
import javax.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class ParameterServiceSpec extends BaseServiceSpec {

    void setup() {
    }

    void 'test - insert parameter'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()
        Set<ConstraintViolation<Parameter>> constraintViolations = validator.validate(parameter)

        when:
        parameterService.insertParameter(parameter)

        then:
        1 * validatorMock.validate(parameter) >> constraintViolations
        1 * parameterRepositoryMock.saveAndFlush(parameter)
        0 * _
    }

    void 'test - insert parameter - parm not valid'() {
        given:
        Parameter parameter = ParameterBuilder.builder().withParameterName('').build()
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
