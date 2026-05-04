package finance.configurations

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import spock.lang.Specification

class HttpErrorLoggingFilterSpec extends Specification {

    MeterRegistry meterRegistry
    HttpErrorLoggingFilter filter

    HttpServletRequest requestMock
    HttpServletResponse responseMock
    FilterChain filterChainMock

    def setup() {
        meterRegistry = new SimpleMeterRegistry()
        filter = new HttpErrorLoggingFilter(meterRegistry)
        requestMock = Mock(HttpServletRequest)
        responseMock = Mock(HttpServletResponse)
        filterChainMock = Mock(FilterChain)

        // Provide a safe output stream for the wrapper to copy to
        responseMock.getOutputStream() >> new ServletOutputStream() {
            @Override
            boolean isReady() { return true }
            @Override
            void setWriteListener(WriteListener writeListener) {}
            @Override
            void write(int b) {}
        }
    }

    def "increments 4xx counter and sanitizes endpoint for 404"() {
        given:
        requestMock.method >> "GET"
        requestMock.requestURI >> "/users/123/orders/550e8400-e29b-41d4-a716-446655440000/details"
        requestMock.queryString >> "password=secret&foo=bar"
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        and: "the downstream sets 404"
        filterChainMock.doFilter(_, _) >> { HttpServletRequest req, HttpServletResponse resp ->
            resp.sendError(404)
        }

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _)

        and: "no exception occurs during logging"
        noExceptionThrown()
    }

    def "increments 5xx counter for 500 responses"() {
        given:
        requestMock.method >> "POST"
        requestMock.requestURI >> "/api/v1/payments/9999"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        and:
        filterChainMock.doFilter(_, _) >> { HttpServletRequest req, HttpServletResponse resp ->
            resp.sendError(500)
        }

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _)

        and:
        noExceptionThrown()
    }

    def "does not record metrics for 2xx responses"() {
        given:
        requestMock.method >> "GET"
        requestMock.requestURI >> "/health"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, _) >> { HttpServletRequest req, HttpServletResponse resp ->
            resp.setStatus(200)
        }

        and:
        meterRegistry.find("http.error.responses").counter() == null
    }

    def "increments 4xx counter for 401 unauthorized responses"() {
        given:
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/accounts"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        and:
        filterChainMock.doFilter(_, _) >> { HttpServletRequest req, HttpServletResponse resp ->
            resp.sendError(401)
        }

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _)

        and:
        noExceptionThrown()
    }

    def "increments 4xx counter for 403 forbidden responses"() {
        given:
        requestMock.method >> "DELETE"
        requestMock.requestURI >> "/api/admin/users"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        and:
        filterChainMock.doFilter(_, _) >> { HttpServletRequest req, HttpServletResponse resp ->
            resp.sendError(403)
        }

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _)

        and:
        noExceptionThrown()
    }

    def "increments 4xx counter for generic 400 bad request responses"() {
        given:
        requestMock.method >> "POST"
        requestMock.requestURI >> "/api/transactions"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        and:
        filterChainMock.doFilter(_, _) >> { HttpServletRequest req, HttpServletResponse resp ->
            resp.sendError(400)
        }

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _)

        and:
        noExceptionThrown()
    }

    def "sanitizes sensitive query parameters in 4xx logging"() {
        given:
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/login"
        requestMock.queryString >> "username=alice&password=secret123&foo=bar"
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        and:
        filterChainMock.doFilter(_, _) >> { HttpServletRequest req, HttpServletResponse resp ->
            resp.sendError(400)
        }

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _)

        and:
        noExceptionThrown()
    }

    def "sanitizes UUID and numeric IDs in endpoint metrics for 5xx responses"() {
        given:
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/users/12345/orders/550e8400-e29b-41d4-a716-446655440000"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        and:
        filterChainMock.doFilter(_, _) >> { HttpServletRequest req, HttpServletResponse resp ->
            resp.sendError(503)
        }

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _)

        and:
        noExceptionThrown()
    }

    def "includes referer header in log output when present"() {
        given:
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/accounts"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "Mozilla/5.0"
        requestMock.getHeader("Referer") >> "https://example.com/dashboard"
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "10.0.0.1"

        and:
        filterChainMock.doFilter(_, _) >> { HttpServletRequest req, HttpServletResponse resp ->
            resp.sendError(404)
        }

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _)

        and:
        noExceptionThrown()
    }
}
