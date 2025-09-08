package finance.controllers

import jakarta.validation.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import spock.lang.Specification

class CategoryControllerAdviceSpec extends Specification {
    def advice = new CategoryControllerAdvice()

    def "handleMessageNotReadableException returns 400 with error message"() {
        given:
        def cause = new RuntimeException("category json invalid")
        def ex = new HttpMessageNotReadableException("Invalid", cause, null)

        when:
        def resp = advice.handleMessageNotReadableException(ex)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
        resp.body.get("error").toLowerCase().contains("invalid request data")
        resp.body.get("error").contains("category json invalid")
    }

    def "handleValidationException returns 400 with message"() {
        given:
        def ex = new ValidationException("category invalid")

        when:
        def resp = advice.handleValidationException(ex)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
        resp.body.get("error") == "Validation error: category invalid"
    }
}

