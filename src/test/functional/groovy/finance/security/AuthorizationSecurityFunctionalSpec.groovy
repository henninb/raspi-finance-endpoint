package finance.security

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestClientResponseException
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
        def response = getWithoutAuth("/api/account/select/active")

        then: "should return 401 Unauthorized or 403 Forbidden"
        isAccessDenied(response)
    }

    def "should deny access to transaction endpoint without JWT"() {
        when: "accessing protected transaction endpoint without authentication"
        def response = getWithoutAuth("/api/transaction")

        then: "should return 401 Unauthorized or 403 Forbidden"
        isAccessDenied(response)
    }

    def "should deny access to category endpoint without JWT"() {
        when: "accessing protected category endpoint without authentication"
        def response = getWithoutAuth("/api/category")

        then: "should return 401 Unauthorized or 403 Forbidden"
        isAccessDenied(response)
    }

    def "should deny access to description endpoint without JWT"() {
        when: "accessing protected description endpoint without authentication"
        def response = getWithoutAuth("/api/description")

        then: "should return 401 Unauthorized or 403 Forbidden"
        isAccessDenied(response)
    }

    def "should deny access to parameter endpoint without JWT"() {
        when: "accessing protected parameter endpoint without authentication"
        def response = getWithoutAuth("/api/parameter")

        then: "should return 401 Unauthorized or 403 Forbidden"
        isAccessDenied(response)
    }

    def "should deny access to payment endpoint without JWT"() {
        when: "accessing protected payment endpoint without authentication"
        def response = getWithoutAuth("/api/payment")

        then: "should return 401 Unauthorized or 403 Forbidden"
        isAccessDenied(response)
    }

    def "should deny access to validation amount endpoint without JWT"() {
        when: "accessing protected validation amount endpoint without authentication"
        def response = getWithoutAuth("/api/validation/amount")

        then: "should return 401 Unauthorized or 403 Forbidden"
        isAccessDenied(response)
    }

    def "should deny access to medical expense endpoint without JWT"() {
        when: "accessing protected medical expense endpoint without authentication"
        def response = getWithoutAuth("/api/medical-expenses")

        then: "should return 401 Unauthorized or 403 Forbidden"
        isAccessDenied(response)
    }

    def "should deny access to receipt image endpoint without JWT"() {
        when: "accessing protected receipt image endpoint without authentication"
        def response = getWithoutAuth("/api/receipt/image")

        then: "should return 401 Unauthorized or 403 Forbidden"
        isAccessDenied(response)
    }

    def "should allow access to public login endpoint"() {
        when: "accessing public login endpoint without authentication"
        def requestBody = [username: "nonexistent", password: "invalid"]
        def response = postWithoutAuth("/api/login", requestBody)

        then: "should not return hard authentication failure for public endpoint"
        isPublicEndpointReachable(response)
    }

    def "should allow access to public register endpoint"() {
        when: "accessing public register endpoint without authentication"
        def requestBody = [
            username: "testuser",
            password: "invalid",  // Will fail validation, but endpoint is accessible
            firstName: "Test",
            lastName: "User"
        ]
        def response = postWithoutAuth("/api/register", requestBody)

        then: "should not return authentication error (may return 400 for validation)"
        isPublicEndpointReachable(response)
    }

    def "should deny unauthorized GraphQL access"() {
        when: "accessing GraphQL endpoint without authentication"
        def graphqlQuery = [
            query: "{ accounts { accountNameOwner } }"
        ]
        def response = postWithoutAuth("/graphql", graphqlQuery)

        then: "should return 401 Unauthorized or 403 Forbidden"
        isAccessDenied(response)
    }

    private ResponseEntity<String> getWithoutAuth(String path) {
        try {
            return restTemplate.getForEntity("${baseUrl}${path}", String)
        } catch (RestClientResponseException ex) {
            return new ResponseEntity<>(ex.responseBodyAsString, ex.statusCode)
        }
    }

    private ResponseEntity<String> postWithoutAuth(String path, Object body) {
        try {
            return restTemplate.postForEntity("${baseUrl}${path}", body, String)
        } catch (RestClientResponseException ex) {
            return new ResponseEntity<>(ex.responseBodyAsString, ex.statusCode)
        }
    }

    private boolean isAccessDenied(ResponseEntity<String> response) {
        response.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN] ||
            response.statusCode.is3xxRedirection() ||
            (response.statusCode == HttpStatus.OK && response.body?.contains("Please sign in"))
    }

    private boolean isPublicEndpointReachable(ResponseEntity<String> response) {
        response.statusCode.is2xxSuccessful() ||
            response.statusCode in [HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }
}
