package finance.configurations

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import spock.lang.Specification

import java.util.function.Supplier

class SpaCsrfTokenRequestHandlerSpec extends Specification {

    SpaCsrfTokenRequestHandler handler

    def setup() {
        handler = new SpaCsrfTokenRequestHandler()
    }

    def "handler can be instantiated"() {
        expect:
        handler != null
    }

    def "handle sets request attributes via delegate without exception"() {
        given:
        HttpServletRequest request = Mock()
        HttpServletResponse response = Mock()
        CsrfToken csrfToken = Mock()
        csrfToken.headerName >> "X-CSRF-TOKEN"
        csrfToken.parameterName >> "_csrf"
        csrfToken.token >> "test-csrf-token"
        request.setAttribute(_, _) >> {}
        Supplier<CsrfToken> tokenSupplier = { csrfToken }

        when:
        handler.handle(request, response, tokenSupplier)

        then:
        noExceptionThrown()
        (1.._) * request.setAttribute(_, _)
    }

    def "resolveCsrfTokenValue returns null when no CSRF header in request"() {
        given:
        HttpServletRequest request = Mock()
        CsrfToken csrfToken = Mock()
        csrfToken.headerName >> "X-CSRF-TOKEN"
        csrfToken.parameterName >> "_csrf"
        csrfToken.token >> "actual-token"
        request.getHeader("X-CSRF-TOKEN") >> null
        request.getParameter("_csrf") >> null

        when:
        String result = handler.resolveCsrfTokenValue(request, csrfToken)

        then:
        result == null
    }

    def "resolveCsrfTokenValue delegates to XorCsrfTokenRequestAttributeHandler"() {
        given:
        HttpServletRequest request = Mock()
        CsrfToken csrfToken = Mock()
        csrfToken.headerName >> "X-CSRF-TOKEN"
        csrfToken.parameterName >> "_csrf"
        csrfToken.token >> "base-token"
        request.getHeader("X-CSRF-TOKEN") >> null
        request.getParameter("_csrf") >> null

        when:
        handler.resolveCsrfTokenValue(request, csrfToken)

        then:
        noExceptionThrown()
    }
}
