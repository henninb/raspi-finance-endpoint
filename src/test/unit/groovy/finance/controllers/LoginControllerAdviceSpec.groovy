package finance.controllers

import jakarta.validation.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification

class LoginControllerAdviceSpec extends Specification {
    def advice = new LoginControllerAdvice()

    def "handleMessageNotReadableException returns 400 with error message and uses request"() {
        given:
        def cause = new RuntimeException("login parse issue")
        def ex = new HttpMessageNotReadableException("Invalid", cause, null)
        def req = new MockHttpServletRequest("POST", "/login")

        when:
        def resp = advice.handleMessageNotReadableException(req, ex)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
        resp.body.get("error").toLowerCase().contains("invalid request data")
        resp.body.get("error").contains("login parse issue")
    }

    def "handleValidationException returns 400 with message and uses request"() {
        given:
        def ex = new ValidationException("login invalid")
        def req = new MockHttpServletRequest("POST", "/login")

        when:
        def resp = advice.handleValidationException(req, ex)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
        resp.body.get("error") == "Validation error: login invalid"
    }
}

