package finance.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

@ActiveProfiles("func")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthorizationSecurityFunctionalSpec extends Specification {

    @LocalServerPort
    int port

    RestTemplate restTemplate = new RestTemplate()

    String getBaseUrl() {
        "http://localhost:${port}"
    }

    def "should deny access to account endpoint without JWT"() {
        when: "accessing protected account endpoint without authentication"
        ResponseEntity<String> response
        try {
            response = restTemplate.getForEntity("${baseUrl}/api/account/select/active", String)
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            response = new ResponseEntity<>(ex.responseBodyAsString, ex.statusCode)
        }

        then: "should return 401 Unauthorized or 403 Forbidden"
        response.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }

    def "should deny access to transaction endpoint without JWT"() {
        when: "accessing protected transaction endpoint without authentication"
        ResponseEntity<String> response
        try {
            response = restTemplate.getForEntity("${baseUrl}/api/transaction", String)
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            response = new ResponseEntity<>(ex.responseBodyAsString, ex.statusCode)
        }

        then: "should return 401 Unauthorized or 403 Forbidden"
        response.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }

    def "should deny access to category endpoint without JWT"() {
        when: "accessing protected category endpoint without authentication"
        ResponseEntity<String> response
        try {
            response = restTemplate.getForEntity("${baseUrl}/api/category", String)
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            response = new ResponseEntity<>(ex.responseBodyAsString, ex.statusCode)
        }

        then: "should return 401 Unauthorized or 403 Forbidden"
        response.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }

    def "should deny access to description endpoint without JWT"() {
        when: "accessing protected description endpoint without authentication"
        ResponseEntity<String> response
        try {
            response = restTemplate.getForEntity("${baseUrl}/api/description", String)
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            response = new ResponseEntity<>(ex.responseBodyAsString, ex.statusCode)
        }

        then: "should return 401 Unauthorized or 403 Forbidden"
        response.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }

    def "should deny access to parameter endpoint without JWT"() {
        when: "accessing protected parameter endpoint without authentication"
        ResponseEntity<String> response
        try {
            response = restTemplate.getForEntity("${baseUrl}/api/parameter", String)
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            response = new ResponseEntity<>(ex.responseBodyAsString, ex.statusCode)
        }

        then: "should return 401 Unauthorized or 403 Forbidden"
        response.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }

    def "should deny access to payment endpoint without JWT"() {
        when: "accessing protected payment endpoint without authentication"
        ResponseEntity<String> response
        try {
            response = restTemplate.getForEntity("${baseUrl}/api/payment", String)
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            response = new ResponseEntity<>(ex.responseBodyAsString, ex.statusCode)
        }

        then: "should return 401 Unauthorized or 403 Forbidden"
        response.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }

    def "should deny access to validation amount endpoint without JWT"() {
        when: "accessing protected validation amount endpoint without authentication"
        ResponseEntity<String> response
        try {
            response = restTemplate.getForEntity("${baseUrl}/api/validation/amount", String)
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            response = new ResponseEntity<>(ex.responseBodyAsString, ex.statusCode)
        }

        then: "should return 401 Unauthorized or 403 Forbidden"
        response.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }

    def "should deny access to medical expense endpoint without JWT"() {
        when: "accessing protected medical expense endpoint without authentication"
        ResponseEntity<String> response
        try {
            response = restTemplate.getForEntity("${baseUrl}/api/medical-expenses", String)
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            response = new ResponseEntity<>(ex.responseBodyAsString, ex.statusCode)
        }

        then: "should return 401 Unauthorized or 403 Forbidden"
        response.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }

    def "should deny access to receipt image endpoint without JWT"() {
        when: "accessing protected receipt image endpoint without authentication"
        ResponseEntity<String> response
        try {
            response = restTemplate.getForEntity("${baseUrl}/api/receipt/image", String)
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            response = new ResponseEntity<>(ex.responseBodyAsString, ex.statusCode)
        }

        then: "should return 401 Unauthorized or 403 Forbidden"
        response.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }

    def "should allow access to public login endpoint"() {
        when: "accessing public login endpoint without authentication"
        def requestBody = [username: "nonexistent", password: "invalid"]
        def response = restTemplate.postForEntity(
            "/api/login",
            requestBody,
            String
        )

        then: "should not return authentication error (may return 401 for bad credentials)"
        response.statusCode != HttpStatus.FORBIDDEN
    }

    def "should allow access to public register endpoint"() {
        when: "accessing public register endpoint without authentication"
        def requestBody = [
            username: "testuser",
            password: "invalid",  // Will fail validation, but endpoint is accessible
            firstName: "Test",
            lastName: "User"
        ]
        def response = restTemplate.postForEntity(
            "/api/register",
            requestBody,
            String
        )

        then: "should not return authentication error (may return 400 for validation)"
        response.statusCode != HttpStatus.FORBIDDEN
        response.statusCode != HttpStatus.UNAUTHORIZED || response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "should deny unauthorized GraphQL access"() {
        when: "accessing GraphQL endpoint without authentication"
        def graphqlQuery = [
            query: "{ accounts { accountNameOwner } }"
        ]
        def response = restTemplate.postForEntity(
            "/graphql",
            graphqlQuery,
            String
        )

        then: "should return 401 Unauthorized or 403 Forbidden"
        response.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }
}
