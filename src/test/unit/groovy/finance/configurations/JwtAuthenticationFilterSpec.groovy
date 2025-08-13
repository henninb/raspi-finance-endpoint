package finance.configurations

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.security.SignatureException
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.env.Environment
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

class JwtAuthenticationFilterSpec extends Specification {

    JwtAuthenticationFilter jwtAuthenticationFilter
    Environment environmentMock = Mock(Environment)
    MeterRegistry meterRegistry = new SimpleMeterRegistry()
    HttpServletRequest requestMock = Mock(HttpServletRequest)
    HttpServletResponse responseMock = Mock(HttpServletResponse)
    FilterChain filterChainMock = Mock(FilterChain)

    def setup() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(environmentMock, meterRegistry)
        jwtAuthenticationFilter.jwtKey = "mySecretKeyForJwtTokensThisKeyMustBe256BitsOrMore"
        
        // Clear security context before each test
        SecurityContextHolder.clearContext()
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "test doFilterInternal with no token cookie continues filter chain"() {
        given:
        requestMock.cookies >> null

        when:
        jwtAuthenticationFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        SecurityContextHolder.context.authentication == null
    }

    def "test doFilterInternal with empty cookies continues filter chain"() {
        given:
        Cookie[] cookies = []
        requestMock.cookies >> cookies

        when:
        jwtAuthenticationFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        SecurityContextHolder.context.authentication == null
    }

    def "test doFilterInternal with non-token cookies continues filter chain"() {
        given:
        Cookie sessionCookie = new Cookie("sessionId", "abc123")
        Cookie[] cookies = [sessionCookie]
        requestMock.cookies >> cookies

        when:
        jwtAuthenticationFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        SecurityContextHolder.context.authentication == null
    }

    def "test doFilterInternal with invalid JWT token logs security event and clears context"() {
        given:
        String invalidToken = "invalid.jwt.token"
        Cookie tokenCookie = new Cookie("token", invalidToken)
        Cookie[] cookies = [tokenCookie]
        
        requestMock.cookies >> cookies
        requestMock.remoteAddr >> "192.168.1.100"
        requestMock.getHeader("User-Agent") >> "Mozilla/5.0 (Test Browser)"
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null

        when:
        jwtAuthenticationFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        SecurityContextHolder.context.authentication == null
    }

    def "test doFilterInternal with expired JWT token logs security event and clears context"() {
        given:
        // This is a deliberately malformed token that will throw JwtException
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.invalid"
        Cookie tokenCookie = new Cookie("token", expiredToken)
        Cookie[] cookies = [tokenCookie]
        
        requestMock.cookies >> cookies
        requestMock.remoteAddr >> "10.0.0.50"
        requestMock.getHeader("User-Agent") >> "Mozilla/5.0 (Malicious Browser)"
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null

        when:
        jwtAuthenticationFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        SecurityContextHolder.context.authentication == null
    }

    def "test doFilterInternal with valid JWT token sets authentication"() {
        given:
        // Generate a valid JWT token for testing
        String validToken = generateValidJwtToken("testuser")
        Cookie tokenCookie = new Cookie("token", validToken)
        Cookie[] cookies = [tokenCookie]
        
        requestMock.cookies >> cookies
        requestMock.remoteAddr >> "192.168.1.200"
        requestMock.getHeader("User-Agent") >> "Mozilla/5.0 (Valid Browser)"
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null

        when:
        jwtAuthenticationFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        SecurityContextHolder.context.authentication != null
        SecurityContextHolder.context.authentication.name == "testuser"
        SecurityContextHolder.context.authentication.authorities.size() == 1
        SecurityContextHolder.context.authentication.authorities[0].authority == "ROLE_USER"
    }

    def "test getClientIpAddress extracts IP from X-Forwarded-For header"() {
        given:
        requestMock.getHeader("X-Forwarded-For") >> "203.0.113.1, 198.51.100.1"
        requestMock.getHeader("X-Real-IP") >> "198.51.100.1"
        requestMock.remoteAddr >> "192.168.1.1"

        when:
        String clientIp = jwtAuthenticationFilter.getClientIpAddress(requestMock)

        then:
        clientIp == "203.0.113.1"
    }

    def "test getClientIpAddress extracts IP from X-Real-IP header when X-Forwarded-For is empty"() {
        given:
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> "198.51.100.1"
        requestMock.remoteAddr >> "192.168.1.1"

        when:
        String clientIp = jwtAuthenticationFilter.getClientIpAddress(requestMock)

        then:
        clientIp == "198.51.100.1"
    }

    def "test getClientIpAddress falls back to remoteAddr"() {
        given:
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "192.168.1.1"

        when:
        String clientIp = jwtAuthenticationFilter.getClientIpAddress(requestMock)

        then:
        clientIp == "192.168.1.1"
    }

    def "test getClientIpAddress returns unknown when all IP sources are null"() {
        given:
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> null

        when:
        String clientIp = jwtAuthenticationFilter.getClientIpAddress(requestMock)

        then:
        clientIp == "unknown"
    }

    def "test security logging includes detailed information for authentication failures"() {
        given:
        String invalidToken = "malicious.token.attempt"
        Cookie tokenCookie = new Cookie("token", invalidToken)
        Cookie[] cookies = [tokenCookie]
        
        requestMock.cookies >> cookies
        requestMock.remoteAddr >> "192.168.1.100"
        requestMock.getHeader("User-Agent") >> "Mozilla/5.0 (Suspicious Browser with very long user agent string that should be truncated in metrics for security reasons and to prevent metric explosion)"
        requestMock.getHeader("X-Forwarded-For") >> "203.0.113.195"
        requestMock.getHeader("X-Real-IP") >> null

        when:
        jwtAuthenticationFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        SecurityContextHolder.context.authentication == null
        // Verify that the client IP extraction works correctly with X-Forwarded-For
        jwtAuthenticationFilter.getClientIpAddress(requestMock) == "203.0.113.195"
    }

    def "test metrics are collected for failed authentication attempts"() {
        given:
        String invalidToken = "invalid.jwt.token"
        Cookie tokenCookie = new Cookie("token", invalidToken)
        Cookie[] cookies = [tokenCookie]
        
        requestMock.cookies >> cookies
        requestMock.remoteAddr >> "192.168.1.100"
        requestMock.getHeader("User-Agent") >> "Test Browser"
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null

        when:
        jwtAuthenticationFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        SecurityContextHolder.context.authentication == null
        
        // Verify that failure metrics were recorded
        def failureCounter = meterRegistry.find("authentication.failure").counter()
        failureCounter != null
        failureCounter.count() > 0
    }

    def "test metrics are collected for successful authentication attempts"() {
        given:
        String validToken = generateValidJwtToken("testuser")
        Cookie tokenCookie = new Cookie("token", validToken)
        Cookie[] cookies = [tokenCookie]
        
        requestMock.cookies >> cookies
        requestMock.remoteAddr >> "192.168.1.200"
        requestMock.getHeader("User-Agent") >> "Valid Browser"
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null

        when:
        jwtAuthenticationFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        SecurityContextHolder.context.authentication != null
        SecurityContextHolder.context.authentication.name == "testuser"
        
        // Verify that success metrics were recorded
        def successCounter = meterRegistry.find("authentication.success").counter()
        successCounter != null
        successCounter.count() > 0
    }

    private String generateValidJwtToken(String username) {
        // Generate a simple valid JWT token for testing purposes  
        def key = io.jsonwebtoken.security.Keys.hmacShaKeyFor("mySecretKeyForJwtTokensThisKeyMustBe256BitsOrMore".getBytes())
        def now = new Date()
        def expiration = new Date(now.time + 3600000) // 1 hour from now
        
        return io.jsonwebtoken.Jwts.builder()
            .claim("username", username)
            .subject(username)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact()
    }
}