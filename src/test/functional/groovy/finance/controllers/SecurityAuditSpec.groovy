package finance.controllers

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import finance.helpers.AccountBuilder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
@ActiveProfiles("func")
class SecurityAuditSpec extends BaseControllerSpec {

    @Shared
    ListAppender<ILoggingEvent> securityLogAppender

    @Shared
    Logger securityLogger

    def setupSpec() {
        securityLogger = (Logger) LoggerFactory.getLogger("SECURITY.finance.controllers.AccountController")
        securityLogAppender = new ListAppender<>()
        securityLogAppender.start()
        securityLogger.addAppender(securityLogAppender)
        securityLogger.setLevel(Level.INFO)
    }

    def cleanupSpec() {
        securityLogger.detachAppender(securityLogAppender)
    }

    void 'should reject access to /select/active endpoints without authentication'() {
        when: 'accessing account select/active endpoint without authentication'
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/account/select/active"),
                HttpMethod.GET,
                null,
                String
        )

        then: 'should be forbidden due to missing authentication'
        response.statusCode == HttpStatus.FORBIDDEN || response.statusCode == HttpStatus.UNAUTHORIZED

        and: 'response should not contain sensitive data'
        !response.body?.contains("accountNameOwner")
        !response.body?.contains("totals")
        !response.body?.contains("balance")
    }

    void 'should allow access to /select/active endpoints with valid authentication'() {
        given: 'insert test account for the test'
        def account = AccountBuilder.builder().withAccountNameOwner('audit-test_account').build()  // Pattern compliant: ^[a-z-]*_[a-z]*$
        insertEndpoint('account', account.toString())

        when: 'accessing account select/active endpoint with authentication'
        ResponseEntity<String> response = selectActiveEndpoint('account')

        then: 'should be successful'
        response.statusCode == HttpStatus.OK

        and: 'response should contain account data'
        response.body?.contains("accountNameOwner")
        response.body?.contains("accountType")
    }

    void 'should reject access to category endpoints without authentication'() {
        when: 'accessing category select/active endpoint without authentication'
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/category/select/active"),
                HttpMethod.GET,
                null,
                String
        )

        then: 'should be forbidden'
        response.statusCode == HttpStatus.FORBIDDEN || response.statusCode == HttpStatus.UNAUTHORIZED

        and: 'should not contain sensitive category data'
        !response.body?.contains("categoryName")
        !response.body?.contains("activeStatus")
    }

    protected ResponseEntity<String> selectActiveEndpoint(String endpointName) {
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        def entity = new org.springframework.http.HttpEntity<>(null, headers)

        return restTemplate.exchange(
                createURLWithPort("/api/${endpointName}/select/active"),
                HttpMethod.GET,
                entity,
                String
        )
    }
}