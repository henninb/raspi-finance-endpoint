package finance.domain

import spock.lang.Specification

class LoginResponseSpec extends Specification {

    def "test LoginResponse creation with token only"() {
        given:
        String token = "jwt-token-123"

        when:
        LoginResponse response = new LoginResponse(token, null)

        then:
        response.token == token
        response.error == null
    }

    def "test LoginResponse creation with error only"() {
        given:
        String error = "Invalid credentials"

        when:
        LoginResponse response = new LoginResponse(null, error)

        then:
        response.token == null
        response.error == error
    }

    def "test LoginResponse creation with both token and error"() {
        given:
        String token = "jwt-token-123"
        String error = "Warning message"

        when:
        LoginResponse response = new LoginResponse(token, error)

        then:
        response.token == token
        response.error == error
    }

    def "test LoginResponse creation with default values"() {
        when:
        LoginResponse response = new LoginResponse()

        then:
        response.token == null
        response.error == null
    }

    def "test LoginResponse equals and hashCode with same values"() {
        given:
        LoginResponse response1 = new LoginResponse("token", "error")
        LoginResponse response2 = new LoginResponse("token", "error")

        expect:
        response1 == response2
        response1.hashCode() == response2.hashCode()
    }

    def "test LoginResponse equals returns false with different values"() {
        given:
        LoginResponse response1 = new LoginResponse("token1", "error1")
        LoginResponse response2 = new LoginResponse("token2", "error2")

        expect:
        response1 != response2
    }

    def "test LoginResponse with empty strings"() {
        when:
        LoginResponse response = new LoginResponse("", "")

        then:
        response.token == ""
        response.error == ""
    }

    def "test LoginResponse toString method"() {
        given:
        LoginResponse response = new LoginResponse("test-token", "test-error")

        when:
        String result = response.toString()

        then:
        result != null
        result.contains("test-token")
        result.contains("test-error")
    }

    def "test LoginResponse with only token parameter"() {
        when:
        LoginResponse response = new LoginResponse("only-token", null)

        then:
        response.token == "only-token"
        response.error == null
    }

    def "test LoginResponse successful authentication scenario"() {
        given:
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"

        when:
        LoginResponse response = new LoginResponse(validToken, null)

        then:
        response.token == validToken
        response.error == null
    }

    def "test LoginResponse failed authentication scenario"() {
        given:
        String errorMessage = "Authentication failed: Invalid username or password"

        when:
        LoginResponse response = new LoginResponse(null, errorMessage)

        then:
        response.token == null
        response.error == errorMessage
    }
}