package finance

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ActiveProfiles("int")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
class HealthEndpointSpec extends Specification {

    @LocalServerPort
    int port

    protected TestRestTemplate restTemplate = new TestRestTemplate("foo", "bar")

    @Shared
    protected HttpHeaders headers = new HttpHeaders()

    protected String createURLWithPort(String uri) {
        return "http://localhost:${port}" + uri
    }

    void 'should return health status when health endpoint is accessed'() {
        when:
        ResponseEntity<String> response = restTemplate.getForEntity(createURLWithPort("/health"), String)

        then:
        port > 0
        response != null
        // Expect 200 since integration test profile permits all requests
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.contains("UP")
    }

    void 'should return health status when actuator health endpoint is accessed'() {
        when:
        ResponseEntity<String> response = restTemplate.getForEntity(createURLWithPort("/actuator/health"), String)

        then:
        port > 0
        response != null
        // Expect 200 since integration test profile permits all requests
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.contains("UP")
    }
}
