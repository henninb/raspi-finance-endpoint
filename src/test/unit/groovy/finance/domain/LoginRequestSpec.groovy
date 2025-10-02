package finance.domain

import jakarta.validation.Validation
import jakarta.validation.Validator
import spock.lang.Specification

class LoginRequestSpec extends Specification {

    Validator validator

    def setup() {
        def factory = Validation.buildDefaultValidatorFactory()
        validator = factory.getValidator()
    }

    def "should create LoginRequest with valid username and password"() {
        when: "creating a LoginRequest with valid data"
        def loginRequest = new LoginRequest("testuser", "testpassword")

        then: "loginRequest is created successfully"
        loginRequest.username == "testuser"
        loginRequest.password == "testpassword"

        and: "validation passes"
        def violations = validator.validate(loginRequest)
        violations.isEmpty()
    }

    def "should fail validation when username is blank"() {
        given: "a LoginRequest with blank username"
        def loginRequest = new LoginRequest("", "testpassword")

        when: "validating the request"
        def violations = validator.validate(loginRequest)

        then: "validation fails with username constraint"
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == "username" }
        violations.any { it.message.contains("blank") }
    }

    def "should fail validation when password is blank"() {
        given: "a LoginRequest with blank password"
        def loginRequest = new LoginRequest("testuser", "")

        when: "validating the request"
        def violations = validator.validate(loginRequest)

        then: "validation fails with password constraint"
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == "password" }
        violations.any { it.message.contains("blank") }
    }

    def "should fail validation when both username and password are blank"() {
        given: "a LoginRequest with blank credentials"
        def loginRequest = new LoginRequest("", "")

        when: "validating the request"
        def violations = validator.validate(loginRequest)

        then: "validation fails with both constraints"
        violations.size() >= 2
        violations.any { it.propertyPath.toString() == "username" }
        violations.any { it.propertyPath.toString() == "password" }
    }

    def "should create LoginRequest with default empty values"() {
        when: "creating a LoginRequest with defaults"
        def loginRequest = new LoginRequest()

        then: "default values are empty strings"
        loginRequest.username == ""
        loginRequest.password == ""
    }

    def "should support data class equality"() {
        given: "two LoginRequest objects with same data"
        def request1 = new LoginRequest("user1", "pass1")
        def request2 = new LoginRequest("user1", "pass1")

        expect: "they are equal"
        request1 == request2
        request1.hashCode() == request2.hashCode()
    }

    def "should support data class inequality"() {
        given: "two LoginRequest objects with different data"
        def request1 = new LoginRequest("user1", "pass1")
        def request2 = new LoginRequest("user2", "pass2")

        expect: "they are not equal"
        request1 != request2
    }

    def "should handle whitespace-only username as invalid"() {
        given: "a LoginRequest with whitespace-only username"
        def loginRequest = new LoginRequest("   ", "testpassword")

        when: "validating the request"
        def violations = validator.validate(loginRequest)

        then: "validation fails with username constraint"
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == "username" }
    }

    def "should handle whitespace-only password as invalid"() {
        given: "a LoginRequest with whitespace-only password"
        def loginRequest = new LoginRequest("testuser", "   ")

        when: "validating the request"
        def violations = validator.validate(loginRequest)

        then: "validation fails with password constraint"
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == "password" }
    }
}
