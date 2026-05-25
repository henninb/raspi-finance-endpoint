package finance.controllers

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort

//import org.springframework.boot.context.embedded.LocalServerPort
//import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import spock.lang.Shared
import spock.lang.Specification

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@SpringBootTest(webEnvironment = RANDOM_PORT)
class BaseControllerSpec extends Specification {
    @LocalServerPort
    //@Value("\${local.server.port}")
    //@Value("${local.server.port}")
    //@Value('${local.server.port}')
    //@Value("\${server.port}")
    protected int port
    protected String username = "henninb"
    protected String password = "monday1"

    protected TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    protected HttpHeaders headers = new HttpHeaders()

    protected String createURLWithPort(String uri) {
        return "http://localhost:${port}" + uri
    }

    protected ResponseEntity<String> insertEndpoint(String endpointName, String payload) {
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.setBasicAuth(username, password)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        log.info(payload)

        return restTemplate.exchange(
                "http://localhost:${port}/${endpointName}/insert/", HttpMethod.POST, entity, String)
    }

    protected ResponseEntity<String> selectEndpoint(String endpointName, String parameter) {
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.setBasicAuth(username, password)
        HttpEntity entity = new HttpEntity<>(null, headers)

        log.info("http://localhost:${port}/${endpointName}/select/${parameter}")

        return restTemplate.exchange(
                "http://localhost:${port}/${endpointName}/select/${parameter}", HttpMethod.GET, entity, String)
    }

    protected ResponseEntity<String> deleteEndpoint(String endpointName, String parameter) {
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.setBasicAuth(username, password)
        HttpEntity entity = new HttpEntity<>(null, headers)

        log.info("http://localhost:${port}/${endpointName}/delete/${parameter}")

        return restTemplate.exchange(
                "http://localhost:${port}/${endpointName}/delete/${parameter}", HttpMethod.DELETE, entity, String)
    }
}
