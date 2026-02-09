package finance.services

import finance.Application
import finance.domain.TransactionState
import groovy.util.logging.Log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Ignore
import spock.lang.Specification

@Log
@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
class AccountServiceIntSpec extends Specification {

    @Autowired
    AccountService accountService

    void setup() {
        def authorities = [new SimpleGrantedAuthority("USER")]
        def auth = new UsernamePasswordAuthenticationToken("test-account-user", "N/A", authorities)
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    void cleanup() {
        SecurityContextHolder.clearContext()
    }

    void 'computeTheGrandTotalForAllTransactions'() {
        when:
        def totalCleared = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Cleared)
        def totalFuture = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Future)
        def totalOutstanding = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Outstanding)

        then:
        noExceptionThrown()
        totalCleared != null
        totalFuture != null
        totalOutstanding != null
        totalCleared.scale() == 2
        totalFuture.scale() == 2
        totalOutstanding.scale() == 2
    }

    void 'computeTheGrandTotalForAllClearedTransactions'() {
        when:
        accountService.sumOfAllTransactionsByTransactionState(TransactionState.Cleared)

        then:
        0 * _
    }

    void 'log4j vulnerability test'() {
        when:
        log.info('\${jndi:ldap://localhost/a}')

        then:
        0 * _
    }
}
