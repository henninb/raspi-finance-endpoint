package finance

import finance.Application
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.client.RestTemplate
import org.springframework.core.env.Environment
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.beans.factory.annotation.Value
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import javax.crypto.SecretKey
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification
import spock.lang.Shared
import spock.lang.AutoCleanup

@Slf4j
@ActiveProfiles("int")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@TestPropertySource(properties = [
    "server.port=0",
    "management.server.port=0",
    "management.endpoints.web.exposure.include=*",
    "management.influx.metrics.export.enabled=false",
    "management.endpoint.health.show-details=always"
])
class BaseRestTemplateIntegrationSpec extends Specification {

    @AutoCleanup
    finance.helpers.SecurityContextCleaner securityContextCleaner = new finance.helpers.SecurityContextCleaner()

    @Autowired
    protected Environment environment

    @Shared
    protected RestTemplate restTemplate = new RestTemplate()

    protected int port
    protected String baseUrl
    protected int managementPort
    protected String managementBaseUrl

    @Value('\${custom.project.jwt.key:abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789}')
    protected String jwtKey

    def setupSpec() {
        log.info("Initializing RestTemplate for Spring Boot 4.0 integration tests")
    }

    def setup() {
        // Get the actual port from Spring Boot 4.0 environment
        port = environment.getProperty("local.server.port", Integer.class, 8080)
        baseUrl = "http://localhost:${port}"
        log.info("Configured RestTemplate for base URL: ${baseUrl}")

        // When management runs on a separate port (set to 0 → random), expose it
        managementPort = environment.getProperty("local.management.port", Integer.class, port)
        managementBaseUrl = "http://localhost:${managementPort}"
        log.info("Configured management base URL: ${managementBaseUrl}")

        // Give server time to be fully ready
        Thread.sleep(200)
    }

    def cleanup() {
        log.debug("Test completed for port ${port}")
    }

    /**
     * Helper method for making GET requests with retry logic
     */
    protected ResponseEntity<String> getWithRetry(String uri, int maxAttempts = 3) {
        Exception lastException = null

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    log.info("Retry attempt ${attempt} for ${uri}")
                    Thread.sleep(200 * attempt) // Progressive delay
                }

                ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + uri, String.class)
                log.debug("GET ${uri} completed with status: ${response.statusCode}")
                return response

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
    protected ResponseEntity<String> postWithRetry(String uri, Object body, int maxAttempts = 3) {
        Exception lastException = null

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    log.info("Retry attempt ${attempt} for POST ${uri}")
                    Thread.sleep(200 * attempt) // Progressive delay
                }

                HttpHeaders headers = new HttpHeaders()
                headers.setContentType(MediaType.APPLICATION_JSON)
                HttpEntity<Object> entity = new HttpEntity<>(body, headers)

                ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + uri, entity, String.class)
                log.debug("POST ${uri} completed with status: ${response.statusCode}")
                return response

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
     * Create a short-lived JWT for tests using configured signing key.
     */
    protected String createJwtToken(String username = 'test-user', List<String> authorities = ['USER']) {
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.getBytes())
        java.util.Date issuedAt = new java.util.Date()
        java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + 60_000) // 1 min
        return Jwts.builder()
            .claim('username', username)
            .claim('authorities', authorities)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(key)
            .compact()
    }

    /**
     * POST helper with Authorization: Bearer token and retries.
     */
    protected ResponseEntity<String> postWithRetryAuth(String uri, Object body, String token, int maxAttempts = 3) {
        Exception lastException = null
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    log.info("Retry attempt ${attempt} for POST ${uri} (auth)")
                    Thread.sleep(200 * attempt)
                }
                HttpHeaders headers = new HttpHeaders()
                headers.setContentType(MediaType.APPLICATION_JSON)
                headers.set('Authorization', 'Bearer ' + token)
                HttpEntity<Object> entity = new HttpEntity<>(body, headers)
                ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + uri, entity, String.class)
                log.debug("POST ${uri} (auth) completed with status: ${response.statusCode}")
                return response
            } catch (Exception e) {
                lastException = e
                log.warn("POST attempt ${attempt} (auth) failed for ${uri}: ${e.message}")
                if (attempt >= maxAttempts) throw e
            }
        }
        throw lastException
    }

    /**
     * Helper method to check server connectivity
     */
    protected boolean isServerReady() {
        try {
            ResponseEntity<String> response = getWithRetry("/actuator/health", 1)
            return response.statusCode.is2xxSuccessful()
        } catch (Exception e) {
            log.debug("Server not ready: ${e.message}")
            return false
        }
    }

    /**
     * Helper method for making GET requests to the management port with retry logic
     */
    protected ResponseEntity<String> getMgmtWithRetry(String uri, int maxAttempts = 3) {
        Exception lastException = null

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    log.info("Retry attempt ${attempt} for mgmt ${uri}")
                    Thread.sleep(200 * attempt) // Progressive delay
                }

                ResponseEntity<String> response = restTemplate.getForEntity(managementBaseUrl + uri, String.class)
                log.debug("GET (mgmt) ${uri} completed with status: ${response.statusCode}")
                return response

            } catch (Exception e) {
                lastException = e
                log.warn("Mgmt attempt ${attempt} failed for ${uri}: ${e.message}")
                if (attempt >= maxAttempts) {
                    throw e
                }
            }
        }
        throw lastException
    }

    /**
     * Helper to set an authenticated security context with USER role.
     */
    protected void withUserRole(String username = 'test-user', List<String> roles = ['USER']) {
        def authorities = roles.collect { role -> new SimpleGrantedAuthority(role) }
        def auth = new UsernamePasswordAuthenticationToken(username, 'N/A', authorities)
        SecurityContextHolder.getContext().setAuthentication(auth)
    }
}
