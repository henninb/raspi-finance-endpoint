package finance.configurations

import finance.services.TokenBlacklistService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

import javax.crypto.SecretKey

class JwtAuthenticationFilterSpec extends Specification {

    def setup() {
        SecurityContextHolder.clearContext()
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "sets authentication from valid Bearer token"() {
        given:
        def meter = new SimpleMeterRegistry()
        def tokenBlacklistService = Mock(TokenBlacklistService)
        tokenBlacklistService.isBlacklisted(_) >> false
        String secret = 'a' * 64 // 64-byte key
        def filter = new JwtAuthenticationFilter(meter, tokenBlacklistService, secret, new CustomProperties())
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes())
        String token = Jwts.builder().issuer('raspi-finance-endpoint').audience().add('raspi-finance-endpoint').and().claim('username', 'alice').expiration(new Date(System.currentTimeMillis() + 3600_000L)).signWith(key).compact()

        def req = Mock(HttpServletRequest)
        req.getCookies() >> null
        req.getHeader('Cookie') >> null
        req.getHeader('Authorization') >> "Bearer ${token}"
        def res = Mock(HttpServletResponse)
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        SecurityContextHolder.context.authentication != null
        SecurityContextHolder.context.authentication.principal == 'alice'
        SecurityContextHolder.context.authentication.authorities*.authority.containsAll(['ROLE_USER','USER'])
        1 * chain.doFilter(req, res)
    }

    def "sets authentication from token cookie"() {
        given:
        def meter = new SimpleMeterRegistry()
        def tokenBlacklistService = Mock(TokenBlacklistService)
        tokenBlacklistService.isBlacklisted(_) >> false
        String secret = 'b' * 64
        def filter = new JwtAuthenticationFilter(meter, tokenBlacklistService, secret, new CustomProperties())
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes())
        String token = Jwts.builder().issuer('raspi-finance-endpoint').audience().add('raspi-finance-endpoint').and().claim('username', 'bob').expiration(new Date(System.currentTimeMillis() + 3600_000L)).signWith(key).compact()

        def req = Mock(HttpServletRequest)
        req.getCookies() >> ([new Cookie('token', token)] as Cookie[])
        req.getHeader('Cookie') >> null
        req.getHeader('Authorization') >> null
        req.getHeader('X-Forwarded-For') >> null
        req.getHeader('X-Real-IP') >> null
        req.getRemoteAddr() >> '127.0.0.1'
        def res = Mock(HttpServletResponse)
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        SecurityContextHolder.context.authentication?.principal == 'bob'
        1 * chain.doFilter(req, res)
    }

    def "invalid token clears SecurityContext and continues chain"() {
        given:
        def meter = new SimpleMeterRegistry()
        def tokenBlacklistService = Mock(TokenBlacklistService)
        tokenBlacklistService.isBlacklisted(_) >> false
        String secret = 'c' * 64
        def filter = new JwtAuthenticationFilter(meter, tokenBlacklistService, secret, new CustomProperties())
        // Malformed token
        String token = 'not.a.valid.token'

        def req = Mock(HttpServletRequest)
        req.getCookies() >> null
        req.getHeader('Cookie') >> null
        req.getHeader('Authorization') >> "Bearer ${token}"
        req.getHeader('User-Agent') >> 'JUnit'
        req.getHeader('X-Forwarded-For') >> null
        req.getHeader('X-Real-IP') >> null
        req.getRemoteAddr() >> '127.0.0.1'
        def res = Mock(HttpServletResponse)
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        SecurityContextHolder.context.authentication == null
        1 * chain.doFilter(req, res)
    }

    def "blacklisted token clears SecurityContext and continues chain"() {
        given:
        def meter = new SimpleMeterRegistry()
        def tokenBlacklistService = Mock(TokenBlacklistService)
        String secret = 'd' * 64
        def filter = new JwtAuthenticationFilter(meter, tokenBlacklistService, secret, new CustomProperties())
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes())
        String token = Jwts.builder().issuer('raspi-finance-endpoint').audience().add('raspi-finance-endpoint').and().claim('username', 'charlie').expiration(new Date(System.currentTimeMillis() + 3600_000L)).signWith(key).compact()

        // Configure mock to return true for this specific token
        tokenBlacklistService.isBlacklisted(token) >> true

        def req = Mock(HttpServletRequest)
        req.getCookies() >> null
        req.getHeader('Cookie') >> null
        req.getHeader('Authorization') >> "Bearer ${token}"
        req.getHeader('X-Forwarded-For') >> null
        req.getHeader('X-Real-IP') >> null
        req.getRemoteAddr() >> '127.0.0.1'
        def res = Mock(HttpServletResponse)
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * tokenBlacklistService.isBlacklisted(token) >> true
        SecurityContextHolder.context.authentication == null
        1 * chain.doFilter(req, res)
    }
}
