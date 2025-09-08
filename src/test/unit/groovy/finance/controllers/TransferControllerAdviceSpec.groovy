package finance.controllers

import jakarta.validation.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import spock.lang.Specification

class TransferControllerAdviceSpec extends Specification {
    def advice = new TransferControllerAdvice()

    def "handleMessageNotReadableException returns 400 with error message"() {
        given:
        def cause = new RuntimeException("transfer bad body")
        def ex = new HttpMessageNotReadableException("Invalid", cause, null)

        when:
        def resp = advice.handleMessageNotReadableException(ex)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
        resp.body.get("error").toLowerCase().contains("invalid request data")
        resp.body.get("error").contains("transfer bad body")
    }

    def "handleValidationException returns 400 with message"() {
        given:
        def ex = new ValidationException("transfer invalid")

        when:
        def resp = advice.handleValidationException(ex)

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
        resp.body.get("error") == "Validation error: transfer invalid"
    }
}

