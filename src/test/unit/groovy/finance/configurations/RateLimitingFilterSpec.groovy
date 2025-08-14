package finance.configurations

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import spock.lang.Specification

import java.io.PrintWriter
import java.util.concurrent.TimeUnit

class RateLimitingFilterSpec extends Specification {

    RateLimitingFilter rateLimitingFilter
    HttpServletRequest requestMock = Mock(HttpServletRequest)
    HttpServletResponse responseMock = Mock(HttpServletResponse)
    FilterChain filterChainMock = Mock(FilterChain)
    PrintWriter writerMock = Mock(PrintWriter)

    def setup() {
        rateLimitingFilter = new RateLimitingFilter()
        responseMock.writer >> writerMock
    }

    def "test doFilterInternal with rate limiting disabled continues filter chain"() {
        given:
        rateLimitingFilter.rateLimitingEnabled = false
        requestMock.remoteAddr >> "192.168.1.1"

        when:
        rateLimitingFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        0 * responseMock.setStatus(_)
    }

    def "test doFilterInternal allows requests within rate limit"() {
        given:
        rateLimitingFilter.rateLimitingEnabled = true
        rateLimitingFilter.rateLimitPerMinute = 500
        rateLimitingFilter.windowSizeMinutes = 1L
        requestMock.remoteAddr >> "192.168.1.1"
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null

        when:
        // Make 5 requests (well within the 500 limit)
        5.times {
            rateLimitingFilter.doFilterInternal(requestMock, responseMock, filterChainMock)
        }

        then:
        5 * filterChainMock.doFilter(requestMock, responseMock)
        5 * responseMock.setHeader("X-RateLimit-Limit", "500")
        5 * responseMock.setHeader("X-RateLimit-Remaining", _)
        5 * responseMock.setHeader("X-RateLimit-Reset", _)
        0 * responseMock.setStatus(HttpStatus.TOO_MANY_REQUESTS.value())
    }

    def "test doFilterInternal blocks requests exceeding rate limit"() {
        given:
        rateLimitingFilter.rateLimitingEnabled = true
        rateLimitingFilter.rateLimitPerMinute = 2 // Set very low limit for testing
        rateLimitingFilter.windowSizeMinutes = 1L
        requestMock.remoteAddr >> "192.168.1.1"
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null

        when:
        // Make 3 requests (exceeds limit of 2)
        3.times {
            rateLimitingFilter.doFilterInternal(requestMock, responseMock, filterChainMock)
        }

        then:
        // First 2 requests should pass through
        2 * filterChainMock.doFilter(requestMock, responseMock)
        
        // Third request should be blocked
        1 * responseMock.setStatus(HttpStatus.TOO_MANY_REQUESTS.value())
        (1.._) * responseMock.setHeader("X-RateLimit-Remaining", _)
        1 * responseMock.setContentType("application/json")
        1 * writerMock.write('{"error":"Rate limit exceeded","message":"Too many requests"}')
        (0.._) * responseMock.setHeader("X-RateLimit-Limit", _)
        (0.._) * responseMock.setHeader("X-RateLimit-Reset", _)
    }

    def "test doFilterInternal with different IPs are tracked separately"() {
        given:
        rateLimitingFilter.rateLimitingEnabled = true
        rateLimitingFilter.rateLimitPerMinute = 2
        rateLimitingFilter.windowSizeMinutes = 1L
        
        HttpServletRequest request1Mock = Mock(HttpServletRequest)
        HttpServletRequest request2Mock = Mock(HttpServletRequest)

        when:
        // IP 1 makes 2 requests (at limit)
        request1Mock.remoteAddr >> "192.168.1.1"
        request1Mock.getHeader("X-Forwarded-For") >> null
        request1Mock.getHeader("X-Real-IP") >> null
        2.times {
            rateLimitingFilter.doFilterInternal(request1Mock, responseMock, filterChainMock)
        }

        // IP 2 makes 2 requests (should also be allowed)
        request2Mock.remoteAddr >> "192.168.1.2"
        request2Mock.getHeader("X-Forwarded-For") >> null
        request2Mock.getHeader("X-Real-IP") >> null
        2.times {
            rateLimitingFilter.doFilterInternal(request2Mock, responseMock, filterChainMock)
        }

        then:
        4 * filterChainMock.doFilter(_, responseMock)
        0 * responseMock.setStatus(HttpStatus.TOO_MANY_REQUESTS.value())
        (0.._) * responseMock.setHeader("X-RateLimit-Limit", _)
        (0.._) * responseMock.setHeader("X-RateLimit-Remaining", _)
        (0.._) * responseMock.setHeader("X-RateLimit-Reset", _)
    }

    def "test getClientIpAddress extracts IP from X-Forwarded-For header"() {
        given:
        requestMock.getHeader("X-Forwarded-For") >> "203.0.113.1, 198.51.100.1"
        requestMock.getHeader("X-Real-IP") >> "198.51.100.1"
        requestMock.remoteAddr >> "192.168.1.1"

        when:
        String clientIp = rateLimitingFilter.getClientIpAddress(requestMock)

        then:
        clientIp == "203.0.113.1"
    }

    def "test getClientIpAddress extracts IP from X-Real-IP header when X-Forwarded-For is empty"() {
        given:
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> "198.51.100.1"
        requestMock.remoteAddr >> "192.168.1.1"

        when:
        String clientIp = rateLimitingFilter.getClientIpAddress(requestMock)

        then:
        clientIp == "198.51.100.1"
    }

    def "test getClientIpAddress falls back to remoteAddr"() {
        given:
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "192.168.1.1"

        when:
        String clientIp = rateLimitingFilter.getClientIpAddress(requestMock)

        then:
        clientIp == "192.168.1.1"
    }

    def "test getClientIpAddress returns unknown when all IP sources are null"() {
        given:
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> null

        when:
        String clientIp = rateLimitingFilter.getClientIpAddress(requestMock)

        then:
        clientIp == "unknown"
    }

    def "test rate limit headers are correctly set for allowed requests"() {
        given:
        rateLimitingFilter.rateLimitingEnabled = true
        rateLimitingFilter.rateLimitPerMinute = 500
        rateLimitingFilter.windowSizeMinutes = 1L
        requestMock.remoteAddr >> "192.168.1.1"
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null

        when:
        rateLimitingFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        1 * responseMock.setHeader("X-RateLimit-Limit", "500")
        1 * responseMock.setHeader("X-RateLimit-Remaining", "499") // 500 - 1 request
        1 * responseMock.setHeader("X-RateLimit-Reset", _)
    }

    def "test rate limit headers are correctly set for blocked requests"() {
        given:
        rateLimitingFilter.rateLimitingEnabled = true
        rateLimitingFilter.rateLimitPerMinute = 1 // Very low limit
        rateLimitingFilter.windowSizeMinutes = 1L
        requestMock.remoteAddr >> "192.168.1.1"
        requestMock.getHeader("X-Forwarded-For") >> null
        requestMock.getHeader("X-Real-IP") >> null

        when:
        // First request - should pass
        rateLimitingFilter.doFilterInternal(requestMock, responseMock, filterChainMock)
        // Second request - should be blocked
        rateLimitingFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        1 * responseMock.setStatus(HttpStatus.TOO_MANY_REQUESTS.value())
        (1.._) * responseMock.setHeader("X-RateLimit-Limit", _)
        (1.._) * responseMock.setHeader("X-RateLimit-Remaining", _)
        (1.._) * responseMock.setHeader("X-RateLimit-Reset", _)
        1 * responseMock.setContentType("application/json")
        1 * writerMock.write('{"error":"Rate limit exceeded","message":"Too many requests"}')
    }

    def "test default rate limit is 500 requests per minute"() {
        given:
        def filter = new RateLimitingFilter()

        expect:
        filter.rateLimitPerMinute == 500
        filter.windowSizeMinutes == 1L
        filter.rateLimitingEnabled == true
    }

    def "test X-Forwarded-For with multiple IPs uses first one"() {
        given:
        rateLimitingFilter.rateLimitingEnabled = true
        rateLimitingFilter.rateLimitPerMinute = 500
        requestMock.getHeader("X-Forwarded-For") >> "203.0.113.1, 198.51.100.1, 192.168.1.1"
        requestMock.getHeader("X-Real-IP") >> null
        requestMock.remoteAddr >> "192.168.1.1"

        when:
        rateLimitingFilter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter(requestMock, responseMock)
        // Verify that the first IP from X-Forwarded-For is used
        rateLimitingFilter.getClientIpAddress(requestMock) == "203.0.113.1"
    }
}