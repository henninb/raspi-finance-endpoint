package finance

import finance.Application
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification
import spock.lang.Shared

import java.time.Duration

@Slf4j
@ActiveProfiles("int")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@TestPropertySource(properties = [
    "server.port=0",
    "management.server.port=0"
])
class BaseWebClientIntegrationSpec extends Specification {

    @Autowired
    protected Environment environment

    @Shared
    protected WebClient webClient

    protected int port

    def setupSpec() {
        log.info("Initializing WebClient for Spring Boot 4.0 integration tests")
    }

    def setup() {
        // Get the actual port from Spring Boot 4.0 environment
        port = environment.getProperty("local.server.port", Integer.class, 8080)
        log.info("Configuring WebClient for port ${port}")

        // Configure WebClient with Spring Boot 4.0 optimized settings
        webClient = WebClient.builder()
            .baseUrl("http://localhost:${port}")
            .defaultHeaders(headers -> {
                headers.add("Accept", "application/json")
                headers.add("Content-Type", "application/json")
            })
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024) // 2MB buffer
            })
            .build()

        log.info("WebClient configured successfully for base URL: http://localhost:${port}")

        // Give server time to be fully ready
        Thread.sleep(200)
    }

    def cleanup() {
        // WebClient doesn't require explicit cleanup, but log completion
        log.debug("Test completed for port ${port}")
    }

    /**
     * Helper method for making GET requests with retry logic
     */
    protected String getWithRetry(String uri, int maxAttempts = 3) {
        Exception lastException = null

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    log.info("Retry attempt ${attempt} for ${uri}")
                    Thread.sleep(200 * attempt) // Progressive delay
                }

                return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String)
                    .timeout(Duration.ofSeconds(30))
                    .block()

            } catch (Exception e) {
                lastException = e
                log.warn("Attempt ${attempt} failed for ${uri}: ${e.message}")
                if (attempt >= maxAttempts) {
                    throw e
                }
            }
        }
        throw lastException
    }

    /**
     * Helper method for making POST requests with retry logic
     */
    protected String postWithRetry(String uri, Object body, int maxAttempts = 3) {
        Exception lastException = null

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    log.info("Retry attempt ${attempt} for POST ${uri}")
                    Thread.sleep(200 * attempt) // Progressive delay
                }

                return webClient.post()
                    .uri(uri)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String)
                    .timeout(Duration.ofSeconds(30))
                    .block()

            } catch (Exception e) {
                lastException = e
                log.warn("POST attempt ${attempt} failed for ${uri}: ${e.message}")
                if (attempt >= maxAttempts) {
                    throw e
                }
            }
        }
        throw lastException
    }

    /**
     * Helper method to check server connectivity
     */
    protected boolean isServerReady() {
        try {
            getWithRetry("/actuator/health", 1)
            return true
        } catch (Exception e) {
            log.debug("Server not ready: ${e.message}")
            return false
        }
    }
}