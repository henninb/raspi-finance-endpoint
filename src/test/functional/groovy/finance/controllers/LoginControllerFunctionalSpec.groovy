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
class LoginControllerFunctionalSpec extends BaseControllerFunctionalSpec {

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

        // User.toString() excludes password via @get:JsonIgnore, so build payload as Map
        String payload = jsonMapper.writeValueAsString([
            userId: user.userId,
            activeStatus: user.activeStatus,
            firstName: user.firstName,
            lastName: user.lastName,
            username: user.username,
            password: testPassword
        ])

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
        given: "a user registration payload with existing username using SmartUserBuilder"
        def user = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername(testUsername)
            .withPassword("ValidPass123!")
            .withFirstName("functional")
            .withLastName("test")
            .buildAndValidate()

        String payload = jsonMapper.writeValueAsString([
            userId: user.userId,
            activeStatus: user.activeStatus,
            firstName: user.firstName,
            lastName: user.lastName,
            username: user.username,
            password: "ValidPass123!"
        ])

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
        String payload = jsonMapper.writeValueAsString([
            username: testUsername,
            password: testPassword
        ])

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
        String payload = jsonMapper.writeValueAsString([
            username: testUsername,
            password: "WrongPass123!"
        ])

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
        String payload = jsonMapper.writeValueAsString([
            username: nonExistentUser,
            password: "SomePass123!"
        ])

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

    void 'should blacklist token on logout and reject subsequent requests'() {
        given: "a registered user with valid credentials"
        String blacklistTestUser = "blacklist_test_${testOwner.split('_')[1]}"
        String registerPayload = jsonMapper.writeValueAsString([
            userId: 0,
            activeStatus: true,
            firstName: "blacklist",
            lastName: "test",
            username: blacklistTestUser,
            password: testPassword
        ])

        // Register the user first
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity registerEntity = new HttpEntity<>(registerPayload, headers)
        ResponseEntity<String> registerResponse = restTemplate.exchange(
            createURLWithPort("/api/register"),
            HttpMethod.POST, registerEntity, String)

        and: "valid login credentials to obtain a fresh token"
        String payload = jsonMapper.writeValueAsString([
            username: blacklistTestUser,
            password: testPassword
        ])

        when: "logging in to get a token"
        HttpEntity loginEntity = new HttpEntity<>(payload, headers)
        ResponseEntity<String> loginResponse = restTemplate.exchange(
            createURLWithPort("/api/login"),
            HttpMethod.POST, loginEntity, String)

        then: "login should be successful"
        loginResponse.statusCode == HttpStatus.OK
        def cookieHeaders = loginResponse.headers.get("Set-Cookie")
        cookieHeaders != null
        def tokenCookie = cookieHeaders.find { it.contains("token=") }
        tokenCookie != null
        def token = tokenCookie.split("token=")[1].split(";")[0]

        when: "accessing protected endpoint with token before logout"
        HttpHeaders authHeaders = new HttpHeaders()
        authHeaders.set("Cookie", "token=${token}")
        HttpEntity meEntity = new HttpEntity<>(null, authHeaders)
        ResponseEntity<String> meResponse = restTemplate.exchange(
            createURLWithPort("/api/me"),
            HttpMethod.GET, meEntity, String)

        then: "should successfully access protected endpoint"
        meResponse.statusCode == HttpStatus.OK

        when: "logging out with the token"
        HttpHeaders logoutHeaders = new HttpHeaders()
        logoutHeaders.set("Cookie", "token=${token}")
        HttpEntity logoutEntity = new HttpEntity<>(null, logoutHeaders)
        ResponseEntity<String> logoutResponse = restTemplate.exchange(
            createURLWithPort("/api/logout"),
            HttpMethod.POST, logoutEntity, String)

        then: "logout should be successful"
        logoutResponse.statusCode == HttpStatus.NO_CONTENT

        when: "trying to access protected endpoint with blacklisted token"
        HttpHeaders blacklistedTokenHeaders = new HttpHeaders()
        blacklistedTokenHeaders.set("Cookie", "token=${token}")
        HttpEntity blacklistedEntity = new HttpEntity<>(null, blacklistedTokenHeaders)
        ResponseEntity<String> blacklistedResponse = restTemplate.exchange(
            createURLWithPort("/api/me"),
            HttpMethod.GET, blacklistedEntity, String)

        then: "should reject request with blacklisted token"
        (blacklistedResponse.statusCode == HttpStatus.UNAUTHORIZED || blacklistedResponse.statusCode == HttpStatus.FORBIDDEN)
        0 * _
    }

    void 'should blacklist token when logging out via Authorization header'() {
        given: "a registered user with valid credentials"
        String authHeaderTestUser = "authheader_test_${testOwner.split('_')[1]}"
        String registerPayload = jsonMapper.writeValueAsString([
            userId: 0,
            activeStatus: true,
            firstName: "authheader",
            lastName: "test",
            username: authHeaderTestUser,
            password: testPassword
        ])

        // Register the user first
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity registerEntity = new HttpEntity<>(registerPayload, headers)
        ResponseEntity<String> registerResponse = restTemplate.exchange(
            createURLWithPort("/api/register"),
            HttpMethod.POST, registerEntity, String)

        and: "valid login credentials to obtain a fresh token"
        String payload = jsonMapper.writeValueAsString([
            username: authHeaderTestUser,
            password: testPassword
        ])

        when: "logging in to get a token"
        HttpEntity loginEntity = new HttpEntity<>(payload, headers)
        ResponseEntity<String> loginResponse = restTemplate.exchange(
            createURLWithPort("/api/login"),
            HttpMethod.POST, loginEntity, String)

        then: "login should be successful"
        loginResponse.statusCode == HttpStatus.OK
        def cookieHeaders = loginResponse.headers.get("Set-Cookie")
        cookieHeaders != null
        def tokenCookie = cookieHeaders.find { it.contains("token=") }
        tokenCookie != null
        def token = tokenCookie.split("token=")[1].split(";")[0]

        when: "logging out using Authorization header"
        HttpHeaders logoutHeaders = new HttpHeaders()
        logoutHeaders.set("Authorization", "Bearer ${token}")
        HttpEntity logoutEntity = new HttpEntity<>(null, logoutHeaders)
        ResponseEntity<String> logoutResponse = restTemplate.exchange(
            createURLWithPort("/api/logout"),
            HttpMethod.POST, logoutEntity, String)

        then: "logout should be successful"
        logoutResponse.statusCode == HttpStatus.NO_CONTENT

        when: "trying to access protected endpoint with blacklisted token via Authorization header"
        HttpHeaders blacklistedTokenHeaders = new HttpHeaders()
        blacklistedTokenHeaders.set("Authorization", "Bearer ${token}")
        HttpEntity blacklistedEntity = new HttpEntity<>(null, blacklistedTokenHeaders)
        ResponseEntity<String> blacklistedResponse = restTemplate.exchange(
            createURLWithPort("/api/me"),
            HttpMethod.GET, blacklistedEntity, String)

        then: "should reject request with blacklisted token"
        (blacklistedResponse.statusCode == HttpStatus.UNAUTHORIZED || blacklistedResponse.statusCode == HttpStatus.FORBIDDEN)
        0 * _
    }

    void 'should handle logout gracefully with invalid token'() {
        when: "logging out with an invalid token"
        HttpHeaders logoutHeaders = new HttpHeaders()
        logoutHeaders.set("Cookie", "token=invalid_token_value")
        HttpEntity logoutEntity = new HttpEntity<>(null, logoutHeaders)
        ResponseEntity<String> logoutResponse = restTemplate.exchange(
            createURLWithPort("/api/logout"),
            HttpMethod.POST, logoutEntity, String)

        then: "logout should still succeed or be forbidden (both are acceptable for invalid tokens)"
        (logoutResponse.statusCode == HttpStatus.NO_CONTENT || logoutResponse.statusCode == HttpStatus.FORBIDDEN)
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