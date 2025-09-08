package finance.configurations

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
}

