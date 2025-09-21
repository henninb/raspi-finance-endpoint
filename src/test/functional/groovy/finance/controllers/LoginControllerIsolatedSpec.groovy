package finance.controllers

import finance.helpers.SmartUserBuilder
import groovy.json.JsonSlurper
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@ActiveProfiles("func")
class LoginControllerIsolatedSpec extends BaseControllerSpec {

    @Shared
    protected String endpointName = 'login'

    @Shared
    String testUsername

    @Shared
    String testPassword = "FunctionalTestPass123!"

    @Shared
    String authToken

    void setupSpec() {
        // Generate unique test username for this test run
        testUsername = "functional_test_${testOwner.split('_')[1]}"
    }

    void 'should register new user successfully'() {
        given: "a new user registration payload using SmartUserBuilder"
        def user = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername(testUsername)
            .withPassword(testPassword)
            .withFirstName("functional")
            .withLastName("test")
            .buildAndValidate()

        String payload = user.toString()

        when: "posting to register endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/register"),
            HttpMethod.POST, entity, String)

        then: "response should be created and contain authentication cookie"
        response.statusCode == HttpStatus.CREATED
        response.body != null
        response.body.contains("Registration successful")

        and: "response should contain Set-Cookie header"
        def cookieHeaders = response.headers.get("Set-Cookie")
        cookieHeaders != null
        cookieHeaders.any { it.contains("token=") }
        0 * _
    }

    void 'should reject registration with existing username using generic error message'() {
        given: "ensure user exists first by registering them"
        def firstRegistrationUser = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername(testUsername)
            .withPassword("ValidPass123!")
            .withFirstName("functional")
            .withLastName("test")
            .buildAndValidate()

        // First registration to ensure user exists
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity firstEntity = new HttpEntity<>(firstRegistrationUser.toString(), headers)
        ResponseEntity<String> firstResponse = restTemplate.exchange(
            createURLWithPort("/api/register"),
            HttpMethod.POST, firstEntity, String)

        // We don't care if this succeeds or fails, we just want to ensure the user exists

        and: "a duplicate user registration payload using SmartUserBuilder"
        def duplicateUser = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername(testUsername)
            .withPassword("DifferentPass123!")
            .withFirstName("duplicate")
            .withLastName("user")
            .buildAndValidate()

        String payload = duplicateUser.toString()

        when: "posting duplicate registration to register endpoint"
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/register"),
            HttpMethod.POST, entity, String)

        then: "response should be bad request with generic error message"
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body != null

        and: "should contain generic error message that doesn't reveal username existence"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.error == "Registration failed. Please verify your information and try again."

        and: "should not contain specific information about username existence"
        !response.body.contains("already exists")
        !response.body.contains("Username")
        !response.body.contains("conflict")
        0 * _
    }

    void 'should login with valid credentials'() {
        given: "valid login credentials using SmartUserBuilder"
        def user = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername(testUsername)
            .withPassword(testPassword)
            .withFirstName("functional")
            .withLastName("test")
            .buildAndValidate()

        String payload = user.toString()

        when: "posting to login endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/login"),
            HttpMethod.POST, entity, String)

        then: "response should be successful with authentication cookie"
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.contains("Login successful")

        and: "response should contain Set-Cookie header with token"
        def cookieHeaders = response.headers.get("Set-Cookie")
        cookieHeaders != null
        def tokenCookie = cookieHeaders.find { it.contains("token=") }
        tokenCookie != null

        and: "extract token for subsequent tests"
        def extractedToken = tokenCookie.split("token=")[1].split(";")[0]
        extractedToken != null

        cleanup:
        authToken = extractedToken
        0 * _
    }

    void 'should reject login with invalid credentials'() {
        given: "invalid login credentials using SmartUserBuilder"
        def user = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername(testUsername)
            .withPassword("WrongPass123!")
            .withFirstName("functional")
            .withLastName("test")
            .buildAndValidate()

        String payload = user.toString()

        when: "posting to login endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/login"),
            HttpMethod.POST, entity, String)

        then: "response should be unauthorized"
        response.statusCode == HttpStatus.UNAUTHORIZED
        0 * _
    }

    void 'should reject login with non-existent user'() {
        given: "non-existent user credentials using SmartUserBuilder"
        String nonExistentUser = "non_existent_user_${testOwner.split('_')[1]}"
        def user = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername(nonExistentUser)
            .withPassword("SomePass123!")
            .withFirstName("non")
            .withLastName("existent")
            .buildAndValidate()

        String payload = user.toString()

        when: "posting to login endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/login"),
            HttpMethod.POST, entity, String)

        then: "response should be unauthorized"
        response.statusCode == HttpStatus.UNAUTHORIZED
        0 * _
    }

    void 'should get current user with valid token'() {
        when: "getting current user with valid token"
        HttpHeaders authHeaders = new HttpHeaders()
        authHeaders.set("Cookie", "token=${authToken}")
        HttpEntity entity = new HttpEntity<>(null, authHeaders)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/me"),
            HttpMethod.GET, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body != null

        and: "response should contain user data"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.username == testUsername
        0 * _
    }

    void 'should reject get current user without token'() {
        when: "getting current user without token"
        HttpEntity entity = new HttpEntity<>(null, new HttpHeaders())

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/me"),
            HttpMethod.GET, entity, String)

        then: "response should be unauthorized or forbidden"
        (response.statusCode == HttpStatus.UNAUTHORIZED || response.statusCode == HttpStatus.FORBIDDEN)
        0 * _
    }

    void 'should reject get current user with invalid token'() {
        when: "getting current user with invalid token"
        HttpHeaders authHeaders = new HttpHeaders()
        authHeaders.set("Cookie", "token=invalid_token_value")
        HttpEntity entity = new HttpEntity<>(null, authHeaders)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/me"),
            HttpMethod.GET, entity, String)

        then: "response should be unauthorized or forbidden"
        (response.statusCode == HttpStatus.UNAUTHORIZED || response.statusCode == HttpStatus.FORBIDDEN)
        0 * _
    }

    void 'should handle logout functionality'() {
        when: "posting to logout endpoint"
        HttpEntity entity = new HttpEntity<>(null, new HttpHeaders())

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/logout"),
            HttpMethod.POST, entity, String)

        then: "response should be no content or forbidden when not authenticated"
        (response.statusCode == HttpStatus.NO_CONTENT || response.statusCode == HttpStatus.FORBIDDEN)
        0 * _
    }

    void 'should reject registration with invalid payload'() {
        given: "invalid registration payload"
        String payload = '{"invalidField": "invalid"}'

        when: "posting to register endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/register"),
            HttpMethod.POST, entity, String)

        then: "response should be bad request"
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should reject login with malformed JSON'() {
        given: "malformed JSON payload"
        String payload = '{"username": "test", "password": incomplete'

        when: "posting to login endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/login"),
            HttpMethod.POST, entity, String)

        then: "response should be bad request"
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should reject registration with missing required fields using generic error message'() {
        given: "registration payload with missing password field"
        String uniqueUsername = "test_user_no_password_${testOwner.split('_')[1]}"
        String payload = """{
            "userId": 0,
            "activeStatus": true,
            "firstName": "test",
            "lastName": "user",
            "username": "${uniqueUsername}"
        }"""

        when: "posting to register endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/register"),
            HttpMethod.POST, entity, String)

        then: "response should be bad request with JSON parsing error"
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body != null

        and: "error message should contain BAD_REQUEST for missing required field"
        response.body.contains("BAD_REQUEST")
        response.body.contains("HttpMessageNotReadableException")
        0 * _
    }

    void 'should return generic error message for weak password to prevent enumeration'() {
        given: "registration with weak password pattern that passes entity validation but fails custom validation"
        String uniqueUsername = "test_weak_pass_${System.currentTimeMillis()}_${testOwner.split('_')[1]}"
        // Use password that passes entity validation (8+ chars) but fails custom validation (no uppercase/digit/special)
        def weakPasswordUser = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername(uniqueUsername)
            .withPassword("weakpassword") // 12 chars, passes entity validation but fails custom validation
            .withFirstName("test")
            .withLastName("user")
            .build()
            .toString()

        when: "posting registration with weak password"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(weakPasswordUser, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/register"),
            HttpMethod.POST, entity, String)

        then: "response should be bad request with generic error message"
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body != null

        and: "error message should be generic and not reveal specific validation details"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.error == "Registration failed. Please verify your information and try again."

        and: "should not contain specific password requirements"
        !response.body.contains("uppercase")
        !response.body.contains("lowercase")
        !response.body.contains("digit")
        !response.body.contains("special character")
        0 * _
    }

    void 'should reject login with missing required fields'() {
        given: "login payload with missing password"
        String uniqueUsername = "test_user_${testOwner.split('_')[1]}"
        String payload = "{\"userId\": 0, \"activeStatus\": true, \"username\": \"${uniqueUsername}\", \"firstName\": \"test\", \"lastName\": \"user\"}"

        when: "posting to login endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/login"),
            HttpMethod.POST, entity, String)

        then: "response should be bad request"
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }
}