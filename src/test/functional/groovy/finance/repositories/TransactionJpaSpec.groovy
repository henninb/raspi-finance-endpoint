package finance.repositories

import finance.Application
import finance.domain.Account
import finance.domain.Transaction
import finance.helpers.SmartAccountBuilder
import finance.helpers.TransactionBuilder
import finance.helpers.SmartCategoryBuilder
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
    @Autowired
    protected CategoryRepository categoryRepository

    def setup() {
        ensureCategoryExists('online')
    }

    private void ensureCategoryExists(String name) {
        if (!categoryRepository.findByCategoryName(name).isPresent()) {
            def cat = SmartCategoryBuilder.builderForOwner('brian')
                .withCategoryName(name)
                .asActive()
                .buildAndValidate()
            entityManager.persist(cat)
        }
    }

    void 'test Transaction to JSON - valid insert'() {
        given:
        long beforeTx = transactionRepository.count()
        long beforeAcct = accountRepository.count()
        Account account = SmartAccountBuilder.builderForOwner('brian')
            .withUniqueAccountName('txa')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        Transaction transactionFromBuilder = TransactionBuilder.builder()
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('online')
            .build()

        when:
        Transaction result = entityManager.persist(transactionFromBuilder)

        then:
        transactionRepository.count() == beforeTx + 1
        accountRepository.count() == beforeAcct + 1
        result.guid == transactionFromBuilder.guid
    }

    void 'test Transaction to JSON - attempt to insert same record twice - different uuid and description'() {
        given:
        long beforeTx = transactionRepository.count()
        Account account = SmartAccountBuilder.builderForOwner('brian')
            .withUniqueAccountName('txb')
            .asCredit()
            .buildAndValidate()
        Account res = entityManager.persist(account)
        Transaction t1 = TransactionBuilder.builder()
            .withAccountId(res.accountId)
            .withAccountNameOwner(res.accountNameOwner)
            .withCategory('online')
            .build()
        entityManager.persist(t1)
        Transaction t2 = TransactionBuilder.builder()
            .withAccountId(res.accountId)
            .withAccountNameOwner(res.accountNameOwner)
            .withCategory('online')
            .withGuid('3ea3be58-aaaa-cccc-bbbb-4ffc7f1d73b1')
            .withDescription('different_description')
            .build()

        when:
        entityManager.persist(t2)

        then:
        transactionRepository.count() == beforeTx + 2
    }

    void 'test Transaction to JSON - attempt to insert same record twice - different guid and description'() {
        given:
        long beforeTx = transactionRepository.count()
        Account account = SmartAccountBuilder.builderForOwner('brian')
            .withUniqueAccountName('txc')
            .asCredit()
            .buildAndValidate()
        Account res = entityManager.persist(account)
        Transaction tFirst = TransactionBuilder.builder()
            .withAccountId(res.accountId)
            .withAccountNameOwner(res.accountNameOwner)
            .withCategory('online')
            .build()
        Transaction first = entityManager.persist(tFirst)
        Transaction tSecond = TransactionBuilder.builder()
            .withAccountId(res.accountId)
            .withAccountNameOwner(res.accountNameOwner)
            .withCategory('online')
            .withGuid('3ea3be58-aaaa-cccc-bbbb-4ffc7f1d73bd')
            .withDescription('a different description')
            .build()

        when:
        Transaction sec = entityManager.persist(tSecond)

        then:
        first.guid == tFirst.guid
        sec.guid == tSecond.guid
        transactionRepository.count() == beforeTx + 2
    }

    void 'test transaction repository - insert a valid record'() {
        given:
        long beforeTx = transactionRepository.count()
        long beforeAcct = accountRepository.count()
        Account account = SmartAccountBuilder.builderForOwner('brian')
            .withUniqueAccountName('txd')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        Transaction transaction = TransactionBuilder.builder()
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('online')
            .build()

        when:
        Transaction transactionResult = entityManager.persist(transaction)

        then:
        transactionRepository.count() == beforeTx + 1
        accountRepository.count() == beforeAcct + 1
        transactionRepository.findByGuid(transaction.guid).get().guid == transaction.guid
        transactionResult.guid == transaction.guid
    }

    void 'test transaction repository - insert 2 records with duplicate guid - throws an exception'() {
        given:
        String duplicateGuid = '11111111-2222-3333-4444-555555555555'
        Account account = SmartAccountBuilder.builderForOwner('brian')
            .withUniqueAccountName('txe')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        Transaction transaction1 = TransactionBuilder.builder()
            .withGuid(duplicateGuid)
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('online')
            .build()
        Transaction transaction2 = TransactionBuilder.builder()
            .withGuid(duplicateGuid)
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('online')
            .withDescription('my-description-data')
            .withNotes('my-notes')
            .build()
        entityManager.persist(transaction1)

        when:
        entityManager.persist(transaction2)

        then:
        Exception ex = thrown()
        ex.message.contains('duplicate') || ex.message.contains('unique') || ex.message.contains('constraint') || ex.message.contains('Unique index')
    }

    void 'test transaction repository - attempt to insert a transaction with a category with too many characters'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner('brian')
            .withUniqueAccountName('txf')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        Transaction transaction = TransactionBuilder.builder()
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('123451234512345123451234512345123451234512345123451234512345')
            .build()

        when:
        entityManager.persist(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.contains('size must be between 0 and 50')
        0 * _
    }

    void 'test transaction repository - attempt to insert a transaction with an invalid guid'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner('brian')
            .withUniqueAccountName('txg')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        Transaction transaction = TransactionBuilder.builder()
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('online')
            .withGuid('123')
            .build()

        when:
        entityManager.persist(transaction)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.contains('must be uuid formatted')
        0 * _
    }

    void 'test transaction repository - delete record'() {
        given:
        long beforeTx = transactionRepository.count()
        long beforeAcct = accountRepository.count()
        Account account = SmartAccountBuilder.builderForOwner('brian')
            .withUniqueAccountName('txh')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        Transaction transaction = TransactionBuilder.builder()
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('online')
            .build()
        entityManager.persist(transaction)

        when:
        transactionRepository.delete(transaction)

        then:
        transactionRepository.count() == beforeTx
        accountRepository.count() == beforeAcct + 1
    }
}
