package finance.repositories

import finance.domain.Account
import finance.domain.Transaction
import finance.helpers.AccountBuilder
import finance.helpers.TransactionBuilder

//import finance.repositories.TransactionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import spock.lang.Specification

import javax.validation.ConstraintViolation
import javax.validation.ConstraintViolationException
import java.sql.Timestamp

@DataJpaTest
class JpaTransactionSpec extends Specification {

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    AccountRepository accountRepository

    @Autowired
    TestEntityManager entityManager

    def "jpa test for transaction repository"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()
        println "transaction = $transaction"
        println "account = $account"

        def accountResult = entityManager.merge(account)
        transaction.accountId = accountResult.accountId
        println "accountResult = $accountResult"
        def transactionResult = entityManager.merge(transaction)
        println "transactionResult = $transactionResult"

        expect:
        transactionRepository.count() == 1L
        accountRepository.count() == 1L
    }

    def "jpa test for transaction repository - long category"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()

        when:
        def accountResult = entityManager.merge(account)
        transaction.accountId = accountResult.accountId
        transaction.category = "123451234512345123451234512345123451234512345123451234512345"
        entityManager.merge(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('size must be between 0 and 50')
        0 * _
    }

    def "jpa test for transaction repository - clearedStatus out of range"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()

        when:
        def accountResult = entityManager.merge(account)
        transaction.accountId = accountResult.accountId
        transaction.cleared = 3
        entityManager.merge(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('must be less than or equal to 1')
        0 * _
    }

    def "jpa test for transaction repository - invalid guid"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()

        when:
        def accountResult = entityManager.merge(account)
        transaction.accountId = accountResult.accountId
        transaction.guid = "123"
        entityManager.merge(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('must be uuid formatted')
        0 * _
    }

    def "jpa test for transaction repository - invalid dateAdded"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()

        when:
        def accountResult = entityManager.merge(account)
        transaction.accountId = accountResult.accountId
        transaction.dateAdded = new Timestamp(123456)
        println "transaction.dateAdded = $transaction.dateAdded"
        entityManager.merge(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains( 'timestamp must be greater than 1/1/2000.')
        0 * _
    }

    def "jpa test for transaction repository - invalid dateUpdated"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()

        when:
        def accountResult = entityManager.merge(account)
        transaction.accountId = accountResult.accountId
        transaction.dateUpdated = new Timestamp(123456)
        println "transaction.dateUpdated = $transaction.dateUpdated"
        entityManager.merge(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains( 'timestamp must be greater than 1/1/2000.')
        0 * _
    }

}








