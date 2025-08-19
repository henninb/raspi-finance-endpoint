package finance.configurations

import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.util.ContentCachingRequestWrapper
import spock.lang.Specification

class RequestLoggingFilterSpec extends Specification {

    RequestLoggingFilter filter
    HttpServletRequest requestMock
    HttpServletResponse responseMock
    FilterChain filterChainMock

    def setup() {
        filter = new RequestLoggingFilter()
        requestMock = Mock(HttpServletRequest)
        responseMock = Mock(HttpServletResponse)
        filterChainMock = Mock(FilterChain)
    }

    private static ServletInputStream inputStreamWith(byte[] bytes) {
        def bais = new ByteArrayInputStream(bytes)
        return new ServletInputStream() {
            @Override
            boolean isFinished() { return bais.available() == 0 }
            @Override
            boolean isReady() { return true }
            @Override
            void setReadListener(ReadListener readListener) {}
            @Override
            int read() { return bais.read() }
        }
    }

    def "wraps raw request before invoking chain"() {
        given:
        def body = '{"hello":"world"}'.getBytes("UTF-8")
        requestMock.getInputStream() >> inputStreamWith(body)
        requestMock.getRequestURI() >> "/api/test"

        when:
        filter.doFilterInternal(requestMock, responseMock, filterChainMock)

        then:
        1 * filterChainMock.doFilter({ it instanceof ContentCachingRequestWrapper }, responseMock)
    }

    def "reuses existing ContentCachingRequestWrapper"() {
        given:
        def body = '{"ping":true}'.getBytes("UTF-8")

        HttpServletRequest baseReq = Mock(HttpServletRequest)
        baseReq.getInputStream() >> inputStreamWith(body)
        baseReq.getRequestURI() >> "/api/echo"

        def wrapped = new ContentCachingRequestWrapper(baseReq, 1024)  // Added cache limit for Spring Boot 4.0.0-M1

        when:
        filter.doFilterInternal(wrapped, responseMock, new FilterChain() {
            @Override
            void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                // Read the body to ensure it is cached
                req.getInputStream().readAllBytes()
            }
        })

        then:
        noExceptionThrown()
    }
}
