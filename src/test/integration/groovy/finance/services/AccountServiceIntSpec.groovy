package finance.services

import finance.Application
import finance.domain.TransactionState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Specification

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
}
