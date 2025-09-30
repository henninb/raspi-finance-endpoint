package finance

import finance.Application
import finance.helpers.TestDataManager
import finance.helpers.TestFixtures
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.spockframework.spring.EnableSharedInjection
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

@Slf4j
@ActiveProfiles("int")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@EnableSharedInjection
@Transactional
class BaseIntegrationSpec extends Specification {

    @AutoCleanup
    finance.helpers.SecurityContextCleaner securityContextCleaner = new finance.helpers.SecurityContextCleaner()

    @Shared
    @Autowired
    protected TestDataManager testDataManager

    @Shared
    @Autowired
    protected TestFixtures testFixtures

    @Shared
    protected String testOwner = "test_${UUID.randomUUID().toString().replace('-', '')[0..8]}"

    def setupSpec() {
        log.info("Setting up integration test environment for test owner: ${testOwner}")
        testDataManager.initializeIntegrationTestEnvironment(testOwner)
    }

    def cleanupSpec() {
        log.info("Cleaning up integration test data for test owner: ${testOwner}")
        testDataManager.cleanupIntegrationTestsFor(testOwner)
    }

    protected String getPrimaryAccountName() {
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = "testowner"
        return "primary_${cleanOwner}"
    }

    protected String getSecondaryAccountName() {
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = "testowner"
        return "secondary_${cleanOwner}"
    }

    protected String getTestCategory() {
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = "testowner"
        return "test_category_${cleanOwner}"
    }

    /**
     * Helper to set an authenticated security context with USER role.
     */
    protected void withUserRole(String username = "test-user", List<String> roles = ["USER"]) {
        def authorities = roles.collect { new SimpleGrantedAuthority(it) }
        def auth = new UsernamePasswordAuthenticationToken(username, "N/A", authorities)
        SecurityContextHolder.getContext().setAuthentication(auth)
    }
}
