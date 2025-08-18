package finance.controllers

import finance.domain.Parameter
import finance.services.ParameterService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject

import jakarta.validation.ValidationException
import java.util.Optional

class ParameterControllerSpec extends Specification {

    ParameterService parameterServiceMock = GroovyMock(ParameterService)

    @Subject
    ParameterController parameterController = new ParameterController(parameterServiceMock)

    def "parameters should return list of parameters when found"() {
        given:
        List<Parameter> expectedParameters = [
            new Parameter(parameterName: "param1", parameterValue: "value1", activeStatus: true),
            new Parameter(parameterName: "param2", parameterValue: "value2", activeStatus: true)
        ]

        when:
        ResponseEntity<List<Parameter>> response = parameterController.parameters()

        then:
        1 * parameterServiceMock.selectAll() >> expectedParameters
        response.statusCode == HttpStatus.OK
        response.body == expectedParameters
        response.body.size() == 2
    }

    def "parameters should throw NOT_FOUND when no parameters exist"() {
        when:
        parameterController.parameters()

        then:
        1 * parameterServiceMock.selectAll() >> []
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == "No parameters found"
    }

    def "parameters should throw INTERNAL_SERVER_ERROR when service fails"() {
        when:
        parameterController.parameters()

        then:
        1 * parameterServiceMock.selectAll() >> { throw new RuntimeException("Database error") }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        ex.reason.contains("Failed to retrieve parameters")
    }

    def "selectParameter should return parameter when found"() {
        given:
        String paramName = "test_param"
        Parameter expectedParameter = new Parameter(
            parameterName: paramName, 
            parameterValue: "test_value", 
            activeStatus: true
        )

        when:
        ResponseEntity<Parameter> response = parameterController.selectParameter(paramName)

        then:
        1 * parameterServiceMock.findByParameterName(paramName) >> Optional.of(expectedParameter)
        response.statusCode == HttpStatus.OK
        response.body == expectedParameter
        response.body.parameterName == paramName
    }

    def "selectParameter should throw NOT_FOUND when parameter doesn't exist"() {
        given:
        String paramName = "nonexistent_param"

        when:
        parameterController.selectParameter(paramName)

        then:
        1 * parameterServiceMock.findByParameterName(paramName) >> Optional.empty()
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == "Parameter not found: $paramName"
    }

    def "selectParameter should throw INTERNAL_SERVER_ERROR when service fails"() {
        given:
        String paramName = "test_param"

        when:
        parameterController.selectParameter(paramName)

        then:
        1 * parameterServiceMock.findByParameterName(paramName) >> { throw new RuntimeException("Service error") }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        ex.reason.contains("Failed to retrieve parameter")
    }

    def "insertParameter should create parameter successfully"() {
        given:
        Parameter inputParameter = new Parameter(
            parameterName: "new_param", 
            parameterValue: "new_value", 
            activeStatus: true
        )
        Parameter createdParameter = new Parameter(
            parameterName: "new_param", 
            parameterValue: "new_value", 
            activeStatus: true
        )

        when:
        ResponseEntity<Parameter> response = parameterController.insertParameter(inputParameter)

        then:
        1 * parameterServiceMock.insertParameter(inputParameter) >> createdParameter
        response.statusCode == HttpStatus.CREATED
        response.body == createdParameter
    }

    def "insertParameter should throw CONFLICT on duplicate parameter"() {
        given:
        Parameter parameter = new Parameter(
            parameterName: "duplicate_param", 
            parameterValue: "value", 
            activeStatus: true
        )

        when:
        parameterController.insertParameter(parameter)

        then:
        1 * parameterServiceMock.insertParameter(parameter) >> { 
            throw new DataIntegrityViolationException("Duplicate entry") 
        }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
        ex.reason == "Duplicate parameter found."
    }

    def "insertParameter should throw BAD_REQUEST on validation error"() {
        given:
        Parameter parameter = new Parameter(
            parameterName: "invalid_param", 
            parameterValue: "", 
            activeStatus: true
        )

        when:
        parameterController.insertParameter(parameter)

        then:
        1 * parameterServiceMock.insertParameter(parameter) >> { 
            throw new ValidationException("Validation failed") 
        }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
        ex.reason.contains("Validation error")
    }

    def "insertParameter should throw INTERNAL_SERVER_ERROR on unexpected error"() {
        given:
        Parameter parameter = new Parameter(
            parameterName: "test_param", 
            parameterValue: "value", 
            activeStatus: true
        )

        when:
        parameterController.insertParameter(parameter)

        then:
        1 * parameterServiceMock.insertParameter(parameter) >> { 
            throw new RuntimeException("Unexpected error") 
        }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        ex.reason.contains("Unexpected error")
    }

    def "updateParameter should update parameter successfully"() {
        given:
        String paramName = "existing_param"
        Parameter existingParameter = new Parameter(
            parameterName: paramName, 
            parameterValue: "old_value", 
            activeStatus: true
        )
        Parameter updateParameter = new Parameter(
            parameterName: paramName, 
            parameterValue: "new_value", 
            activeStatus: true
        )
        Parameter updatedParameter = new Parameter(
            parameterName: paramName, 
            parameterValue: "new_value", 
            activeStatus: true
        )

        when:
        ResponseEntity<Parameter> response = parameterController.updateParameter(paramName, updateParameter)

        then:
        1 * parameterServiceMock.findByParameterName(paramName) >> Optional.of(existingParameter)
        1 * parameterServiceMock.updateParameter(updateParameter) >> updatedParameter
        response.statusCode == HttpStatus.OK
        response.body == updatedParameter
    }

    def "updateParameter should throw NOT_FOUND when parameter doesn't exist"() {
        given:
        String paramName = "nonexistent_param"
        Parameter updateParameter = new Parameter(
            parameterName: paramName, 
            parameterValue: "value", 
            activeStatus: true
        )

        when:
        parameterController.updateParameter(paramName, updateParameter)

        then:
        1 * parameterServiceMock.findByParameterName(paramName) >> Optional.empty()
        0 * parameterServiceMock.updateParameter(_)
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == "Parameter not found: $paramName"
    }

    def "updateParameter should throw INTERNAL_SERVER_ERROR when service fails"() {
        given:
        String paramName = "test_param"
        Parameter existingParameter = new Parameter(
            parameterName: paramName, 
            parameterValue: "old_value", 
            activeStatus: true
        )
        Parameter updateParameter = new Parameter(
            parameterName: paramName, 
            parameterValue: "new_value", 
            activeStatus: true
        )

        when:
        parameterController.updateParameter(paramName, updateParameter)

        then:
        1 * parameterServiceMock.findByParameterName(paramName) >> Optional.of(existingParameter)
        1 * parameterServiceMock.updateParameter(updateParameter) >> { throw new RuntimeException("Update failed") }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        ex.reason.contains("Failed to update parameter")
    }

    def "deleteByParameterName should delete parameter successfully"() {
        given:
        String paramName = "param_to_delete"
        Parameter existingParameter = new Parameter(
            parameterName: paramName, 
            parameterValue: "value", 
            activeStatus: true
        )

        when:
        ResponseEntity<Parameter> response = parameterController.deleteByParameterName(paramName)

        then:
        1 * parameterServiceMock.findByParameterName(paramName) >> Optional.of(existingParameter)
        1 * parameterServiceMock.deleteByParameterName(paramName)
        response.statusCode == HttpStatus.OK
        response.body == existingParameter
    }

    def "deleteByParameterName should throw NOT_FOUND when parameter doesn't exist"() {
        given:
        String paramName = "nonexistent_param"

        when:
        parameterController.deleteByParameterName(paramName)

        then:
        1 * parameterServiceMock.findByParameterName(paramName) >> Optional.empty()
        0 * parameterServiceMock.deleteByParameterName(_)
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == "Parameter not found: $paramName"
    }

    def "deleteByParameterName should throw INTERNAL_SERVER_ERROR when service fails"() {
        given:
        String paramName = "test_param"
        Parameter existingParameter = new Parameter(
            parameterName: paramName, 
            parameterValue: "value", 
            activeStatus: true
        )

        when:
        parameterController.deleteByParameterName(paramName)

        then:
        1 * parameterServiceMock.findByParameterName(paramName) >> Optional.of(existingParameter)
        1 * parameterServiceMock.deleteByParameterName(paramName) >> { throw new RuntimeException("Delete failed") }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        ex.reason.contains("Failed to delete parameter")
    }

    def "should handle edge cases for parameter names"() {
        given:
        String paramName = parameterName
        Parameter expectedParameter = new Parameter(
            parameterName: paramName, 
            parameterValue: "value", 
            activeStatus: true
        )

        when:
        ResponseEntity<Parameter> response = parameterController.selectParameter(paramName)

        then:
        1 * parameterServiceMock.findByParameterName(paramName) >> Optional.of(expectedParameter)
        response.statusCode == HttpStatus.OK
        response.body.parameterName == paramName

        where:
        parameterName << ["simple", "with_underscore", "with-dash", "UPPERCASE", "123numeric"]
    }
}