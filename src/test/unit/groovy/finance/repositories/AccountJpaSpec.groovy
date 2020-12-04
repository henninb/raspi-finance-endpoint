package finance.repositories

import finance.domain.Account
import finance.helpers.AccountBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

import javax.validation.ConstraintViolationException

@ActiveProfiles("unit")
@DataJpaTest
class AccountJpaSpec extends Specification {
    @Autowired
    protected AccountRepository accountRepository

    @Autowired
    protected TestEntityManager entityManager

    void 'test account repository - computeTheGrandTotalForAllTransactions - empty'() {
        when:
        Double result = accountRepository.computeTheGrandTotalForAllTransactions()

        then:
        result == 0.0
        0 * _
    }

    void 'test account repository - computeTheGrandTotalForAllClearedTransactions - empty'() {
        when:
        Double result = accountRepository.computeTheGrandTotalForAllClearedTransactions()

        then:
        0.0 == result
        0 * _
    }

    void 'test account - valid insert'() {
        given:
        Account account = new AccountBuilder().build()

        when:
        Account accountResult = entityManager.persist(account)

        then:
        accountRepository.count() == 1L
        accountResult.accountNameOwner == account.accountNameOwner
    }

    void 'test account - valid insert - 2 of the same does the update on the first record'() {
        given:
        Account account = new AccountBuilder().build()
        entityManager.persist(account)
        account.moniker = '9999'

        when:
        Account accountResult = entityManager.persist(account)

        then:
        accountRepository.count() == 1L
        '9999' == accountResult.moniker
        accountResult.accountNameOwner == account.accountNameOwner
    }

    void 'test account - invalid moniker'() {
        given:
        Account account = new AccountBuilder().build()
        account.moniker = 'invalid'

        when:
        entityManager.persist(account)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.contains('Validation failed for classes [finance.domain.Account]')
    }

    void 'test account - invalid accountNameOwner'() {
        given:
        Account account = new AccountBuilder().build()
        account.accountNameOwner = 'invalid'

        when:
        entityManager.persist(account)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.contains('Validation failed for classes [finance.domain.Account]')
    }
}
