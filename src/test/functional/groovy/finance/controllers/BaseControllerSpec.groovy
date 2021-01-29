package finance.controllers

import groovy.util.logging.Slf4j
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import spock.lang.Shared
import spock.lang.Specification

@Slf4j
class BaseControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    protected TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    protected HttpHeaders headers = new HttpHeaders()

    protected String createURLWithPort(String uri) {
        return "http://localhost:${port}" + uri
    }

    protected ResponseEntity<String> insertEndpoint(String endpointName, String payload) {
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        log.info(payload)

        return restTemplate.exchange(
                "http://localhost:${port}/${endpointName}/insert/", HttpMethod.POST, entity, String)
    }

    protected ResponseEntity<String> selectEndpoint(String endpointName, String parameter) {
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(null, headers)

        log.info("http://localhost:${port}/${endpointName}/select/${parameter}")

        return restTemplate.exchange(
                "http://localhost:${port}/${endpointName}/select/${parameter}", HttpMethod.GET, entity, String)
    }

    protected ResponseEntity<String> deleteEndpoint(String endpointName, String parameter) {
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(null, headers)

        log.info("http://localhost:${port}/${endpointName}/delete/${parameter}")

        return restTemplate.exchange(
                "http://localhost:${port}/${endpointName}/delete/${parameter}", HttpMethod.DELETE, entity, String)
    }
}
