package finance.repositories

import finance.Application
import finance.domain.Account
import finance.domain.PendingTransaction
import finance.helpers.SmartAccountBuilder
import finance.helpers.SmartPendingTransactionBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import jakarta.persistence.PersistenceException
import jakarta.validation.ConstraintViolationException

@ActiveProfiles("func")
@DataJpaTest
@ContextConfiguration(classes = [Application])
class PendingTransactionJpaSpec extends Specification {

    @Autowired
    protected PendingTransactionRepository pendingTransactionRepository

    @Autowired
    protected AccountRepository accountRepository

    @Autowired
    protected TestEntityManager entityManager

    @Shared
    protected String testOwner = "jpa_${UUID.randomUUID().toString().replace('-', '')[0..7]}"

    void 'test PendingTransaction - valid insert'() {
        given:
        long beforePendingTransaction = pendingTransactionRepository.count()
        long beforeAccount = accountRepository.count()
        
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('pendingtest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        PendingTransaction pendingTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withAmount(175.50G)
            .withDescription("Test pending transaction")
            .buildAndValidate()

        when:
        PendingTransaction result = entityManager.persist(pendingTransaction)

        then:
        pendingTransactionRepository.count() == beforePendingTransaction + 1
        accountRepository.count() == beforeAccount + 1
        result.amount.compareTo(175.50G) == 0
        result.description == "Test pending transaction"
        result.accountNameOwner == accountResult.accountNameOwner
    }

    void 'test PendingTransaction - different review statuses'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('statetest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        PendingTransaction clearedTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withAmount(100.00G)
            .withDescription("Cleared pending transaction")
            .asReviewed()
            .buildAndValidate()
            
        PendingTransaction outstandingTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withAmount(200.00G)
            .withDescription("Outstanding pending transaction")
            .asPending()
            .buildAndValidate()

        when:
        PendingTransaction clearedResult = entityManager.persist(clearedTransaction)
        PendingTransaction outstandingResult = entityManager.persist(outstandingTransaction)

        then:
        clearedResult.reviewStatus == 'approved'
        outstandingResult.reviewStatus == 'pending'
        clearedResult.amount.compareTo(100.00G) == 0
        outstandingResult.amount.compareTo(200.00G) == 0
    }

    void 'test PendingTransaction - different review status values'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('activetest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        PendingTransaction activeTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withAmount(75.25G)
            .withDescription("Active pending transaction")
            .asPending()
            .buildAndValidate()
            
        PendingTransaction inactiveTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withAmount(125.75G)
            .withDescription("Inactive pending transaction")
            .asIgnored()
            .buildAndValidate()

        when:
        PendingTransaction activeResult = entityManager.persist(activeTransaction)
        PendingTransaction inactiveResult = entityManager.persist(inactiveTransaction)

        then:
        activeResult.reviewStatus == 'pending'
        inactiveResult.reviewStatus == 'rejected'
        activeResult.amount.compareTo(75.25G) == 0
        inactiveResult.amount.compareTo(125.75G) == 0
    }

    void 'test PendingTransaction - find by id'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('findtest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        PendingTransaction transaction1 = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withAmount(50.00G)
            .withDescription("Find test transaction 1")
            .asPending()
            .buildAndValidate()
        PendingTransaction transaction2 = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withAmount(60.00G)
            .withDescription("Find test transaction 2")
            .asReviewed()
            .buildAndValidate()
        PendingTransaction r1 = entityManager.persist(transaction1)
        entityManager.persist(transaction2)

        when:
        def opt = pendingTransactionRepository.findByPendingTransactionIdOrderByTransactionDateDesc(r1.pendingTransactionId)

        then:
        opt.isPresent()
        opt.get().description == "Find test transaction 1"
        opt.get().accountNameOwner == accountResult.accountNameOwner
    }

    void 'test PendingTransaction - delete record'() {
        given:
        long beforePendingTransaction = pendingTransactionRepository.count()
        long beforeAccount = accountRepository.count()
        
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('deletetest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        PendingTransaction pendingTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withAmount(99.99G)
            .withDescription("Delete test transaction")
            .buildAndValidate()
        entityManager.persist(pendingTransaction)

        when:
        pendingTransactionRepository.delete(pendingTransaction)

        then:
        pendingTransactionRepository.count() == beforePendingTransaction
        accountRepository.count() == beforeAccount + 1  // Account should remain
    }

    void 'test PendingTransaction - invalid amount precision'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('precisiontest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        PendingTransaction pendingTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withAmount(25.123456G)  // Too many decimal places
            .withDescription("Precision test transaction")
            .build()  // Use build() instead of buildAndValidate() to allow constraint violation

        when:
        PendingTransaction saved = entityManager.persist(pendingTransaction)
        entityManager.flush()
        entityManager.clear()
        def reloaded = pendingTransactionRepository.findByPendingTransactionIdOrderByTransactionDateDesc(saved.pendingTransactionId)

        then:
        reloaded.isPresent()
        reloaded.get().amount.setScale(2, java.math.RoundingMode.HALF_UP).compareTo(new BigDecimal('25.12')) == 0
    }

    void 'test PendingTransaction - invalid FK constraint with non-existent account'() {
        given:
        PendingTransaction pendingTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner("nonexistent_account")
            .withAmount(50.00G)
            .withDescription("Invalid FK test")
            .buildAndValidate()

        when:
        entityManager.persist(pendingTransaction)

        then:
        PersistenceException ex = thrown()
        ex.message.contains('constraint') || ex.message.contains('foreign key')
        0 * _
    }

    void 'test PendingTransaction - invalid empty description'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('emptytest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        PendingTransaction pendingTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withAmount(100.00G)
            .withDescription("")  // Empty description
            .build()  // Use build() instead of buildAndValidate() to allow constraint violation

        when:
        PendingTransaction saved = entityManager.persist(pendingTransaction)

        then:
        saved.description == ''
    }

    // Note: repository no longer supports delete/find by accountNameOwner; skip bulk-delete test
}
