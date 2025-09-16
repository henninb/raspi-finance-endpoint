package finance.services

import finance.domain.Parameter
import finance.helpers.ParameterBuilder

import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class ParameterServiceSpec extends BaseServiceSpec {
    io.micrometer.core.instrument.simple.SimpleMeterRegistry registry

    void setup() {
        parameterService.validator = validatorMock
        registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        parameterService.meterService = new MeterService(registry)
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

        // Create mock constraint violation
        ConstraintViolation<Parameter> violation = Mock(ConstraintViolation)
        violation.invalidValue >> ""
        violation.message >> "size must be between 3 and 40"

        Set<ConstraintViolation<Parameter>> constraintViolations = [violation] as Set

        when:
        parameterService.insertParameter(parameter)

        then:
        thrown(ValidationException)
        constraintViolations.size() == 1
        1 * validatorMock.validate(parameter) >> constraintViolations
        def c = registry.find('exception.thrown.counter').tag('exception.name.tag','ValidationException').counter()
        assert c != null && c.count() >= 1
    }

    void 'insertParameter - duplicate leads to ResponseStatusException(CONFLICT)'() {
        given:
        def parameter = ParameterBuilder.builder().build()
        Set<ConstraintViolation<Parameter>> constraintViolations = [] as Set

        when:
        parameterService.insertParameter(parameter)

        then:
        1 * validatorMock.validate(parameter) >> constraintViolations
        1 * parameterRepositoryMock.saveAndFlush(parameter) >> { throw new org.springframework.dao.DataIntegrityViolationException('duplicate') }
        thrown(org.springframework.web.server.ResponseStatusException)
    }

    void 'insertParameter - unexpected repo exception mapped to ResponseStatusException(500)'() {
        given:
        def parameter = ParameterBuilder.builder().build()
        Set<ConstraintViolation<Parameter>> constraintViolations = [] as Set

        when:
        parameterService.insertParameter(parameter)

        then:
        1 * validatorMock.validate(parameter) >> constraintViolations
        1 * parameterRepositoryMock.saveAndFlush(parameter) >> { throw new RuntimeException('boom') }
        thrown(org.springframework.web.server.ResponseStatusException)
    }

    void 'deleteByParameterName - not found throws ResponseStatusException(404)'() {
        given:
        def name = 'missing'

        when:
        parameterService.deleteByParameterName(name)

        then:
        1 * parameterRepositoryMock.findByParameterName(name) >> Optional.empty()
        thrown(org.springframework.web.server.ResponseStatusException)
    }
}
