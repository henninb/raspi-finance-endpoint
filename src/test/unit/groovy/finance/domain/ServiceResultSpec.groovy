package finance.domain

import spock.lang.Specification

class ServiceResultSpec extends Specification {

    def "Success should create success result with data"() {
        given:
        def data = "test data"

        when:
        def result = ServiceResult.Success.of(data)

        then:
        result.data == data
        result.isSuccess()
        !result.isError()
    }

    def "Success should handle null data"() {
        when:
        def result = ServiceResult.Success.of(null)

        then:
        result.data == null
        result.isSuccess()
        !result.isError()
    }

    def "NotFound should create not found result with message"() {
        given:
        def message = "Resource not found"

        when:
        def result = ServiceResult.NotFound.of(message)

        then:
        result.message == message
        !result.isSuccess()
        result.isError()
    }

    def "ValidationError should create validation error with field errors"() {
        given:
        def errors = ["field1": "error1", "field2": "error2"]

        when:
        def result = ServiceResult.ValidationError.of(errors)

        then:
        result.errors == errors
        !result.isSuccess()
        result.isError()
    }

    def "BusinessError should create business error with message and code"() {
        given:
        def message = "Business rule violation"
        def errorCode = "BR001"

        when:
        def result = ServiceResult.BusinessError.of(message, errorCode)

        then:
        result.message == message
        result.errorCode == errorCode
        !result.isSuccess()
        result.isError()
    }

    def "SystemError should create system error with exception"() {
        given:
        def exception = new RuntimeException("System failure")

        when:
        def result = ServiceResult.SystemError.of(exception)

        then:
        result.exception == exception
        !result.isSuccess()
        result.isError()
    }

    def "onSuccess should execute action for Success result"() {
        given:
        def data = "test data"
        def result = ServiceResult.Success.of(data)
        def actionExecuted = false
        def receivedData = null

        when:
        def chainResult = result.onSuccess { receivedData = it; actionExecuted = true }

        then:
        actionExecuted
        receivedData == data
        chainResult.is(result) // Should return same instance for chaining
    }

    def "onSuccess should not execute action for error results"() {
        given:
        def result = ServiceResult.NotFound.of("Not found")
        def actionExecuted = false

        when:
        def chainResult = result.onSuccess { actionExecuted = true }

        then:
        !actionExecuted
        chainResult.is(result)
    }

    def "onError should execute action for NotFound result"() {
        given:
        def message = "Resource not found"
        def result = ServiceResult.NotFound.of(message)
        def actionExecuted = false
        def receivedMessage = null

        when:
        def chainResult = result.onError { receivedMessage = it; actionExecuted = true }

        then:
        actionExecuted
        receivedMessage == message
        chainResult.is(result)
    }

    def "onError should execute action for ValidationError result"() {
        given:
        def errors = ["field1": "error1"]
        def result = ServiceResult.ValidationError.of(errors)
        def actionExecuted = false
        def receivedMessage = null

        when:
        def chainResult = result.onError { receivedMessage = it; actionExecuted = true }

        then:
        actionExecuted
        receivedMessage.contains("field1")
        receivedMessage.contains("error1")
        chainResult.is(result)
    }

    def "onError should execute action for BusinessError result"() {
        given:
        def message = "Business error"
        def result = ServiceResult.BusinessError.of(message, "BR001")
        def actionExecuted = false
        def receivedMessage = null

        when:
        def chainResult = result.onError { receivedMessage = it; actionExecuted = true }

        then:
        actionExecuted
        receivedMessage == message
        chainResult.is(result)
    }

    def "onError should execute action for SystemError result with message"() {
        given:
        def exception = new RuntimeException("System error")
        def result = ServiceResult.SystemError.of(exception)
        def actionExecuted = false
        def receivedMessage = null

        when:
        def chainResult = result.onError { receivedMessage = it; actionExecuted = true }

        then:
        actionExecuted
        receivedMessage == "System error"
        chainResult.is(result)
    }

    def "onError should execute action for SystemError result without message"() {
        given:
        def exception = new RuntimeException()
        def result = ServiceResult.SystemError.of(exception)
        def actionExecuted = false
        def receivedMessage = null

        when:
        def chainResult = result.onError { receivedMessage = it; actionExecuted = true }

        then:
        actionExecuted
        receivedMessage == "System error occurred"
        chainResult.is(result)
    }

    def "onError should not execute action for Success result"() {
        given:
        def result = ServiceResult.Success.of("data")
        def actionExecuted = false

        when:
        def chainResult = result.onError { actionExecuted = true }

        then:
        !actionExecuted
        chainResult.is(result)
    }

    def "getDataOrNull should return data for Success result"() {
        given:
        def data = "test data"
        def result = ServiceResult.Success.of(data)

        when:
        def retrievedData = result.getDataOrNull()

        then:
        retrievedData == data
    }

    def "getDataOrNull should return null for error results"() {
        given:
        def results = [
            ServiceResult.NotFound.of("Not found"),
            ServiceResult.ValidationError.of(["field": "error"]),
            ServiceResult.BusinessError.of("Business error", "BR001"),
            ServiceResult.SystemError.of(new RuntimeException())
        ]

        when:
        def retrievedData = results.collect { it.getDataOrNull() }

        then:
        retrievedData.every { it == null }
    }

    def "getDataOrDefault should return data for Success result"() {
        given:
        def data = "test data"
        def defaultValue = "default"
        def result = ServiceResult.Success.of(data)

        when:
        def retrievedData = result.getDataOrDefault(defaultValue)

        then:
        retrievedData == data
    }

    def "getDataOrDefault should return default value for error results"() {
        given:
        def defaultValue = "default value"
        def results = [
            ServiceResult.NotFound.of("Not found"),
            ServiceResult.ValidationError.of(["field": "error"]),
            ServiceResult.BusinessError.of("Business error", "BR001"),
            ServiceResult.SystemError.of(new RuntimeException())
        ]

        when:
        def retrievedData = results.collect { it.getDataOrDefault(defaultValue) }

        then:
        retrievedData.every { it == defaultValue }
    }

    def "Success should handle different data types"() {
        given:
        def stringResult = ServiceResult.Success.of("string")
        def numberResult = ServiceResult.Success.of(42)
        def listResult = ServiceResult.Success.of([1, 2, 3])
        def mapResult = ServiceResult.Success.of(["key": "value"])

        expect:
        stringResult.data == "string"
        numberResult.data == 42
        listResult.data == [1, 2, 3]
        mapResult.data == ["key": "value"]
    }

    def "method chaining should work correctly"() {
        given:
        def result = ServiceResult.Success.of("test")
        def actionExecuted = false
        def errorExecuted = false

        when:
        def chainResult = result
            .onSuccess { actionExecuted = true }
            .onError { errorExecuted = true }

        then:
        actionExecuted
        !errorExecuted
        chainResult.is(result)
    }

    def "error method chaining should work correctly"() {
        given:
        def result = ServiceResult.NotFound.of("Not found")
        def actionExecuted = false
        def errorExecuted = false

        when:
        def chainResult = result
            .onSuccess { actionExecuted = true }
            .onError { errorExecuted = true }

        then:
        !actionExecuted
        errorExecuted
        chainResult.is(result)
    }
}