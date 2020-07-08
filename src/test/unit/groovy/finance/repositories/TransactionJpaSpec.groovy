package finance.repositories

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.domain.Transaction
import finance.helpers.AccountBuilder
import finance.helpers.TransactionBuilder
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.dao.EmptyResultDataAccessException
import spock.lang.Ignore
import spock.lang.Specification

import javax.persistence.PersistenceException
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

    private ObjectMapper mapper = new ObjectMapper()

    def jsonPayload = '''
{"accountId":0,
"accountType":"credit",
"transactionDate":1553645394,
"dateUpdated":1593981072000,
"dateAdded":1593981072000,
"guid":"4ea3be58-3993-46de-88a2-4ffc7f1d73bd",
"accountNameOwner":"chase_brian",
"description":"aliexpress.com",
"category":"online",
"amount":3.14,
"cleared":1,
"reoccurring":false,
"notes":"my note to you",
"sha256":"963e35c37ea59f3f6fa35d72fb0ba47e1e1523fae867eeeb7ead64b55ff22b77"}
'''

    def "test Transaction to JSON - valid insert"() {

        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction.class)
        Account account = new AccountBuilder().build()
        def accountResult = entityManager.persist(account)
        transactionFromString.accountId = accountResult.accountId
        when:
        entityManager.persist(transactionFromString)
        then:
        transactionRepository.count() == 1L
    }

    @Ignore
    def "test Transaction to JSON - attempt to insert same record twice - different uuid"() {

        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction.class)
        Account account = new AccountBuilder().build()
        def accountResult = entityManager.persist(account)
        transactionFromString.accountId = accountResult.accountId
        entityManager.persist(transactionFromString)
        transactionFromString.guid = "4ea3be58-aaaa-cccc-bbbb-4ffc7f1d73bd"
        when:
        entityManager.persist(transactionFromString)
        then:
        transactionRepository.count() == 2L
    }

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

    def "test transaction repository - insert 2 records with same guid - throws an exception."() {
        given:
        Transaction transaction1 = new TransactionBuilder().build()
        Transaction transaction2 = new TransactionBuilder().build()
        transaction2.category = ""
        transaction2.description = "my-description-data"
        transaction2.notes = "my-notes"

        Account account = new Account()
        account.accountNameOwner = transaction1.accountNameOwner
        def accountResult = entityManager.persist(account)
        transaction1.accountId = accountResult.accountId
        transaction2.accountId = accountResult.accountId
        def transactionResult = entityManager.persist(transaction1)
        when:
        entityManager.persist(transaction2)

        then:
        //JdbcSQLIntegrityConstraintViolationException ex = thrown()
        PersistenceException ex = thrown()
        println 'ex.getMessage(): ' + ex.getMessage()
        ex.getMessage().contains('ConstraintViolationException: could not execute statement')
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

    def "test transaction repository - getTotalsByAccountNameOwner - empty"() {
        when:
        transactionRepository.getTotalsByAccountNameOwner("some_account")

        then:
        EmptyResultDataAccessException ex = thrown()
        ex.getMessage().contains('Result must not be null!')
        0 * _
    }

    def "test transaction repository - getTotalsByAccountNameOwnerCleared - empty"() {
        when:
        transactionRepository.getTotalsByAccountNameOwnerCleared("some_account")

        then:
        EmptyResultDataAccessException ex = thrown()
        ex.getMessage().contains('Result must not be null!')
        0 * _
    }

    def "test transaction repository - setClearedByGuid - not in the database"() {
        when:
        transactionRepository.setClearedByGuid(1, "guid-does-not-exist")

        then:
        0 * _
    }
}








