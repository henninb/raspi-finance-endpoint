package finance.repositories

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.helpers.AccountBuilder
import finance.helpers.TransactionBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

import javax.persistence.PersistenceException
import javax.validation.ConstraintViolationException

@ActiveProfiles("unit")
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
"guid":"4ea3be58-3993-46de-88a2-4ffc7f1d73bb",
"accountNameOwner":"chase_brian",
"description":"aliexpress.com",
"category":"online",
"amount":3.14,
"transactionState":"cleared",
"reoccurring":false,
"notes":"my note to you"}
'''

    def "test Transaction to JSON - valid insert"() {
        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction.class)
        Account account = new AccountBuilder().build()
        def accountResult = entityManager.persist(account)
        transactionFromString.accountId = accountResult.accountId

        when:
        def result = entityManager.persist(transactionFromString)

        then:
        transactionRepository.count() == 1L
        accountRepository.count() == 1L
        result.guid == transactionFromString.guid
    }

    def "test Transaction to JSON - attempt to insert same record twice - different uuid"() {
        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction.class)
        Account account = new AccountBuilder().builder().build()

        def res = entityManager.persist(account)
        transactionFromString.accountId = res.accountId
        transactionFromString.accountNameOwner = res.accountNameOwner
        entityManager.persist(transactionFromString)
        def transactionFromString2 = mapper.readValue(jsonPayload, Transaction.class)
        transactionFromString2.accountId = account.accountId
        transactionFromString2.accountNameOwner = account.accountNameOwner
        transactionFromString2.guid = '3ea3be58-aaaa-cccc-bbbb-4ffc7f1d73b1'

        when:
        entityManager.persist(transactionFromString2)

        then:
        transactionRepository.count() == 2L
    }

    def "test Transaction to JSON - attempt to insert same record twice - different guid"() {
        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction.class)
        transactionFromString.category = ''
        Transaction transaction1 = TransactionBuilder.builder().build()
        Account account = new AccountBuilder().accountNameOwner('trash_id').build()
        def res = entityManager.persist(account)
        transaction1.accountId = res.accountId
        transaction1.accountNameOwner = res.accountNameOwner
        transactionFromString.accountId = res.accountId
        transactionFromString.accountNameOwner = res.accountNameOwner
        def first = entityManager.persist(transactionFromString)
        transaction1.guid = '3ea3be58-aaaa-cccc-bbbb-4ffc7f1d73bd'

        when:
        def sec = entityManager.persist(transaction1)

        then:
        first.guid == transactionFromString.guid
        sec.guid == transaction1.guid
        transactionRepository.count() == 2L
    }

    def "test transaction repository - insert a valid record"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()
        def accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId

        when:
        def transactionResult = entityManager.persist(transaction)

        then:
        transactionRepository.count() == 1L
        accountRepository.count() == 1L
        transactionRepository.findByGuid(transaction.guid).get().guid == transaction.guid
        transactionResult.guid == transaction.guid
    }

    def "test transaction repository - insert 2 records with duplicate guid - throws an exception."() {
        given:
        Transaction transaction1 = new TransactionBuilder().build()
        Transaction transaction2 = new TransactionBuilder().build()
        transaction2.category = ''
        transaction2.description = 'my-description-data'
        transaction2.notes = 'my-notes'

        Account account = new Account()
        account.accountNameOwner = transaction1.accountNameOwner
        def accountResult = entityManager.persist(account)
        transaction1.accountId = accountResult.accountId
        transaction2.accountId = accountResult.accountId
        entityManager.persist(transaction1)

        when:
        entityManager.persist(transaction2)

        then:
        PersistenceException ex = thrown()
        ex.getMessage().contains('ConstraintViolationException: could not execute statement')
    }

    def "test transaction repository - attempt to insert a transaction with a category with too many characters"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()
        def accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        transaction.category = '123451234512345123451234512345123451234512345123451234512345'

        when:
        entityManager.persist(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('size must be between 0 and 50')
        0 * _
    }

    def "test transaction repository - attempt to insert a transaction with an invalid guid"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()
        def accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        transaction.guid = '123'

        when:
        entityManager.persist(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.getMessage().contains('must be uuid formatted')
        0 * _
    }

    def "test transaction repository - delete record"() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().build()

        def accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        entityManager.persist(transaction)

        when:
        transactionRepository.deleteByGuid(transaction.guid)

        then:
        transactionRepository.count() == 0L
        accountRepository.count() == 1L
    }

    def "test transaction repository - getTotalsByAccountNameOwner - empty"() {
        when:
        Double result = transactionRepository.getTotalsByAccountNameOwner('refa')

        then:
        0.0 == result
        0 * _
    }

    def "test transaction repository - getTotalsByAccountNameOwnerCleared - empty"() {
        when:
        Double result = transactionRepository.getTotalsByAccountNameOwnerTransactionState('some_account')

        then:
        0.0 == result
        0 * _
    }

    def "test transaction repository - setClearedByGuid - not in the database"() {
        when:
        transactionRepository.setTransactionStateByGuid(TransactionState.Cleared, 'guid-does-not-exist')

        then:
        0 * _
    }
}








