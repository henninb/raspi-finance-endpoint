package finance.controllers

import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.DefaultCsrfToken
import spock.lang.Specification
import spock.lang.Subject

class CsrfControllerSpec extends Specification {

    @Subject
    CsrfController controller = new CsrfController()

    def "getCsrfToken returns map with token details"() {
        given:
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token-value-abc123")

        when:
        Map<String, String> result = controller.getCsrfToken(token)

        then:
        result != null
        result.size() == 3
        result["token"] == "test-token-value-abc123"
        result["headerName"] == "X-CSRF-TOKEN"
        result["parameterName"] == "_csrf"
    }

    def "getCsrfToken returns correct header name"() {
        given:
        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "some-token")

        when:
        Map<String, String> result = controller.getCsrfToken(token)

        then:
        result["headerName"] == "X-XSRF-TOKEN"
        result["parameterName"] == "_csrf"
        result["token"] == "some-token"
    }

    def "getCsrfToken returns map with all expected keys"() {
        given:
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "abc-def-123")

        when:
        Map<String, String> result = controller.getCsrfToken(token)

        then:
        result.containsKey("token")
        result.containsKey("headerName")
        result.containsKey("parameterName")
    }

    def "getCsrfToken token value is passed through unchanged"() {
        given:
        String tokenValue = "eyJhbGciOiJIUzI1NiJ9.abc.signature"
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", tokenValue)

        when:
        Map<String, String> result = controller.getCsrfToken(token)

        then:
        result["token"] == tokenValue
    }
}
