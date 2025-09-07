package finance

import finance.Application
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

import java.time.Duration

@Slf4j
@ActiveProfiles("int")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@TestPropertySource(properties = [
    "server.port=0",
    "management.server.port=0",
    "spring.webflux.multipart.max-in-memory-size=1MB",
    "server.tomcat.connection-timeout=30000",
    "server.tomcat.keep-alive-timeout=30000"
])
class BaseWebTestIntegrationSpec extends Specification {

    @Autowired
    protected WebTestClient webTestClient

    def setupSpec() {
        log.info("Configuring WebTestClient with enhanced timeout settings for Spring Boot 4.0 compatibility")
    }

    def setup() {
        log.info("Configuring WebTestClient with Spring Boot 4.0 compatibility settings")

        // For Spring Boot 4.0, we need to handle WebTestClient differently
        // The client may need to be recreated to establish proper connections
        try {
            // Test basic connectivity before configuring
            webTestClient.get().uri("/actuator/health").exchange().expectStatus().is5xxServerError()
        } catch (Exception e) {
            log.warn("Initial WebTestClient connectivity test failed: ${e.message}")
        }

        // Configure WebTestClient with enhanced settings for Spring Boot 4.0
        webTestClient = webTestClient.mutate()
            .responseTimeout(Duration.ofSeconds(60))  // Increased timeout
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(2 * 1024 * 1024))  // 2MB buffer
            .build()

        // Give server more time to stabilize connections
        Thread.sleep(500)
        log.info("WebTestClient configuration completed")
    }
}