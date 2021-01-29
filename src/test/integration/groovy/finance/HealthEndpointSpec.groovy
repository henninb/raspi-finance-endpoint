package finance

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification

@ActiveProfiles("int")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointSpec extends Specification {

    @LocalServerPort
    protected int port

    protected TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    protected HttpHeaders headers = new HttpHeaders()

    protected String createURLWithPort(String uri) {
        return "http://localhost:${port}" + uri
    }

    void 'test health endpoint'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("/health"), HttpMethod.GET, entity, String)

        then:
        response.statusCode.is(HttpStatus.OK)
        0 * _
    }

    void 'test actuator health endpoint'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("/actuator/health"), HttpMethod.GET, entity, String)

        then:
        response.statusCode.is(HttpStatus.OK)
        0 * _
    }
}
