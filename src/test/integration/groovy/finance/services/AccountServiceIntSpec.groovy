package finance.services

import finance.Application
import finance.domain.TransactionState
import groovy.util.logging.Log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Specification

@Log
@ActiveProfiles("int")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountServiceIntSpec extends Specification {

    @Autowired
    AccountService accountService

    @Ignore
    void 'computeTheGrandTotalForAllTransactions'() {
        when:
        accountService.sumOfAllTransactionsByTransactionState(TransactionState.Cleared)

        then:
        noExceptionThrown()
        0 * _
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
