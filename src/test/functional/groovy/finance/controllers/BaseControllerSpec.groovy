package finance.controllers

import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
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
        return "http://localhost:" + port + uri
    }
}
