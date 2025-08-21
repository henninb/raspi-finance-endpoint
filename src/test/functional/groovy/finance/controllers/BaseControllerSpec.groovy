package finance.controllers

import finance.Application
import finance.helpers.TestDataManager
import finance.helpers.TestFixtures
import groovy.util.logging.Slf4j
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import javax.crypto.SecretKey
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort

//import org.springframework.boot.context.embedded.LocalServerPort
//import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import org.spockframework.spring.EnableSharedInjection
import spock.lang.Shared
import spock.lang.Specification

@Slf4j
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@EnableSharedInjection
@Transactional
class BaseControllerSpec extends Specification {
    @LocalServerPort
    //@Value("\${local.server.port}")
    //@Value("${local.server.port}")
    //@Value('${local.server.port}')
    //@Value("\${server.port}")
    protected int port

    @Shared
    @Autowired
    protected TestDataManager testDataManager

    @Shared
    @Autowired
    protected TestFixtures testFixtures

    @Shared
    protected String testOwner = "test_${UUID.randomUUID().toString().replace('-', '')[0..7]}"

    protected String username = "foo"

    @Value('${custom.project.jwt.key}')
    protected String jwtKey

    protected TestRestTemplate restTemplate = new TestRestTemplate()

    protected HttpHeaders headers = new HttpHeaders()

    protected String generateJwtToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.bytes)
        return Jwts.builder()
                .claim("username", username)
                .signWith(key)
                .compact()
    }

    protected String createURLWithPort(String uri) {
        return "http://localhost:${port}" + uri
    }

    protected ResponseEntity<String> insertEndpoint(String endpointName, String payload) {
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(payload, headers)

        log.info(payload)

        return restTemplate.exchange(
                "http://localhost:${port}/api/${endpointName}/insert", HttpMethod.POST, entity, String)
    }

    protected ResponseEntity<String> selectEndpoint(String endpointName, String parameter) {
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        log.info("http://localhost:${port}/api/${endpointName}/select/${parameter}")

        return restTemplate.exchange(
                "http://localhost:${port}/api/${endpointName}/select/${parameter}", HttpMethod.GET, entity, String)
    }

    protected ResponseEntity<String> deleteEndpoint(String endpointName, String parameter) {
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        log.info("http://localhost:${port}/api/${endpointName}/delete/${parameter}")

        return restTemplate.exchange(
                "http://localhost:${port}/api/${endpointName}/delete/${parameter}", HttpMethod.DELETE, entity, String)
    }

    def setupSpec() {
        log.info("Setting up test data for test owner: ${testOwner}")
        testDataManager.createMinimalAccountsFor(testOwner)
    }

    def cleanupSpec() {
        log.info("Cleaning up test data for test owner: ${testOwner}")
        testDataManager.cleanupAccountsFor(testOwner)
    }

    def setup() {
        // Create fresh headers for each test to avoid cross-test pollution
        headers = new HttpHeaders()
    }

    protected String getPrimaryAccountName() {
        return "primary_${testOwner}"
    }

    protected String getSecondaryAccountName() {
        return "secondary_${testOwner}"
    }

    protected String getTestCategory() {
        return "test_category_${testOwner}"
    }
}
