package finance.services


import finance.domain.Parameter
import finance.helpers.ParameterBuilder

import javax.validation.ConstraintViolation
import javax.validation.ValidationException

class ParameterServiceSpec extends BaseServiceSpec {
    ParameterService parameterService = new ParameterService(parameterRepositoryMock, validatorMock, meterServiceMock)

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
        Parameter parameter = ParameterBuilder.builder().parameterName('').build()
        Set<ConstraintViolation<Parameter>> constraintViolations = validator.validate(parameter)

        when:
        parameterService.insertParameter(parameter)

        then:
        thrown(ValidationException)
        constraintViolations.size() == 1
        1 * validatorMock.validate(parameter) >> constraintViolations
        0 * _
    }
}
