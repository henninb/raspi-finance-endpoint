package finance.services

import finance.Application
import finance.domain.TransactionState
import groovy.util.logging.Log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
