package finance.repositories

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles("unit")
@DataJpaTest
class AccountJpaSpec extends Specification {

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    AccountRepository accountRepository

    @Autowired
    TestEntityManager entityManager


    def "test account repository - computeTheGrandTotalForAllTransactions - empty"() {
        when:
        def result = accountRepository.computeTheGrandTotalForAllTransactions()

        then:
        0.0 == result
        0 * _
    }

    def "test account repository - computeTheGrandTotalForAllClearedTransactions - empty"() {
        when:
        def result = accountRepository.computeTheGrandTotalForAllClearedTransactions()

        then:
        0.0 == result
        0 * _
    }
}