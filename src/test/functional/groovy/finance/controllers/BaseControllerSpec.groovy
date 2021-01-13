package finance.controllers

import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import spock.lang.Shared
import spock.lang.Specification

class BaseControllerSpec extends Specification {
    @LocalServerPort
    protected int port

    protected TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    protected HttpHeaders headers

    protected void setup() {
        headers = new HttpHeaders()
    }

    protected String createURLWithPort(String uri) {
        return "http://localhost:${port}" + uri
    }

    protected ResponseEntity<String> insertEndpoint(String endpointName, String payload) {
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        return restTemplate.exchange(
                "http://localhost:${port}/${endpointName}/insert/", HttpMethod.POST, entity, String)
    }
}
