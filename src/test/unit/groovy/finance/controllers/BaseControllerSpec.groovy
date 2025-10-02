package finance.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import jakarta.validation.ValidationException
import org.apache.catalina.connector.ClientAbortException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class BaseControllerSpec extends Specification {

    BaseController controller

    def setup() {
        controller = new BaseController()
    }

    def "should handle bad HTTP requests with proper response"() {
        given:
        def exception = new ConstraintViolationException("Invalid data", null)

        when:
        def response = controller.handleBadHttpRequests(exception)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body["response"] == "BAD_REQUEST: ConstraintViolationException"
    }

    def "should handle NumberFormatException"() {
        given:
        def exception = new NumberFormatException("Invalid number")

        when:
        def response = controller.handleBadHttpRequests(exception)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body["response"] == "BAD_REQUEST: NumberFormatException"
    }

    def "should handle EmptyResultDataAccessException"() {
        given:
        def exception = new EmptyResultDataAccessException(1)

        when:
        def response = controller.handleBadHttpRequests(exception)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body["response"] == "BAD_REQUEST: EmptyResultDataAccessException"
    }

    def "should handle MethodArgumentTypeMismatchException"() {
        given:
        def exception = new MethodArgumentTypeMismatchException("test", String.class, "param", null, null)

        when:
        def response = controller.handleBadHttpRequests(exception)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body["response"] == "BAD_REQUEST: MethodArgumentTypeMismatchException"
    }

    def "should handle HttpMessageNotReadableException"() {
        given:
        def exception = new HttpMessageNotReadableException("Bad message", null as org.springframework.http.HttpInputMessage)

        when:
        def response = controller.handleBadHttpRequests(exception)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body["response"] == "BAD_REQUEST: HttpMessageNotReadableException"
    }

    def "should handle HttpMediaTypeNotSupportedException"() {
        given:
        def exception = new HttpMediaTypeNotSupportedException("Unsupported media type")

        when:
        def response = controller.handleBadHttpRequests(exception)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body["response"] == "BAD_REQUEST: HttpMediaTypeNotSupportedException"
    }

    def "should handle IllegalArgumentException"() {
        given:
        def exception = new IllegalArgumentException("Invalid argument")

        when:
        def response = controller.handleBadHttpRequests(exception)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body["response"] == "BAD_REQUEST: IllegalArgumentException"
    }

    def "should handle ValidationException"() {
        given:
        def exception = new ValidationException("Validation failed")

        when:
        def response = controller.handleValidationException(exception)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body["response"] == "BAD_REQUEST: ValidationException"
    }

    def "should handle ResponseStatusException with custom reason"() {
        given:
        def exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found")

        when:
        def response = controller.handleResponseStatusException(exception)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body["response"] == "Resource not found"
    }

    def "should handle ResponseStatusException without reason"() {
        given:
        def exception = new ResponseStatusException(HttpStatus.CONFLICT)

        when:
        def response = controller.handleResponseStatusException(exception)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body["response"].contains("409")
        response.body["response"].contains("ResponseStatusException")
    }

    def "should handle AuthenticationException"() {
        given:
        // Create anonymous subclass since AuthenticationException is abstract
        def exception = new AuthenticationException("Authentication failed") {}

        when:
        def response = controller.handleUnauthorized(exception)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
        // Response will be "UNAUTHORIZED: " plus the simple class name (which may be empty or "$1" for anonymous class)
        response.body["response"] != null
        response.body["response"].toString().startsWith("UNAUTHORIZED:")
    }

    def "should handle ClientAbortException"() {
        given:
        def exception = new ClientAbortException("Client disconnected")

        when:
        def response = controller.handleServiceUnavailable(exception)

        then:
        response.statusCode == HttpStatus.SERVICE_UNAVAILABLE
        response.body["response"] == "SERVICE_UNAVAILABLE: ClientAbortException"
    }

    def "should handle generic Exception"() {
        given:
        def exception = new RuntimeException("Unexpected error")

        when:
        def response = controller.handleHttpInternalError(exception)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body["response"] == "INTERNAL_SERVER_ERROR: RuntimeException"
    }

    def "should have ObjectMapper in companion object"() {
        when:
        def mapper = BaseController.mapper

        then:
        mapper != null
        mapper instanceof com.fasterxml.jackson.databind.ObjectMapper
    }

    def "should have logger in companion object"() {
        when:
        def logger = BaseController.logger

        then:
        logger != null
        logger instanceof org.apache.logging.log4j.Logger
    }

    def cleanup() {
        // Clear any request context to avoid test pollution
        try {
            RequestContextHolder.resetRequestAttributes()
        } catch (Exception ignored) {
            // Ignore cleanup errors
        }
    }
}