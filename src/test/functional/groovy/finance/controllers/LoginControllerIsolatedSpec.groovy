package finance.controllers

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
        given: "a new user registration payload"
        String payload = """
        {
            "userId": 0,
            "activeStatus": true,
            "username": "${testUsername}",
            "password": "${testPassword}",
            "firstName": "functional",
            "lastName": "test"
        }
        """

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

    void 'should reject registration with existing username'() {
        given: "a user registration payload with existing username"
        String payload = """
        {
            "userId": 0,
            "activeStatus": true,
            "username": "${testUsername}",
            "password": "ValidPass123!",
            "firstName": "functional",
            "lastName": "test"
        }
        """

        when: "posting to register endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/register"),
            HttpMethod.POST, entity, String)

        then: "response should be conflict"
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'should login with valid credentials'() {
        given: "valid login credentials"
        String payload = """
        {
            "userId": 0,
            "activeStatus": true,
            "username": "${testUsername}",
            "password": "${testPassword}",
            "firstName": "functional",
            "lastName": "test"
        }
        """

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
        given: "invalid login credentials"
        String payload = """
        {
            "userId": 0,
            "activeStatus": true,
            "username": "${testUsername}",
            "password": "WrongPass123!",
            "firstName": "functional",
            "lastName": "test"
        }
        """

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
        given: "non-existent user credentials"
        String nonExistentUser = "non_existent_user_${testOwner.split('_')[1]}"
        String payload = """
        {
            "userId": 0,
            "activeStatus": true,
            "username": "${nonExistentUser}",
            "password": "SomePass123!",
            "firstName": "non",
            "lastName": "existent"
        }
        """

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

        then: "response should be forbidden"
        response.statusCode == HttpStatus.FORBIDDEN
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

        then: "response should be forbidden"
        response.statusCode == HttpStatus.FORBIDDEN
        0 * _
    }

    void 'should handle logout functionality'() {
        when: "posting to logout endpoint"
        HttpEntity entity = new HttpEntity<>(null, new HttpHeaders())

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/logout"),
            HttpMethod.POST, entity, String)

        then: "response should be forbidden when not authenticated"
        response.statusCode == HttpStatus.FORBIDDEN
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

    void 'should reject registration with missing required fields'() {
        given: "registration payload with missing password"
        String uniqueUsername = "test_user_no_password_${testOwner.split('_')[1]}"
        String payload = "{\"username\": \"${uniqueUsername}\"}"

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