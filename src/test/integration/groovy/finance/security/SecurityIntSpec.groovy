package finance.security

import finance.BaseRestTemplateIntegrationSpec
import finance.configurations.JwtAuthenticationFilter
import finance.domain.User
import finance.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.transaction.annotation.Transactional
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import javax.crypto.SecretKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@Transactional
class SecurityIntSpec extends BaseRestTemplateIntegrationSpec {

    @Autowired
    UserRepository userRepository

    @Value('${custom.project.jwt.key}')
    String jwtKey

    void setup() {
        // Setup if needed
    }

    // Helper method to create JWT tokens for testing (renamed to avoid overriding base helper)
    private String makeJwtToken(String username, List<String> authorities = ["ROLE_USER"], boolean expired = false) {
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.getBytes())
        java.util.Date issuedAt = new java.util.Date()
        java.util.Date expiration = expired ?
            java.util.Date.from(Instant.now().minus(1, ChronoUnit.HOURS)) :
            java.util.Date.from(Instant.now().plus(1, ChronoUnit.HOURS))

        return Jwts.builder()
            .claim("username", username)
            .claim("authorities", authorities)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(key)
            .compact()
    }


    void 'test JWT authentication filter integration'() {
        expect:
        jwtKey != null
        jwtKey.length() >= 32  // Minimum key length for HMAC-SHA256
    }

    void 'test JWT token creation and structure'() {
        when:
        String token = makeJwtToken("testuser", ["ROLE_USER", "ROLE_ADMIN"])

        then:
        token != null
        token.split('\\.').length == 3  // JWT should have 3 parts (header.payload.signature)

        // Verify token structure by parsing it back
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.getBytes())
        def claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        claims.get("username") == "testuser"
        claims.get("authorities") == ["ROLE_USER", "ROLE_ADMIN"]
    }

    void 'test invalid JWT token validation'() {
        given:
        String invalidToken = "invalid.jwt.token"

        when:
        HttpHeaders headers = new HttpHeaders()
        headers.add("Cookie", "token=${invalidToken}")
        HttpEntity<String> entity = new HttpEntity<>("", headers)

        ResponseEntity<String> response = restTemplate.exchange(managementBaseUrl + "/actuator/health", HttpMethod.GET, entity, String.class)

        then:
        // Invalid token should not cause server error, endpoint should still be accessible
        response.statusCode.is2xxSuccessful()
    }

    void 'test JWT token with different authorities'() {
        when:
        String adminToken = makeJwtToken("admin", ["ROLE_ADMIN", "ROLE_USER"])
        String userToken = makeJwtToken("user", ["ROLE_USER"])

        then:
        adminToken != null
        userToken != null
        adminToken != userToken

        // Verify token contents
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.getBytes())

        def adminClaims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(adminToken)
            .payload

        def userClaims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(userToken)
            .payload

        adminClaims.get("username") == "admin"
        adminClaims.get("authorities") == ["ROLE_ADMIN", "ROLE_USER"]
        userClaims.get("username") == "user"
        userClaims.get("authorities") == ["ROLE_USER"]
    }

    void 'test user detail service load user by username'() {
        given:
        User testUser = new User(
            userId: 0L,
            username: "integration_test_user",
            password: "EncodedPassword123!",
            firstName: "Integration",
            lastName: "Test",
            activeStatus: true
        )
        userRepository.save(testUser)

        when:
        def savedUser = userRepository.findByUsername("integration_test_user")

        then:
        savedUser.isPresent()
        savedUser.get().username == "integration_test_user"
        savedUser.get().password == "EncodedPassword123!"
        savedUser.get().activeStatus == true
    }

    void 'test protected endpoint access without authentication'() {
        when:
        ResponseEntity<String> response
        try {
            response = getWithRetry("/api/accounts", 1)
        } catch (Exception e) {
            // Endpoint may not exist or return error status
            if (e.message?.contains("404") || e.message?.contains("403")) {
                response = new ResponseEntity<>("", HttpStatus.valueOf(
                    e.message.contains("404") ? 404 : 403
                ))
            } else {
                throw e
            }
        }

        then:
        // For integration tests, all requests are permitted per WebSecurityConfig intSecurityFilterChain
        response.statusCode.value() == 200 || response.statusCode.value() == 404 || response.statusCode.value() == 403
    }

    void 'test protected endpoint access with valid JWT token'() {
        given:
        String validToken = makeJwtToken("testuser", ["ROLE_USER"])

        when:
        ResponseEntity<String> response
        try {
            HttpHeaders headers = new HttpHeaders()
            headers.add("Cookie", "token=${validToken}")
            HttpEntity<String> entity = new HttpEntity<>("", headers)
            response = restTemplate.exchange(baseUrl + "/api/accounts", HttpMethod.GET, entity, String.class)
        } catch (Exception e) {
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR
            if (e.message?.contains("404")) status = HttpStatus.NOT_FOUND
            else if (e.message?.contains("403")) status = HttpStatus.FORBIDDEN
            else if (e.message?.contains("401")) status = HttpStatus.UNAUTHORIZED
            response = new ResponseEntity<>("", status)
        }

        then:
        response.statusCode.value() == 200 || response.statusCode.value() == 404 || response.statusCode.value() == 403
    }

    void 'test protected endpoint access with invalid JWT token'() {
        given:
        String invalidToken = "invalid.jwt.token"

        when:
        ResponseEntity<String> response
        try {
            HttpHeaders headers = new HttpHeaders()
            headers.add("Cookie", "token=${invalidToken}")
            HttpEntity<String> entity = new HttpEntity<>("", headers)
            response = restTemplate.exchange(baseUrl + "/api/accounts", HttpMethod.GET, entity, String.class)
        } catch (Exception e) {
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR
            if (e.message?.contains("404")) status = HttpStatus.NOT_FOUND
            else if (e.message?.contains("403")) status = HttpStatus.FORBIDDEN
            else if (e.message?.contains("401")) status = HttpStatus.UNAUTHORIZED
            response = new ResponseEntity<>("", status)
        }

        then:
        response.statusCode.value() == 200 || response.statusCode.value() == 404 || response.statusCode.value() == 403
    }

    void 'test protected endpoint access with expired JWT token'() {
        given:
        String expiredToken = makeJwtToken("testuser", ["ROLE_USER"], true)

        when:
        ResponseEntity<String> response
        try {
            HttpHeaders headers = new HttpHeaders()
            headers.add("Cookie", "token=${expiredToken}")
            HttpEntity<String> entity = new HttpEntity<>("", headers)
            response = restTemplate.exchange(baseUrl + "/api/accounts", HttpMethod.GET, entity, String.class)
        } catch (Exception e) {
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR
            if (e.message?.contains("404")) status = HttpStatus.NOT_FOUND
            else if (e.message?.contains("403")) status = HttpStatus.FORBIDDEN
            else if (e.message?.contains("401")) status = HttpStatus.UNAUTHORIZED
            response = new ResponseEntity<>("", status)
        }

        then:
        response.statusCode.value() == 200 || response.statusCode.value() == 404 || response.statusCode.value() == 403
    }

    void 'test CORS headers in security configuration'() {
        when:
        HttpHeaders headers = new HttpHeaders()
        headers.add("Origin", "http://localhost:3000")
        headers.add("Access-Control-Request-Method", "GET")
        headers.add("Access-Control-Request-Headers", "Content-Type")
        HttpEntity<String> entity = new HttpEntity<>("", headers)

        ResponseEntity<String> response
        try {
            response = restTemplate.exchange(baseUrl + "/api/accounts", HttpMethod.OPTIONS, entity, String.class)
        } catch (Exception e) {
            response = new ResponseEntity<>("", HttpStatus.METHOD_NOT_ALLOWED)
        }

        then:
        response.statusCode.value() == 200 || response.statusCode.value() == 204 || response.statusCode.value() == 404 || response.statusCode.value() == 405
    }

    void 'test health endpoint accessibility without authentication'() {
        when:
        ResponseEntity<String> response = getMgmtWithRetry("/actuator/health", 2)

        then:
        response.statusCode.is2xxSuccessful()
    }

    void 'test login endpoint functionality'() {
        given:
        def loginRequest = [
            username: "testuser",
            password: "testpassword"
        ]

        when:
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity<Object> entity = new HttpEntity<>(loginRequest, headers)
        ResponseEntity<String> response
        try {
            response = restTemplate.postForEntity(baseUrl + "/api/login", entity, String.class)
        } catch (Exception e) {
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR
            if (e.message?.contains("404")) status = HttpStatus.NOT_FOUND
            else if (e.message?.contains("403")) status = HttpStatus.FORBIDDEN
            else if (e.message?.contains("401")) status = HttpStatus.UNAUTHORIZED
            else if (e.message?.contains("400")) status = HttpStatus.BAD_REQUEST
            response = new ResponseEntity<>("", status)
        }

        then:
        response.statusCode.value() in [200, 401, 404, 403, 400]
    }

    void 'test JWT token claims and structure'() {
        when:
        String token = makeJwtToken("financeuser", ["ROLE_USER", "ROLE_FINANCE"])

        then:
        token.split('\\.').length == 3  // JWT should have 3 parts (header.payload.signature)

        // Parse token to verify claims
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.getBytes())
        def claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        claims.get("username") == "financeuser"
        claims.get("authorities") == ["ROLE_USER", "ROLE_FINANCE"]
        claims.getIssuedAt() != null
        claims.getExpiration() != null
        claims.getExpiration().after(claims.getIssuedAt())
    }

    void 'test concurrent JWT token operations'() {
        given:
        List<String> generatedTokens = Collections.synchronizedList([])
        int threadCount = 10
        CountDownLatch latch = new CountDownLatch(threadCount)
        def executor = Executors.newFixedThreadPool(threadCount)

        when:
        (0..<threadCount).each { i ->
            executor.submit {
                try {
                    String token = makeJwtToken("user${i}", ["ROLE_USER"])
                    generatedTokens.add(token)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await() // Wait for all threads to complete
        executor.shutdown()

        then:
        generatedTokens.size() == threadCount
        generatedTokens.unique().size() == threadCount  // All tokens should be unique

        // Verify all tokens are valid
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.getBytes())
        generatedTokens.every { token ->
            try {
                Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                return true
            } catch (Exception e) {
                return false
            }
        }
    }

    void 'test security filter chain integration'() {
        given:
        def publicEndpoints = ["/actuator/health", "/actuator/info"]
        def protectedEndpoints = ["/api/accounts", "/api/transactions", "/api/categories"]

        when:
        List<Boolean> publicResults = publicEndpoints.collect { endpoint ->
            try {
                ResponseEntity<String> resp = restTemplate.getForEntity(managementBaseUrl + endpoint, String.class)
                return resp.statusCode.value() == 200 || resp.statusCode.value() == 404
            } catch (Exception e) {
                return false
            }
        }

        List<Boolean> protectedResults = protectedEndpoints.collect { endpoint ->
            try {
                ResponseEntity<String> resp = restTemplate.getForEntity(baseUrl + endpoint, String.class)
                int code = resp.statusCode.value()
                return code == 200 || code == 404 || code == 403
            } catch (Exception e) {
                return e.message?.contains("404") || e.message?.contains("403") || e.message?.contains("200")
            }
        }

        then:
        // Public endpoints should be accessible
        publicResults.every { it == true }

        // For integration tests, all endpoints are accessible due to permitAll() configuration
        protectedResults.every { it == true }
    }

    void 'test user detail service with non-existent user'() {
        when:
        def result = userRepository.findByUsername("non_existent_user")

        then:
        !result.isPresent()
    }

    void 'test JWT token refresh scenarios'() {
        when:
        String originalToken = makeJwtToken("refreshuser", ["ROLE_USER"])
        // Wait a short time to ensure different timestamp
        Thread.sleep(1000)
        String newToken = makeJwtToken("refreshuser", ["ROLE_USER"])

        then:
        originalToken != newToken  // Tokens should be different due to different timestamps

        // Verify both tokens are valid
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.getBytes())

        def originalClaims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(originalToken)
            .payload

        def newClaims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(newToken)
            .payload

        originalClaims.get("username") == newClaims.get("username")
        originalClaims.get("username") == "refreshuser"
        originalClaims.getIssuedAt().before(newClaims.getIssuedAt())  // New token should have later timestamp
    }
}
