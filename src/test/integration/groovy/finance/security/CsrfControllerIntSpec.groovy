package finance.security

import finance.BaseRestTemplateIntegrationSpec
import org.springframework.http.*
import org.springframework.transaction.annotation.Transactional

/**
 * Integration tests for CSRF token endpoint.
 * Verifies that clients can fetch CSRF tokens and that tokens are properly set in cookies.
 */
@Transactional
class CsrfControllerIntSpec extends BaseRestTemplateIntegrationSpec {

    void '/api/csrf endpoint returns CSRF token without authentication'() {
        given: 'CSRF endpoint URL'
        def url = "${baseUrl}/api/csrf"

        when: 'fetching CSRF token'
        def headers = new HttpHeaders()
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        def entity = new HttpEntity<>(headers)
        def response = restTemplate.exchange(url, HttpMethod.GET, entity, Map)

        then: 'returns 200 OK'
        response.statusCode == HttpStatus.OK

        and: 'response contains token details'
        response.body.token != null
        response.body.headerName == 'X-XSRF-TOKEN'
        response.body.parameterName == '_csrf'

        and: 'XSRF-TOKEN cookie is set'
        def cookies = response.headers.get(HttpHeaders.SET_COOKIE)
        cookies != null
        cookies.any { it.startsWith('XSRF-TOKEN=') }
    }

    void 'CSRF token can be retrieved multiple times'() {
        given: 'CSRF endpoint URL'
        def url = "${baseUrl}/api/csrf"
        def headers = new HttpHeaders()
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        def entity = new HttpEntity<>(headers)

        when: 'fetching token first time'
        def response1 = restTemplate.exchange(url, HttpMethod.GET, entity, Map)

        then: 'first request succeeds'
        response1.statusCode == HttpStatus.OK
        response1.body.token != null

        when: 'fetching token second time'
        def response2 = restTemplate.exchange(url, HttpMethod.GET, entity, Map)

        then: 'second request also succeeds'
        response2.statusCode == HttpStatus.OK
        response2.body.token != null

        and: 'tokens may be different (Spring rotates tokens for security)'
        // Note: Tokens may be the same or different depending on session state
        response2.body.token != null
    }

    void 'CSRF endpoint returns JSON content type'() {
        given: 'CSRF endpoint URL'
        def url = "${baseUrl}/api/csrf"
        def headers = new HttpHeaders()
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        def entity = new HttpEntity<>(headers)

        when: 'fetching CSRF token'
        def response = restTemplate.exchange(url, HttpMethod.GET, entity, Map)

        then: 'returns JSON content type'
        response.statusCode == HttpStatus.OK
        def contentType = response.headers.getContentType()
        contentType.includes(MediaType.APPLICATION_JSON)
    }
}
