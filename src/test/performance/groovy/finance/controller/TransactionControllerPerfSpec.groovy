package finance.controller

import com.fasterxml.jackson.databind.ObjectMapper
import finance.Application
import finance.config.TestSecurityConfig
import finance.domain.Account
import finance.domain.AccountType
import finance.helpers.TransactionBuilder
import finance.repositories.AccountRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.spockframework.spring.EnableSharedInjection
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.crypto.SecretKey

@ActiveProfiles("perf")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import([TestSecurityConfig])
@EnableSharedInjection
class TransactionControllerPerf extends Specification {

    @Shared
    @Autowired
    AccountRepository accountRepository

    @Shared
    @Autowired
    Environment environment

    @LocalServerPort
    int port

    @Shared
    Long testAccountId

    @Shared
    String username = "perf_user"

    @Shared
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()

    @Shared
    org.springframework.web.client.RestTemplate restTemplate

    HttpHeaders headers

    def setupSpec() {
        restTemplate = new org.springframework.web.client.RestTemplate()
        restTemplate.errorHandler = new org.springframework.web.client.ResponseErrorHandler() {
            boolean hasError(org.springframework.http.client.ClientHttpResponse r) { false }
            void handleError(org.springframework.http.client.ClientHttpResponse r) {}
        }

        Account account = new Account()
        account.accountNameOwner = "chase_brian"
        account.accountType = AccountType.Credit
        account.owner = username
        Account saved = accountRepository.save(account)
        testAccountId = saved.accountId
    }

    def setup() {
        String jwtKey = environment.getProperty("custom.project.jwt.key")
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.add("Authorization", "Bearer " + generateJwtToken(username, jwtKey))
    }

    private static String generateJwtToken(String user, String jwtKey) {
        SecretKey key = Keys.hmacShaKeyFor(jwtKey.bytes)
        long now = System.currentTimeMillis()
        return Jwts.builder()
            .issuer("raspi-finance-endpoint")
            .audience().add("raspi-finance-endpoint").and()
            .subject(user)
            .claim("username", user)
            .issuedAt(new Date(now))
            .notBefore(new Date(now))
            .expiration(new Date(now + 3600000L))
            .signWith(key)
            .compact()
    }

    private String createURLWithPort(String uri) {
        "http://localhost:${port}${uri}"
    }

    @Unroll
    void "test insertTransaction endpoint"() {
        given:
        def transaction = TransactionBuilder.builder()
            .guid(guid.toString())
            .notes(notes)
            .description(description)
            .accountId(testAccountId)
            .build()

        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(transaction), headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/transaction"), HttpMethod.POST, entity, String.class)

        then:
        response.statusCode == HttpStatus.CREATED

        where:
        notes                  | guid              | description
        randomAlphanumeric(10) | UUID.randomUUID() | randomAlphanumeric(25)
        randomAlphanumeric(10) | UUID.randomUUID() | randomAlphanumeric(25)
        randomAlphanumeric(10) | UUID.randomUUID() | randomAlphanumeric(25)
        randomAlphanumeric(10) | UUID.randomUUID() | randomAlphanumeric(25)
        randomAlphanumeric(10) | UUID.randomUUID() | randomAlphanumeric(25)
        randomAlphanumeric(10) | UUID.randomUUID() | randomAlphanumeric(25)
        randomAlphanumeric(10) | UUID.randomUUID() | randomAlphanumeric(25)
        randomAlphanumeric(10) | UUID.randomUUID() | randomAlphanumeric(25)
        randomAlphanumeric(10) | UUID.randomUUID() | randomAlphanumeric(25)
        randomAlphanumeric(10) | UUID.randomUUID() | randomAlphanumeric(25)
        randomAlphanumeric(10) | UUID.randomUUID() | randomAlphanumeric(25)
    }

    static String randomAlphanumeric(int length) {
        def chars = (('a'..'z') + ('0'..'9'))
        new Random().with { (1..length).collect { chars[nextInt(chars.size())] }.join() }
    }
}
