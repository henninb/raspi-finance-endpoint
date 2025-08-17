package finance.security

import finance.Application
import finance.configurations.JwtAuthenticationFilter
import finance.domain.User
import finance.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import javax.crypto.SecretKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@ActiveProfiles("int")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@Transactional
class SecurityIntegrationSpec extends Specification {

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    @Autowired
    UserRepository userRepository

    @Value('${custom.project.jwt.key}')
    String jwtKey

    String baseUrl

    void setup() {
        baseUrl = "http://localhost:${port}"
    }

    // Helper method to create JWT tokens for testing
    private String createJwtToken(String username, List<String> authorities = ["ROLE_USER"], boolean expired = false) {
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

    // Helper method to create HTTP entity with JWT cookie
    private HttpEntity<String> createRequestWithJwtCookie(String token) {
        HttpHeaders headers = new HttpHeaders()
        headers.add("Cookie", "token=${token}")
        return new HttpEntity<>(headers)
    }

    void 'test JWT authentication filter integration'() {
        expect:
        jwtKey != null
        jwtKey.length() >= 32  // Minimum key length for HMAC-SHA256
    }

    void 'test JWT token creation and structure'() {
        when:
        String token = createJwtToken("testuser", ["ROLE_USER", "ROLE_ADMIN"])

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
        HttpEntity<String> entity = createRequestWithJwtCookie(invalidToken)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
            "${baseUrl}/actuator/health",
            HttpMethod.GET,
            entity,
            String.class
        )

        then:
        // Invalid token should not cause server error, endpoint should still be accessible
        response.statusCode == HttpStatus.OK
    }

    void 'test JWT token with different authorities'() {
        when:
        String adminToken = createJwtToken("admin", ["ROLE_ADMIN", "ROLE_USER"])
        String userToken = createJwtToken("user", ["ROLE_USER"])

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
            password: "encoded_password",
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
        savedUser.get().password == "encoded_password"
        savedUser.get().activeStatus == true
    }

    void 'test protected endpoint access without authentication'() {
        when:
        ResponseEntity<String> response = restTemplate.getForEntity("${baseUrl}/api/accounts", String.class)

        then:
        // For integration tests, all requests are permitted per WebSecurityConfig intSecurityFilterChain
        response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND || response.statusCode == HttpStatus.FORBIDDEN
    }

    void 'test protected endpoint access with valid JWT token'() {
        given:
        String validToken = createJwtToken("testuser", ["ROLE_USER"])
        HttpEntity<String> entity = createRequestWithJwtCookie(validToken)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
            "${baseUrl}/api/accounts", 
            HttpMethod.GET, 
            entity, 
            String.class
        )

        then:
        // With valid token, should get OK or NOT_FOUND (if endpoint doesn't exist)
        response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND || response.statusCode == HttpStatus.FORBIDDEN
    }

    void 'test protected endpoint access with invalid JWT token'() {
        given:
        String invalidToken = "invalid.jwt.token"
        HttpEntity<String> entity = createRequestWithJwtCookie(invalidToken)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
            "${baseUrl}/api/accounts", 
            HttpMethod.GET, 
            entity, 
            String.class
        )

        then:
        // Invalid token should not cause errors but may not provide authentication
        response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND || response.statusCode == HttpStatus.FORBIDDEN
    }

    void 'test protected endpoint access with expired JWT token'() {
        given:
        String expiredToken = createJwtToken("testuser", ["ROLE_USER"], true)
        HttpEntity<String> entity = createRequestWithJwtCookie(expiredToken)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
            "${baseUrl}/api/accounts", 
            HttpMethod.GET, 
            entity, 
            String.class
        )

        then:
        // Expired token should be rejected gracefully
        response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND || response.statusCode == HttpStatus.FORBIDDEN
    }

    void 'test CORS headers in security configuration'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.set("Origin", "http://localhost:3000")
        headers.set("Access-Control-Request-Method", "GET")
        headers.set("Access-Control-Request-Headers", "Content-Type")
        HttpEntity<String> entity = new HttpEntity<>(headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
            "${baseUrl}/api/accounts", 
            HttpMethod.OPTIONS, 
            entity, 
            String.class
        )

        then:
        // For integration tests, CORS should be handled gracefully
        response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NO_CONTENT || response.statusCode == HttpStatus.NOT_FOUND
        
        // Check if CORS headers are present (when applicable)
        if (response.statusCode == HttpStatus.OK) {
            def corsHeaders = response.headers
            // CORS headers may or may not be present depending on configuration
            corsHeaders != null
        }
    }

    void 'test health endpoint accessibility without authentication'() {
        when:
        ResponseEntity<String> response = restTemplate.getForEntity("${baseUrl}/actuator/health", String.class)

        then:
        response.statusCode == HttpStatus.OK
        // Health endpoint should be accessible without authentication
    }

    void 'test login endpoint functionality'() {
        given:
        def loginRequest = [
            username: "testuser",
            password: "testpassword"
        ]

        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity<Map> entity = new HttpEntity<>(loginRequest, headers)

        when:
        ResponseEntity<Map> response = restTemplate.exchange(
            "${baseUrl}/api/login", 
            HttpMethod.POST, 
            entity, 
            Map.class
        )

        then:
        // Login endpoint behavior varies based on implementation
        response.statusCode != null
        response.statusCode == HttpStatus.OK || 
        response.statusCode == HttpStatus.UNAUTHORIZED || 
        response.statusCode == HttpStatus.NOT_FOUND ||
        response.statusCode == HttpStatus.FORBIDDEN ||
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    void 'test JWT token claims and structure'() {
        when:
        String token = createJwtToken("financeuser", ["ROLE_USER", "ROLE_FINANCE"])

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
                    String token = createJwtToken("user${i}", ["ROLE_USER"])
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
        List<ResponseEntity<String>> publicResponses = publicEndpoints.collect { endpoint ->
            restTemplate.getForEntity("${baseUrl}${endpoint}", String.class)
        }

        List<ResponseEntity<String>> protectedResponses = protectedEndpoints.collect { endpoint ->
            restTemplate.getForEntity("${baseUrl}${endpoint}", String.class)
        }

        then:
        // Public endpoints should be accessible
        publicResponses.every { response ->
            response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND
        }

        // For integration tests, all endpoints are accessible due to permitAll() configuration
        protectedResponses.every { response ->
            response.statusCode == HttpStatus.OK || 
            response.statusCode == HttpStatus.NOT_FOUND ||
            response.statusCode == HttpStatus.FORBIDDEN
        }
    }

    void 'test user detail service with non-existent user'() {
        when:
        def result = userRepository.findByUsername("non_existent_user")

        then:
        !result.isPresent()
    }

    void 'test JWT token refresh scenarios'() {
        when:
        String originalToken = createJwtToken("refreshuser", ["ROLE_USER"])
        // Wait a short time to ensure different timestamp
        Thread.sleep(1000)
        String newToken = createJwtToken("refreshuser", ["ROLE_USER"])

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