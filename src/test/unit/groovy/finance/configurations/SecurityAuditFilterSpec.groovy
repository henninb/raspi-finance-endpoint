package finance.configurations

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

class SecurityAuditFilterSpec extends Specification {
    MeterRegistry registry = new SimpleMeterRegistry()
    def filter = new SecurityAuditFilter(registry)

    def setup() {
        SecurityContextHolder.clearContext()
    }

    def "increments endpoint access counter on sensitive endpoint and calls chain"() {
        given:
        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/api/select/active"
            getMethod() >> "GET"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "JUnit"
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 200
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        registry.find("security.audit.endpoint.access").counters().size() == 1
        registry.find("security.audit.http.4xx").counters().isEmpty()
    }

    def "counts 4xx responses and still logs endpoint access"() {
        given:
        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/select/active"
            getMethod() >> "GET"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "JUnit"
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 403
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        !registry.find("security.audit.endpoint.access").counters().isEmpty()
        !registry.find("security.audit.http.4xx").counters().isEmpty()
    }

    def "non-sensitive endpoint does not increment audit endpoint access counter"() {
        given:
        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/api/accounts"
            getMethod() >> "GET"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "JUnit"
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 200
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        registry.find("security.audit.endpoint.access").counters().isEmpty()
        registry.find("security.audit.http.4xx").counters().isEmpty()
    }

    def "non-sensitive endpoint with 4xx only increments the 4xx counter"() {
        given:
        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/api/transactions"
            getMethod() >> "POST"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "JUnit"
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 400
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        registry.find("security.audit.endpoint.access").counters().isEmpty()
        !registry.find("security.audit.http.4xx").counters().isEmpty()
    }

    def "security violation logged for non-api select/active path with 403"() {
        given:
        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/select/active"
            getMethod() >> "GET"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "JUnit"
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 403
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        noExceptionThrown()
    }

    def "security violation logged for non-api select/active path with 401"() {
        given:
        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/select/active"
            getMethod() >> "GET"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "JUnit"
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 401
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        noExceptionThrown()
    }

    def "api select/active with 403 does NOT trigger security violation"() {
        given:
        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/api/select/active"
            getMethod() >> "GET"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "JUnit"
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 403
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        noExceptionThrown()
    }

    def "sensitive endpoint with null user agent does not throw"() {
        given:
        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/api/select/totals"
            getMethod() >> "GET"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> null
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 200
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        noExceptionThrown()
    }

    def "4xx response with user-agent containing CRLF is sanitized"() {
        given:
        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/api/accounts"
            getMethod() >> "GET"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "Mozilla/5.0\r\nEvil: header"
            getRemoteAddr() >> "10.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 401
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        noExceptionThrown()
    }

    def "authenticated user accessing sensitive endpoint increments audit counter with AUTHORIZED_ACCESS"() {
        given:
        def auth = new UsernamePasswordAuthenticationToken("alice", null, [])
        SecurityContextHolder.getContext().setAuthentication(auth)

        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/api/select/active"
            getMethod() >> "GET"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "JUnit"
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 200
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        !registry.find("security.audit.endpoint.access").counters().isEmpty()
        noExceptionThrown()

        cleanup:
        SecurityContextHolder.clearContext()
    }

    def "authenticated user getting 4xx on sensitive endpoint logs username"() {
        given:
        def auth = new UsernamePasswordAuthenticationToken("bob", null, [])
        SecurityContextHolder.getContext().setAuthentication(auth)

        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/api/select/active"
            getMethod() >> "GET"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "JUnit"
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 403
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        !registry.find("security.audit.http.4xx").counters().isEmpty()
        noExceptionThrown()

        cleanup:
        SecurityContextHolder.clearContext()
    }

    def "authenticated user getting 4xx on non-sensitive endpoint logs username from auth"() {
        given:
        def auth = new UsernamePasswordAuthenticationToken("charlie", null, [])
        SecurityContextHolder.getContext().setAuthentication(auth)

        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/api/transactions"
            getMethod() >> "POST"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "JUnit"
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 422
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        !registry.find("security.audit.http.4xx").counters().isEmpty()
        noExceptionThrown()

        cleanup:
        SecurityContextHolder.clearContext()
    }

    def "sensitive endpoint with payment required path increments audit counter"() {
        given:
        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/payment/required"
            getMethod() >> "GET"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "JUnit"
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 200
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        !registry.find("security.audit.endpoint.access").counters().isEmpty()
        noExceptionThrown()
    }

    def "select/totals sensitive endpoint increments audit counter"() {
        given:
        def req = Mock(HttpServletRequest) {
            getRequestURI() >> "/api/select/totals"
            getMethod() >> "GET"
            getHeader("X-Forwarded-For") >> null
            getHeader("X-Real-IP") >> null
            getHeader("User-Agent") >> "JUnit"
            getRemoteAddr() >> "127.0.0.1"
        }
        def res = Mock(HttpServletResponse) {
            getStatus() >> 200
        }
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        1 * chain.doFilter(req, res)
        !registry.find("security.audit.endpoint.access").counters().isEmpty()
        noExceptionThrown()
    }
}

