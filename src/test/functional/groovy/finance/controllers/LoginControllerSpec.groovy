package finance.controllers

import finance.Application
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Stepwise

@Slf4j
@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
class LoginControllerSpec extends BaseControllerSpec {

    @Shared
    String testUsername = "functional_test_user"
    @Shared
    String testPassword = "FunctionalTestPass123!"
    @Shared
    String authToken

    void "test register new user successfully"() {
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
            "http://localhost:${port}/api/register",
            HttpMethod.POST, entity, String)

        then: "response should be created and contain authentication cookie"
        response.statusCode == HttpStatus.CREATED
        response.body != null
        response.body.contains("Registration successful")

        and: "response should contain Set-Cookie header"
        def cookieHeaders = response.headers.get("Set-Cookie")
        cookieHeaders != null
        cookieHeaders.any { it.contains("token=") }
    }

    void "test register user with existing username"() {
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
            "http://localhost:${port}/api/register",
            HttpMethod.POST, entity, String)

        then: "response should be conflict"
        response.statusCode == HttpStatus.CONFLICT
    }

    void "test login with valid credentials"() {
        given: "valid login credentials"
        String payload = """
        {
            "userId": 0,
            "activeStatus": true,
            "username": "${testUsername}",
            "password": "${testPassword}",
            "firstName": "",
            "lastName": ""
        }
        """

        when: "posting to login endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/login",
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


        cleanup:
        def extractedToken = tokenCookie.split("token=")[1].split(";")[0]
        authToken = extractedToken
    }

    void "test login with invalid credentials"() {
        given: "invalid login credentials"
        String payload = """
        {
            "userId": 0,
            "activeStatus": true,
            "username": "${testUsername}",
            "password": "wrong_password",
            "firstName": "",
            "lastName": ""
        }
        """

        when: "posting to login endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/login",
            HttpMethod.POST, entity, String)

        then: "response should be unauthorized"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    void "test login with non-existent user"() {
        given: "non-existent user credentials"
        String payload = """
        {
            "userId": 0,
            "activeStatus": true,
            "username": "non_existent_user",
            "password": "some_password",
            "firstName": "",
            "lastName": ""
        }
        """

        when: "posting to login endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/login",
            HttpMethod.POST, entity, String)

        then: "response should be unauthorized"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    void "test get current user with valid token"() {
        when: "getting current user with valid token"
        HttpHeaders authHeaders = new HttpHeaders()
        authHeaders.set("Cookie", "token=${authToken}")
        HttpEntity entity = new HttpEntity<>(null, authHeaders)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/me",
            HttpMethod.GET, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body != null

        and: "response should contain user data"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.username == testUsername
    }

    void "test get current user without token"() {
        when: "getting current user without token"
        HttpEntity entity = new HttpEntity<>(null, new HttpHeaders())

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/me",
            HttpMethod.GET, entity, String)

        then: "response should be unauthorized"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    void "test get current user with invalid token"() {
        when: "getting current user with invalid token"
        HttpHeaders authHeaders = new HttpHeaders()
        authHeaders.set("Cookie", "token=invalid_token_value")
        HttpEntity entity = new HttpEntity<>(null, authHeaders)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/me",
            HttpMethod.GET, entity, String)

        then: "response should be unauthorized"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    void "test logout functionality"() {
        when: "posting to logout endpoint"
        HttpEntity entity = new HttpEntity<>(null, new HttpHeaders())

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/logout",
            HttpMethod.POST, entity, String)

        then: "response should be forbidden when not authenticated"
        response.statusCode == HttpStatus.FORBIDDEN
    }

    void "test register with invalid payload"() {
        given: "invalid registration payload"
        String payload = '{"invalidField": "invalid"}'

        when: "posting to register endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/register",
            HttpMethod.POST, entity, String)

        then: "response should be bad request"
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    void "test login with malformed JSON"() {
        given: "malformed JSON payload"
        String payload = '{"username": "test", "password": incomplete'

        when: "posting to login endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/login",
            HttpMethod.POST, entity, String)

        then: "response should be bad request"
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    void "test register with missing required fields"() {
        given: "registration payload with missing password"
        String payload = '{"username": "test_user_no_password"}'

        when: "posting to register endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/register",
            HttpMethod.POST, entity, String)

        then: "response should be bad request"
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    void "test login with missing required fields"() {
        given: "login payload with missing password"
        String payload = '{"userId": 0, "activeStatus": true, "username": "test_user", "firstName": "", "lastName": ""}'

        when: "posting to login endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/login",
            HttpMethod.POST, entity, String)

        then: "response should be bad request"
        response.statusCode == HttpStatus.BAD_REQUEST
    }
}