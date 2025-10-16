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
import org.springframework.core.env.Environment
import org.springframework.web.client.RestTemplate
import org.springframework.http.client.SimpleClientHttpRequestFactory
import java.io.IOException
import java.lang.reflect.Field
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.HttpURLConnection
import org.flywaydb.core.Flyway

//import org.springframework.boot.context.embedded.LocalServerPort
//import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import org.springframework.context.annotation.Import
import finance.config.TestSecurityConfig
import org.spockframework.spring.EnableSharedInjection
import spock.lang.Shared
import spock.lang.Specification
import java.util.Date

@Slf4j
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@EnableSharedInjection
@Transactional
@Import([TestSecurityConfig])
class BaseControllerFunctionalSpec extends Specification {
    @Autowired
    protected WebTestClient webTestClient

    @Shared
    @Autowired
    protected Environment environment

    @Shared
    protected RestTemplate restTemplate = createPatchSupportedRestTemplate()

    protected int port
    protected String baseUrl
    protected int managementPort
    protected String managementBaseUrl

    @Shared
    @Autowired
    protected TestDataManager testDataManager

    @Shared
    @Autowired
    protected TestFixtures testFixtures

    @Shared
    protected String testOwner = "test_${UUID.randomUUID().toString().replace('-', '')[0..7]}"

    @Shared
    protected String username = "foo"

    @Value('${custom.project.jwt.key}')
    protected String jwtKey


    protected HttpHeaders headers = new HttpHeaders()
    @Shared
    protected String authCookie

    private static RestTemplate createPatchSupportedRestTemplate() {
        RestTemplate restTemplate = new RestTemplate()

        // Create a custom request factory that enables PATCH by reflection
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected HttpURLConnection openConnection(URL url, java.net.Proxy proxy) throws IOException {
                HttpURLConnection connection = super.openConnection(url, proxy)
                // Enable all HTTP methods by reflection workaround
                try {
                    Field methodsField = HttpURLConnection.class.getDeclaredField("methods")
                    methodsField.setAccessible(true)
                    String[] methods = (String[]) methodsField.get(connection)
                    String[] newMethods = ["GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE", "PATCH"] as String[]
                    methodsField.set(connection, newMethods)
                } catch (Exception ex) {
                    log.warn("Failed to enable PATCH method via reflection: ${ex.message}")
                }
                return connection
            }
        }
        restTemplate.setRequestFactory(requestFactory)
        return restTemplate
    }

    protected String generateJwtToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.bytes)
        long now = System.currentTimeMillis()
        return Jwts.builder()
                .claim("username", username)
                .notBefore(new Date(now))
                .expiration(new Date(now + 3600000)) // 1 hour expiration
                .signWith(key)
                .compact()
    }


    protected ResponseEntity<String> insertEndpoint(String endpointName, String payload) {
        String token = generateJwtToken(username)
        log.info(payload)

        HttpHeaders reqHeaders = new HttpHeaders()
        reqHeaders.setContentType(MediaType.APPLICATION_JSON)
        reqHeaders.add("Cookie", authCookie ?: ("token=" + token))
        reqHeaders.add("Authorization", "Bearer " + token)
        HttpEntity<String> entity = new HttpEntity<>(payload, reqHeaders)

        try {
            return restTemplate.exchange(
                    baseUrl + "/api/${endpointName}",
                    HttpMethod.POST,
                    entity,
                    String
            )
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }
    }

    protected ResponseEntity<String> selectEndpoint(String endpointName, String parameter) {
        String token = generateJwtToken(username)
        log.info("/api/${endpointName}/select/${parameter}")

        HttpHeaders reqHeaders = new HttpHeaders()
        reqHeaders.add("Cookie", authCookie ?: ("token=" + token))
        reqHeaders.add("Authorization", "Bearer " + token)
        HttpEntity<Void> entity = new HttpEntity<>(reqHeaders)

        try {
            return restTemplate.exchange(
                    baseUrl + "/api/${endpointName}/select/${parameter}",
                    HttpMethod.GET,
                    entity,
                    String
            )
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }
    }

    protected ResponseEntity<String> deleteEndpoint(String endpointName, String parameter) {
        String token = generateJwtToken(username)
        log.info("/api/${endpointName}/delete/${parameter}")

        HttpHeaders reqHeaders = new HttpHeaders()
        reqHeaders.add("Cookie", authCookie ?: ("token=" + token))
        reqHeaders.add("Authorization", "Bearer " + token)
        HttpEntity<Void> entity = new HttpEntity<>(reqHeaders)

        try {
            return restTemplate.exchange(
                    baseUrl + "/api/${endpointName}/delete/${parameter}",
                    HttpMethod.DELETE,
                    entity,
                    String
            )
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }
    }

    private ResponseEntity<String> convertWebTestClientResponse(WebTestClient.ResponseSpec responseSpec) {
        def result = responseSpec.returnResult(String.class)
        def status = HttpStatus.valueOf(result.status.value())
        def body = result.responseBodyContent ? new String(result.responseBodyContent) : null
        def headers = result.responseHeaders
        return new ResponseEntity<String>(body, headers, status)
    }

    def setupSpec() {
        log.info("Setting up test data for test owner: ${testOwner}")
        // Configure RestTemplate to not throw exceptions on non-2xx
        restTemplate.setErrorHandler(new org.springframework.web.client.ResponseErrorHandler() {
                        boolean hasError(org.springframework.http.client.ClientHttpResponse response) { return false }
                        void handleError(org.springframework.http.client.ClientHttpResponse response) { /* no-op */ }
        })
        try {
            flyway?.migrate()
        } catch (Exception e) {
            log.warn("Flyway migration call in functional tests: ${e.message}")
        }
        testDataManager.createMinimalAccountsFor(testOwner)

        // Register a functional test user to obtain a real JWT cookie
        try {
            int p = environment.getProperty("local.server.port", Integer.class, 8080)
            String base = "http://localhost:${p}"
            String rnd = UUID.randomUUID().toString().replace('-', '').substring(0, 8)
            String regUsername = "func_" + rnd
            String regPassword = "Passw0rd!" + rnd
            Map<String, Object> user = [
                firstName: "Test",
                lastName : "User",
                username : regUsername,
                password : regPassword,
                activeStatus: true
            ]
            HttpHeaders h = new HttpHeaders()
            h.setContentType(MediaType.APPLICATION_JSON)
            def entity = new HttpEntity<>(user, h)
            ResponseEntity<String> resp = restTemplate.postForEntity(base + "/api/register", entity, String)
            if (resp.statusCode.is2xxSuccessful() || resp.statusCode.value() == 201) {
                List<String> setCookies = resp.headers.get("Set-Cookie")
                if (setCookies && !setCookies.isEmpty()) {
                    // Find the token cookie among all Set-Cookie values
                    String tokenCookie = setCookies.find { it.startsWith("token=") || it.contains("; token=") } ?: setCookies.find { it.contains("token=") }
                    if (tokenCookie) {
                        String tokenPair = tokenCookie.split(';')[0]
                        if (tokenPair.startsWith("token=")) {
                            authCookie = tokenPair
                            username = regUsername
                            log.info("Functional auth cookie initialized for user=${regUsername}")
                        }
                    } else {
                        log.warn("Register returned Set-Cookie headers but no token cookie present: ${setCookies}")
                    }
                } else {
                    log.warn("Register returned no Set-Cookie headers; status=${resp.statusCode}")
                }
            } else {
                log.warn("Functional register did not return 2xx: status=${resp.statusCode}")
            }
        } catch (Exception e) {
            log.warn("Functional register failed: ${e.message}")
        }
    }

    def cleanupSpec() {
        log.info("Cleaning up test data for test owner: ${testOwner}")
        testDataManager.cleanupAccountsFor(testOwner)
    }

    def setup() {
        // Resolve ports and base URLs for functional tests
        port = environment.getProperty("local.server.port", Integer.class, 8080)
        baseUrl = "http://localhost:${port}"
        managementPort = environment.getProperty("local.management.port", Integer.class, port)
        managementBaseUrl = "http://localhost:${managementPort}"
        log.info("Functional baseUrl=${baseUrl}, managementBaseUrl=${managementBaseUrl}")

        // Create fresh headers for each test and attach auth by default
        headers = new HttpHeaders()
        try {
            String token = generateJwtToken(username)
            if (!authCookie) {
                authCookie = "token=" + token
            }
            headers.add("Cookie", authCookie)
            headers.add("Authorization", "Bearer " + token)
        } catch (Exception e) {
            log.warn("Could not initialize default auth headers: ${e.message}")
        }
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
    @Shared
    @Autowired(required = false)
    protected Flyway flyway

    protected String createURLWithPort(String uri) {
        return baseUrl + uri
    }
}
