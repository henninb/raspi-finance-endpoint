package finance.services

import finance.domain.Category
import finance.domain.Parameter
import finance.helpers.CategoryBuilder
import finance.helpers.ParameterBuilder
import spock.lang.Specification

import javax.validation.ConstraintViolation

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
}
