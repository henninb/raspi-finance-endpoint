package finance.repositories

import com.fasterxml.jackson.databind.ObjectMapper
import finance.Application
import finance.domain.Account
import finance.domain.Transaction
import finance.helpers.AccountBuilder
import finance.helpers.TransactionBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import jakarta.persistence.PersistenceException
import jakarta.validation.ConstraintViolationException

@ActiveProfiles("func")
@DataJpaTest
@ContextConfiguration(classes = [Application])
class TransactionJpaSpec extends Specification {

    @Autowired
    protected TransactionRepository transactionRepository

    @Autowired
    protected AccountRepository accountRepository

    @Autowired
    protected TestEntityManager entityManager

    protected ObjectMapper mapper = new ObjectMapper()

    protected String jsonPayload = '''
{"accountId":0,
"accountType":"credit",
"transactionType":"expense",
"transactionDate":"2020-12-02",
"guid":"4ea3be58-3993-46de-88a2-4ffc7f1d73bb",
"accountNameOwner":"chase_brian",
"description":"aliexpress.com",
"category":"online",
"amount":3.14,
"transactionState":"cleared",
"reoccurringType":"undefined",
"notes":"my note to you"}
'''

    void 'test Transaction to JSON - valid insert'() {
        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction)
        Account account = new AccountBuilder().withAccountNameOwner('testa_brian').build()
        Account accountResult = entityManager.persist(account)
        transactionFromString.accountId = accountResult.accountId
        transactionFromString.accountNameOwner = accountResult.accountNameOwner

        when:
        Transaction result = entityManager.persist(transactionFromString)

        then:
        transactionRepository.count() == 6L
        accountRepository.count() == 6L
        result.guid == transactionFromString.guid
    }

    void 'test Transaction to JSON - attempt to insert same record twice - different uuid'() {
        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction)
        Account account = new AccountBuilder().withAccountNameOwner('testb_brian').build()
        Account res = entityManager.persist(account)
        transactionFromString.accountId = res.accountId
        transactionFromString.accountNameOwner = res.accountNameOwner
        entityManager.persist(transactionFromString)
        Transaction transactionFromString2 = mapper.readValue(jsonPayload, Transaction)
        transactionFromString2.accountId = account.accountId
        transactionFromString2.accountNameOwner = account.accountNameOwner
        transactionFromString2.guid = '3ea3be58-aaaa-cccc-bbbb-4ffc7f1d73b1'
        transactionFromString2.description = 'different_description'

        when:
        entityManager.persist(transactionFromString2)

        then:
        transactionRepository.count() == 7L
    }

    void 'test Transaction to JSON - attempt to insert same record twice - different guid'() {
        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction)
        transactionFromString.category = 'online'
        Transaction transaction1 = TransactionBuilder.builder().build()
        Account account = new AccountBuilder().withAccountNameOwner('testc_brian').build()
        Account res = entityManager.persist(account)
        transaction1.accountId = res.accountId
        transaction1.accountNameOwner = res.accountNameOwner
        transactionFromString.accountId = res.accountId
        transactionFromString.accountNameOwner = res.accountNameOwner
        Transaction first = entityManager.persist(transactionFromString)
        transaction1.guid = '3ea3be58-aaaa-cccc-bbbb-4ffc7f1d73bd'

        when:
        Transaction sec = entityManager.persist(transaction1)

        then:
        first.guid == transactionFromString.guid
        sec.guid == transaction1.guid
        transactionRepository.count() == 7L
    }

    void 'test transaction repository - insert a valid record'() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().withAccountNameOwner('testd_brian').build()
        Account accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        transaction.accountNameOwner = accountResult.accountNameOwner

        when:
        Transaction transactionResult = entityManager.persist(transaction)

        then:
        transactionRepository.count() == 6L
        accountRepository.count() == 6L
        transactionRepository.findByGuid(transaction.guid).get().guid == transaction.guid
        transactionResult.guid == transaction.guid
    }

    void 'test transaction repository - insert 2 records with duplicate guid - throws an exception'() {
        given:
        String duplicateGuid = '11111111-2222-3333-4444-555555555555'
        Transaction transaction1 = new TransactionBuilder().withGuid(duplicateGuid).build()
        Transaction transaction2 = new TransactionBuilder().withGuid(duplicateGuid).build()
        transaction2.category = 'online'
        transaction2.description = 'my-description-data'
        transaction2.notes = 'my-notes'

        Account account = new AccountBuilder().withAccountNameOwner('teste_brian').build()
        Account accountResult = entityManager.persist(account)
        transaction1.accountId = accountResult.accountId
        transaction1.accountNameOwner = accountResult.accountNameOwner
        transaction2.accountId = accountResult.accountId
        transaction2.accountNameOwner = accountResult.accountNameOwner
        entityManager.persist(transaction1)

        when:
        entityManager.persist(transaction2)

        then:
        Exception ex = thrown()
        ex.message.contains('duplicate') || ex.message.contains('unique') || ex.message.contains('constraint') || ex.message.contains('Unique index')
    }

    void 'test transaction repository - attempt to insert a transaction with a category with too many characters'() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().withAccountNameOwner('testf_brian').build()
        Account accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        transaction.accountNameOwner = accountResult.accountNameOwner
        transaction.category = '123451234512345123451234512345123451234512345123451234512345'

        when:
        entityManager.persist(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.contains('size must be between 0 and 50')
        0 * _
    }

    void 'test transaction repository - attempt to insert a transaction with an invalid guid'() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().withAccountNameOwner('testg_brian').build()
        Account accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        transaction.accountNameOwner = accountResult.accountNameOwner
        transaction.guid = '123'

        when:
        entityManager.persist(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.contains('must be uuid formatted')
        0 * _
    }

    void 'test transaction repository - delete record'() {
        given:
        Transaction transaction = new TransactionBuilder().build()
        Account account = new AccountBuilder().withAccountNameOwner('testh_brian').build()

        Account accountResult = entityManager.persist(account)
        transaction.accountId = accountResult.accountId
        transaction.accountNameOwner = accountResult.accountNameOwner
        entityManager.persist(transaction)

        when:
        transactionRepository.delete(transaction)

        then:
        transactionRepository.count() == 5L
        accountRepository.count() == 6L
    }
}
