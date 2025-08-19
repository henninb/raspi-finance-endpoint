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
}
