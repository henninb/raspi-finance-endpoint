package finance.services

import finance.domain.Parameter
import finance.domain.ServiceResult
import finance.helpers.ParameterBuilder
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import jakarta.persistence.EntityNotFoundException

/**
 * TDD Specification for StandardizedParameterService
 * Tests the Parameter service using new ServiceResult pattern with comprehensive error handling
 */
class StandardizedParameterServiceSpec extends BaseServiceSpec {

    def standardizedParameterService = new StandardizedParameterService(parameterRepositoryMock)

    void setup() {
        standardizedParameterService.meterService = meterService
        standardizedParameterService.validator = validatorMock
    }

    // ===== TDD Tests for findAllActive() =====

    def "findAllActive should return Success with parameters when found"() {
        given: "existing active parameters"
        def parameters = [
            ParameterBuilder.builder().withParameterName("param1").build(),
            ParameterBuilder.builder().withParameterName("param2").build()
        ]

        when: "finding all active parameters"
        def result = standardizedParameterService.findAllActive()

        then: "should return Success with parameters"
        1 * parameterRepositoryMock.findByActiveStatusIsTrue() >> parameters
        result instanceof ServiceResult.Success
        result.data.size() == 2
        result.data[0].parameterName == "param1"
        result.data[1].parameterName == "param2"
        0 * _
    }

    def "findAllActive should return Success with empty list when no parameters found"() {
        when: "finding all active parameters with none existing"
        def result = standardizedParameterService.findAllActive()

        then: "should return Success with empty list"
        1 * parameterRepositoryMock.findByActiveStatusIsTrue() >> []
        result instanceof ServiceResult.Success
        result.data.isEmpty()
        0 * _
    }

    // ===== TDD Tests for findById() =====

    def "findById should return Success with parameter when found"() {
        given: "existing parameter"
        def parameter = ParameterBuilder.builder().withParameterId(1L).build()

        when: "finding by valid ID"
        def result = standardizedParameterService.findById(1L)

        then: "should return Success with parameter"
        1 * parameterRepositoryMock.findById(1L) >> Optional.of(parameter)
        result instanceof ServiceResult.Success
        result.data.parameterId == 1L
        0 * _
    }

    def "findById should return NotFound when parameter does not exist"() {
        when: "finding by non-existent ID"
        def result = standardizedParameterService.findById(999L)

        then: "should return NotFound result"
        1 * parameterRepositoryMock.findById(999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Parameter not found: 999")
        0 * _
    }

    // ===== TDD Tests for save() =====

    def "save should return Success with saved parameter when valid"() {
        given: "valid parameter"
        def parameter = ParameterBuilder.builder().build()
        def savedParameter = ParameterBuilder.builder().withParameterId(1L).build()
        Set<ConstraintViolation<Parameter>> noViolations = [] as Set

        when: "saving parameter"
        def result = standardizedParameterService.save(parameter)

        then: "should return Success with saved parameter"
        1 * validatorMock.validate(parameter) >> noViolations
        1 * parameterRepositoryMock.saveAndFlush(parameter) >> savedParameter
        result instanceof ServiceResult.Success
        result.data.parameterId == 1L
        0 * _
    }

    def "save should return ValidationError when parameter has constraint violations"() {
        given: "invalid parameter"
        def parameter = ParameterBuilder.builder().withParameterName("").build()
        ConstraintViolation<Parameter> violation = Mock(ConstraintViolation)
        def mockPath = Mock(javax.validation.Path)
        mockPath.toString() >> "parameterName"
        violation.propertyPath >> mockPath
        violation.message >> "size must be between 1 and 50"
        Set<ConstraintViolation<Parameter>> violations = [violation] as Set

        when: "saving invalid parameter"
        def result = standardizedParameterService.save(parameter)

        then: "should return ValidationError result"
        1 * validatorMock.validate(parameter) >> { throw new ConstraintViolationException("Validation failed", violations) }
        result instanceof ServiceResult.ValidationError
        result.errors.size() == 1
        result.errors.values().contains("size must be between 1 and 50")
    }

    def "save should return BusinessError when duplicate parameter exists"() {
        given: "parameter that will cause duplicate key violation"
        def parameter = ParameterBuilder.builder().withParameterName("duplicate").build()
        Set<ConstraintViolation<Parameter>> noViolations = [] as Set

        when: "saving duplicate parameter"
        def result = standardizedParameterService.save(parameter)

        then: "should return BusinessError result"
        1 * validatorMock.validate(parameter) >> noViolations
        1 * parameterRepositoryMock.saveAndFlush(parameter) >> {
            throw new DataIntegrityViolationException("Duplicate entry")
        }
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("data integrity")
        result.errorCode == "DATA_INTEGRITY_VIOLATION"
        0 * _
    }

    // ===== TDD Tests for update() =====

    def "update should return Success with updated parameter when exists"() {
        given: "existing parameter to update"
        def existingParameter = ParameterBuilder.builder().withParameterId(1L).withParameterName("old").build()
        def updatedParameter = ParameterBuilder.builder().withParameterId(1L).withParameterName("new").build()

        when: "updating existing parameter"
        def result = standardizedParameterService.update(updatedParameter)

        then: "should return Success with updated parameter"
        1 * parameterRepositoryMock.findById(1L) >> Optional.of(existingParameter)
        1 * parameterRepositoryMock.saveAndFlush(_ as Parameter) >> { Parameter param ->
            assert param.parameterName == "new"
            return param
        }
        result instanceof ServiceResult.Success
        result.data.parameterName == "new"
        0 * _
    }

    def "update should return NotFound when parameter does not exist"() {
        given: "parameter with non-existent ID"
        def parameter = ParameterBuilder.builder().withParameterId(999L).build()

        when: "updating non-existent parameter"
        def result = standardizedParameterService.update(parameter)

        then: "should return NotFound result"
        1 * parameterRepositoryMock.findById(999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Parameter not found: 999")
        0 * _
    }

    // ===== TDD Tests for deleteById() =====

    def "deleteById should return Success when parameter exists"() {
        given: "existing parameter"
        def parameter = ParameterBuilder.builder().withParameterId(1L).build()

        when: "deleting existing parameter"
        def result = standardizedParameterService.deleteById(1L)

        then: "should return Success"
        1 * parameterRepositoryMock.findById(1L) >> Optional.of(parameter)
        1 * parameterRepositoryMock.delete(parameter)
        result instanceof ServiceResult.Success
        result.data == true
        0 * _
    }

    def "deleteById should return NotFound when parameter does not exist"() {
        when: "deleting non-existent parameter"
        def result = standardizedParameterService.deleteById(999L)

        then: "should return NotFound result"
        1 * parameterRepositoryMock.findById(999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Parameter not found: 999")
        0 * _
    }

    // ===== TDD Tests for Legacy Method Support =====

    def "selectAll should delegate to findAllActive and return data"() {
        given: "existing parameters"
        def parameters = [ParameterBuilder.builder().build()]

        when: "calling legacy selectAll method"
        def result = standardizedParameterService.selectAll()

        then: "should return parameter list"
        1 * parameterRepositoryMock.findByActiveStatusIsTrue() >> parameters
        result.size() == 1
        0 * _
    }

    def "insertParameter should delegate to save and return data"() {
        given: "valid parameter"
        def parameter = ParameterBuilder.builder().build()
        def savedParameter = ParameterBuilder.builder().withParameterId(1L).build()
        Set<ConstraintViolation<Parameter>> noViolations = [] as Set

        when: "calling legacy insertParameter method"
        def result = standardizedParameterService.insertParameter(parameter)

        then: "should return saved parameter"
        1 * validatorMock.validate(parameter) >> noViolations
        1 * parameterRepositoryMock.saveAndFlush(parameter) >> savedParameter
        result.parameterId == 1L
        0 * _
    }

    def "updateParameter should delegate to update and return data"() {
        given: "existing parameter to update"
        def existingParameter = ParameterBuilder.builder().withParameterId(1L).withParameterName("old").build()
        def updatedParameter = ParameterBuilder.builder().withParameterId(1L).withParameterName("new").build()

        when: "calling legacy updateParameter method"
        def result = standardizedParameterService.updateParameter(updatedParameter)

        then: "should return updated parameter"
        1 * parameterRepositoryMock.findById(1L) >> Optional.of(existingParameter)
        1 * parameterRepositoryMock.saveAndFlush(_ as Parameter) >> { Parameter param -> return param }
        result.parameterName == "new"
        0 * _
    }

    def "findByParameterName should return parameter when found"() {
        given: "existing parameter"
        def parameter = ParameterBuilder.builder().withParameterName("test").build()

        when: "finding by parameter name"
        def result = standardizedParameterService.findByParameterName("test")

        then: "should return parameter optional"
        1 * parameterRepositoryMock.findByParameterName("test") >> Optional.of(parameter)
        result.isPresent()
        result.get().parameterName == "test"
        0 * _
    }

    def "deleteByParameterName should return true when parameter exists"() {
        given: "existing parameter"
        def parameter = ParameterBuilder.builder().withParameterName("test").build()

        when: "deleting by parameter name"
        def result = standardizedParameterService.deleteByParameterName("test")

        then: "should return true"
        1 * parameterRepositoryMock.findByParameterName("test") >> Optional.of(parameter)
        1 * parameterRepositoryMock.delete(parameter)
        result == true
        0 * _
    }

    // ===== TDD Tests for Error Handling in Legacy Methods =====

    def "insertParameter should throw ValidationException for invalid parameter"() {
        given: "invalid parameter"
        def parameter = ParameterBuilder.builder().withParameterName("").build()
        ConstraintViolation<Parameter> violation = Mock(ConstraintViolation)
        violation.invalidValue >> ""
        violation.message >> "size must be between 1 and 50"
        Set<ConstraintViolation<Parameter>> violations = [violation] as Set

        when: "calling legacy insertParameter with invalid data"
        standardizedParameterService.insertParameter(parameter)

        then: "should throw ValidationException"
        1 * validatorMock.validate(parameter) >> { throw new ConstraintViolationException("Validation failed", violations) }
        thrown(jakarta.validation.ValidationException)
    }

    def "deleteByParameterName should throw ResponseStatusException when not found"() {
        when: "deleting by non-existent parameter name"
        standardizedParameterService.deleteByParameterName("missing")

        then: "should throw ResponseStatusException"
        1 * parameterRepositoryMock.findByParameterName("missing") >> Optional.empty()
        thrown(org.springframework.web.server.ResponseStatusException)
        0 * _
    }

    def "updateParameter should throw RuntimeException when parameter not found"() {
        given: "parameter with non-existent ID"
        def parameter = ParameterBuilder.builder().withParameterId(999L).build()

        when: "calling legacy updateParameter with non-existent parameter"
        standardizedParameterService.updateParameter(parameter)

        then: "should throw RuntimeException"
        1 * parameterRepositoryMock.findById(999L) >> Optional.empty()
        thrown(RuntimeException)
        0 * _
    }
}