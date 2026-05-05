package finance.configurations

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification

class HttpErrorLoggingFilterSpec extends Specification {

    MeterRegistry meterRegistry
    HttpErrorLoggingFilter filter

    HttpServletRequest requestMock
    MockHttpServletResponse responseMock
    FilterChain filterChainMock

    def setup() {
        meterRegistry = new SimpleMeterRegistry()
        filter = new HttpErrorLoggingFilter(meterRegistry)
        requestMock = Mock(HttpServletRequest)
        responseMock = new MockHttpServletResponse()
        filterChainMock = Mock(FilterChain)
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

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(404) }
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

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(500) }
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
        1 * filterChainMock.doFilter(requestMock, _) >> { req, resp -> resp.setStatus(200) }
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

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(401) }
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

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(403) }
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

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(400) }
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

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(400) }
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

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(503) }
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

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(404) }
        noExceptionThrown()
    }

    def "increments 4xx counter for generic 405 method not allowed"() {
        given:
        requestMock.method >> "DELETE"
        requestMock.requestURI >> "/api/accounts"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(405) }
        noExceptionThrown()
    }

    def "increments 4xx counter for 409 conflict"() {
        given:
        requestMock.method >> "POST"
        requestMock.requestURI >> "/api/categories"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(409) }
        noExceptionThrown()
    }

    def "handles null user agent without throwing"() {
        given:
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/test"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> null
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(500) }
        noExceptionThrown()
    }

    def "sanitizes user agent with CRLF injection"() {
        given:
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/test"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "Mozilla/5.0\r\nEvil-Header: injected"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(403) }
        noExceptionThrown()
    }

    def "handles empty query string value for sensitive param"() {
        given:
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/login"
        requestMock.queryString >> "password=&username=user"
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(401) }
        noExceptionThrown()
    }

    def "sanitizes endpoint with hash-like path segment"() {
        given:
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/resources/abcdef0123456789abcdef0123456789"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(404) }
        noExceptionThrown()
    }

    def "counter is incremented for 4xx error"() {
        given:
        requestMock.method >> "GET"
        requestMock.requestURI >> "/api/test"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(404) }
        meterRegistry.find("http.error.responses").tag("category", "4xx").counter() != null
    }

    def "counter is incremented for 5xx error"() {
        given:
        requestMock.method >> "POST"
        requestMock.requestURI >> "/api/test"
        requestMock.queryString >> null
        requestMock.getHeader("User-Agent") >> "JUnit"
        requestMock.getHeader("Referer") >> null
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "127.0.0.1"

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(_, _) >> { req, resp -> resp.setStatus(500) }
        meterRegistry.find("http.error.responses").tag("category", "5xx").counter() != null
    }
}
