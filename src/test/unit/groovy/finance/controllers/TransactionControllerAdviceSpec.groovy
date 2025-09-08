package finance.controllers

import jakarta.validation.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import spock.lang.Specification

class TransactionControllerAdviceSpec extends Specification {
    def advice = new TransactionControllerAdvice()

    def "handleMessageNotReadableException returns 400 with error message"() {
        given:
        def cause = new RuntimeException("bad request body")
        def ex = new HttpMessageNotReadableException("Invalid", cause, null)

        when:
        def resp = advice.handleMessageNotReadableException(ex)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
        resp.body.get("error").toLowerCase().contains("invalid request data")
        resp.body.get("error").contains("bad request body")
    }

    def "handleValidationException returns 400 with message"() {
        given:
        def ex = new ValidationException("tx error")

        when:
        def resp = advice.handleValidationException(ex)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
        resp.body.get("error") == "Validation error: tx error"
    }
}

