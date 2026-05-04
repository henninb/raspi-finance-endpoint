package finance.configurations

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import spock.lang.Specification

class LoggingCorsFilterSpec extends Specification {

    def makeFilter(List<String> allowed) {
        CorsConfigurationSource source = Stub(CorsConfigurationSource) {
            getCorsConfiguration(_ as HttpServletRequest) >> new CorsConfiguration().tap {
                setAllowedOrigins(allowed)
            }
        }
        return new LoggingCorsFilter(source)
    }

    def "not-allowed Origin still proceeds with chain"() {
        given:
        def filter = makeFilter(["https://allowed.example"]) // origin not allowed
        def req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/resource")
        req.addHeader("Origin", "https://other.example")
        def res = new org.springframework.mock.web.MockHttpServletResponse()
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        noExceptionThrown()
    }

    def "allowed Origin proceeds with chain"() {
        given:
        def filter = makeFilter(["https://allowed.example"]) // origin allowed
        def req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/resource")
        req.addHeader("Origin", "https://allowed.example")
        def res = new org.springframework.mock.web.MockHttpServletResponse()
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        noExceptionThrown()
    }

    def "no Origin header proceeds with chain without CORS check"() {
        given:
        def filter = makeFilter(["https://allowed.example"])
        def req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/resource")
        // no Origin header added
        def res = new org.springframework.mock.web.MockHttpServletResponse()
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        noExceptionThrown()
    }

    def "null cors configuration proceeds with chain"() {
        given:
        CorsConfigurationSource source = Stub(CorsConfigurationSource) {
            getCorsConfiguration(_ as HttpServletRequest) >> null
        }
        def filter = new LoggingCorsFilter(source)
        def req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/resource")
        req.addHeader("Origin", "https://other.example")
        def res = new org.springframework.mock.web.MockHttpServletResponse()
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        noExceptionThrown()
    }

    def "null allowedOrigins in config proceeds with chain"() {
        given:
        CorsConfigurationSource source = Stub(CorsConfigurationSource) {
            getCorsConfiguration(_ as HttpServletRequest) >> new CorsConfiguration()
            // allowedOrigins is null by default
        }
        def filter = new LoggingCorsFilter(source)
        def req = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/resource")
        req.addHeader("Origin", "https://other.example")
        def res = new org.springframework.mock.web.MockHttpServletResponse()
        def chain = Mock(FilterChain)

        when:
        filter.doFilterInternal(req, res, chain)

        then:
        noExceptionThrown()
    }
}
