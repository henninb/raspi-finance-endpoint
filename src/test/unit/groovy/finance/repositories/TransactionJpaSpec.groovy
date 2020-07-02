package finance.repositories

import finance.domain.Account
import finance.domain.Transaction
import finance.helpers.AccountBuilder
import finance.helpers.TransactionBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import spock.lang.Specification

import javax.validation.ConstraintViolationException
import java.sql.Timestamp

@DataJpaTest
class TransactionJpaSpec extends Specification {

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    AccountRepository accountRepository

    @Autowired
    TestEntityManager entityManager

    def "test transaction repository - insert a valid record"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()
        println "transaction = $transaction"
        println "account = $account"

        def accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        println "accountResult = $accountResult"
        def transactionResult = entityManager.persist(transaction)
        println "transactionResult = $transactionResult"
        def foundTransactionOptional = transactionRepository.findByGuid(transaction.guid)
        def categories = transactionRepository.selectFromTransactionCategories(foundTransactionOptional.get().transactionId)
        println "categories = $categories"
        //categories.get(0)

        expect:
        transactionRepository.count() == 1L
        accountRepository.count() == 1L
        foundTransactionOptional.get().guid == transaction.guid


        //foundTransactionOptional.get().categories.size() == 1
    }

    def "test transaction repository - attempt to insert a transaction with a category with too many characters"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()

        when:
        def accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        transaction.category = "123451234512345123451234512345123451234512345123451234512345"
        entityManager.persist(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('size must be between 0 and 50')
        0 * _
    }

    def "test transaction repository - attempt to insert a transaction with a cleared status out of range"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()

        when:
        def accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        transaction.cleared = 3
        entityManager.persist(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('must be less than or equal to 1')
        0 * _
    }

    def "test transaction repository - attempt to insert a transaction with an invalid guid"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()

        when:
        def accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        transaction.guid = "123"
        entityManager.persist(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('must be uuid formatted')
        0 * _
    }

    def "test transaction repository - attempt to insert a transaction with an invalid dateAdded"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()

        when:
        def accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        transaction.dateAdded = new Timestamp(123456)
        println "transaction.dateAdded = $transaction.dateAdded"
        entityManager.persist(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('timestamp must be greater than 1/1/2000.')
        0 * _
    }

    def "test transaction repository - attempt to insert a transaction with an invalid dateUpdated"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()

        when:
        def accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        transaction.dateUpdated = new Timestamp(123456)
        println "transaction.dateUpdated = $transaction.dateUpdated"
        entityManager.persist(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('timestamp must be greater than 1/1/2000.')
        0 * _
    }

    def "test transaction repository - delete record"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()

        def accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        entityManager.persist(transaction)
        transactionRepository.deleteByGuid(transaction.guid)

        expect:
        transactionRepository.count() == 0L
        accountRepository.count() == 1L
    }
}







