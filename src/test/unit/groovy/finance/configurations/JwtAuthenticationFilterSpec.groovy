package finance.configurations

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

    private void setJwtKey(JwtAuthenticationFilter filter, String key) {
        def f = JwtAuthenticationFilter.class.getDeclaredField('jwtKey')
        f.accessible = true
        f.set(filter, key)
    }

    def "sets authentication from valid Bearer token"() {
        given:
        def meter = new SimpleMeterRegistry()
        def filter = new JwtAuthenticationFilter(meter)
        String secret = 'a' * 64 // 64-byte key
        setJwtKey(filter, secret)
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes())
        String token = Jwts.builder().claim('username', 'alice').signWith(key).compact()

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
        def filter = new JwtAuthenticationFilter(meter)
        String secret = 'b' * 64
        setJwtKey(filter, secret)
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes())
        String token = Jwts.builder().claim('username', 'bob').signWith(key).compact()

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
        def filter = new JwtAuthenticationFilter(meter)
        String secret = 'c' * 64
        setJwtKey(filter, secret)
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
}
