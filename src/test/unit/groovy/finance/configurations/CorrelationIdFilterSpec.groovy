package finance.configurations

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import spock.lang.Specification

class CorrelationIdFilterSpec extends Specification {

    CorrelationIdFilter filter
    HttpServletRequest requestMock = Mock(HttpServletRequest)
    HttpServletResponse responseMock = Mock(HttpServletResponse)
    FilterChain filterChainMock = Mock(FilterChain)

    def setup() {
        filter = new CorrelationIdFilter()
        MDC.clear()
    }

    def cleanup() {
        MDC.clear()
    }

    def "uses X-Correlation-ID header value when present"() {
        given:
        requestMock.getHeader("X-Correlation-ID") >> "test-correlation-123"
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/test"

        when:
        filter.doFilter(requestMock, responseMock, filterChainMock)

        then:
        1 * responseMock.setHeader("X-Correlation-ID", "test-correlation-123")
        1 * filterChainMock.doFilter(requestMock, responseMock)
    }

    def "generates UUID when X-Correlation-ID header is absent"() {
        given:
        requestMock.getHeader("X-Correlation-ID") >> null
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/test"
        String capturedId = null

        when:
        filter.doFilter(requestMock, responseMock, filterChainMock)

        then:
        1 * responseMock.setHeader("X-Correlation-ID", _) >> { String header, String value ->
            capturedId = value
        }
        1 * filterChainMock.doFilter(requestMock, responseMock)
        capturedId ==~ /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/
    }

    def "generates UUID when X-Correlation-ID header is blank"() {
        given:
        requestMock.getHeader("X-Correlation-ID") >> "   "
        requestMock.method >> "POST"
        requestMock.requestURI >> "/api/transactions"

        when:
        filter.doFilter(requestMock, responseMock, filterChainMock)

        then:
        1 * responseMock.setHeader("X-Correlation-ID", { it ==~ /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/ })
        1 * filterChainMock.doFilter(requestMock, responseMock)
    }

    def "MDC is cleared after request completes"() {
        given:
        requestMock.getHeader("X-Correlation-ID") >> "trace-abc"
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/accounts"

        when:
        filter.doFilter(requestMock, responseMock, filterChainMock)

        then:
        MDC.get("correlationId") == null
    }

    def "MDC is cleared even when filter chain throws"() {
        given:
        requestMock.getHeader("X-Correlation-ID") >> "trace-abc"
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/accounts"
        filterChainMock.doFilter(_, _) >> { throw new RuntimeException("downstream error") }

        when:
        filter.doFilter(requestMock, responseMock, filterChainMock)

        then:
        thrown(RuntimeException)
        MDC.get("correlationId") == null
    }

    def "trims whitespace from provided correlation ID"() {
        given:
        requestMock.getHeader("X-Correlation-ID") >> "  trimmed-id  "
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/test"

        when:
        filter.doFilter(requestMock, responseMock, filterChainMock)

        then:
        1 * responseMock.setHeader("X-Correlation-ID", "trimmed-id")
    }
}
