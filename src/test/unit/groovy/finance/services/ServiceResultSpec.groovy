package finance.services

import finance.domain.ServiceResult
import spock.lang.Specification
import spock.lang.Unroll

/**
 * TDD Specification for ServiceResult pattern
 * Tests the standardized service response wrapper for consistent error handling
 */
class ServiceResultSpec extends Specification {

    // ===== TDD Tests for ServiceResult Success =====

    def "Success should contain data and be successful"() {
        given: "a successful result with data"
        String testData = "test data"

        when: "creating a Success result"
        def result = ServiceResult.Success.of(testData)

        then: "result should contain data and be successful"
        result.data == testData
        result.isSuccess()
        !result.isError()
    }

    def "Success should support null data"() {
        given: "null data"
        String nullData = null

        when: "creating a Success result with null"
        def result = ServiceResult.Success.of(nullData)

        then: "result should handle null gracefully"
        result.data == null
        result.isSuccess()
        !result.isError()
    }

    // ===== TDD Tests for ServiceResult NotFound =====

    def "NotFound should contain message and be error"() {
        given: "a not found message"
        String message = "Entity not found"

        when: "creating a NotFound result"
        def result = ServiceResult.NotFound.of(message)

        then: "result should contain message and be error"
        result.message == message
        !result.isSuccess()
        result.isError()
    }

    // ===== TDD Tests for ServiceResult ValidationError =====

    def "ValidationError should contain errors map and be error"() {
        given: "validation errors map"
        Map<String, String> errors = [
            "name": "Name is required",
            "email": "Email format is invalid"
        ]

        when: "creating a ValidationError result"
        def result = ServiceResult.ValidationError.of(errors)

        then: "result should contain errors and be error"
        result.errors == errors
        result.errors.size() == 2
        !result.isSuccess()
        result.isError()
    }

    def "ValidationError should handle empty errors map"() {
        given: "empty errors map"
        Map<String, String> emptyErrors = [:]

        when: "creating a ValidationError result with empty map"
        def result = ServiceResult.ValidationError.of(emptyErrors)

        then: "result should handle empty map gracefully"
        result.errors.isEmpty()
        !result.isSuccess()
        result.isError()
    }

    // ===== TDD Tests for ServiceResult BusinessError =====

    def "BusinessError should contain message and error code"() {
        given: "business error details"
        String message = "Duplicate entity found"
        String errorCode = "DUPLICATE_ENTITY"

        when: "creating a BusinessError result"
        def result = ServiceResult.BusinessError.of(message, errorCode)

        then: "result should contain message and error code"
        result.message == message
        result.errorCode == errorCode
        !result.isSuccess()
        result.isError()
    }

    // ===== TDD Tests for ServiceResult SystemError =====

    def "SystemError should contain exception and be error"() {
        given: "a system exception"
        Exception exception = new RuntimeException("Database connection failed")

        when: "creating a SystemError result"
        def result = ServiceResult.SystemError.of(exception)

        then: "result should contain exception and be error"
        result.exception == exception
        result.exception.message == "Database connection failed"
        !result.isSuccess()
        result.isError()
    }

    // ===== TDD Tests for Extension Functions =====

    def "onSuccess should execute action for Success result"() {
        given: "a successful result"
        def result = ServiceResult.Success.of("test data")
        boolean actionExecuted = false

        when: "calling onSuccess"
        result.onSuccess { data ->
            actionExecuted = true
        }

        then: "action should be executed"
        actionExecuted
    }

    def "onSuccess should not execute action for error results"() {
        given: "an error result"
        def result = ServiceResult.NotFound.of("Not found")
        boolean actionExecuted = false

        when: "calling onSuccess on error"
        result.onSuccess { data ->
            actionExecuted = true
        }

        then: "action should not be executed"
        !actionExecuted
    }

    def "onError should execute action for error results"() {
        given: "an error result"
        def result = ServiceResult.NotFound.of("Entity not found")
        String capturedMessage = null

        when: "calling onError"
        result.onError { message ->
            capturedMessage = message
        }

        then: "action should be executed with error message"
        capturedMessage == "Entity not found"
    }

    def "onError should not execute action for Success result"() {
        given: "a successful result"
        def result = ServiceResult.Success.of("test data")
        boolean actionExecuted = false

        when: "calling onError on success"
        result.onError { message ->
            actionExecuted = true
        }

        then: "action should not be executed"
        !actionExecuted
    }

    // ===== TDD Tests for Result Transformation =====

    @Unroll
    def "should correctly identify result types for #resultType"() {
        when: "checking result type"
        def isSuccess = result.isSuccess()
        def isError = result.isError()

        then: "type should be correctly identified"
        isSuccess == expectedSuccess
        isError == expectedError

        where:
        resultType        | result                                           | expectedSuccess | expectedError
        "Success"         | ServiceResult.Success.of("data")                | true            | false
        "NotFound"        | ServiceResult.NotFound.of("Not found")          | false           | true
        "ValidationError" | ServiceResult.ValidationError.of([:])           | false           | true
        "BusinessError"   | ServiceResult.BusinessError.of("Error", "CODE") | false           | true
        "SystemError"     | ServiceResult.SystemError.of(new Exception())   | false           | true
    }

    // ===== TDD Tests for Chaining Operations =====

    def "should support method chaining with onSuccess and onError"() {
        given: "a successful result"
        def result = ServiceResult.Success.of("test data")
        boolean successExecuted = false
        boolean errorExecuted = false

        when: "chaining onSuccess and onError"
        result
            .onSuccess { data -> successExecuted = true }
            .onError { message -> errorExecuted = true }

        then: "only success action should execute"
        successExecuted
        !errorExecuted
    }

    def "should support error chaining"() {
        given: "an error result"
        def result = ServiceResult.NotFound.of("Not found")
        boolean successExecuted = false
        boolean errorExecuted = false

        when: "chaining onSuccess and onError"
        result
            .onSuccess { data -> successExecuted = true }
            .onError { message -> errorExecuted = true }

        then: "only error action should execute"
        !successExecuted
        errorExecuted
    }

    // ===== TDD Tests for Data Extraction =====

    def "should provide safe data extraction methods"() {
        given: "various result types"
        def successResult = ServiceResult.Success.of("test data")
        def errorResult = ServiceResult.NotFound.of("Not found")

        when: "extracting data safely"
        def successData = successResult.getDataOrNull()
        def errorData = errorResult.getDataOrNull()
        def defaultData = errorResult.getDataOrDefault("default value")

        then: "data should be extracted safely"
        successData == "test data"
        errorData == null
        defaultData == "default value"
    }
}