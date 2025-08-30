package finance.repositories

import finance.Application
import finance.domain.Account
import finance.domain.Transaction
import finance.domain.MedicalExpense
import finance.helpers.SmartAccountBuilder
import finance.helpers.SmartTransactionBuilder
import finance.helpers.SmartMedicalExpenseBuilder
import finance.helpers.SmartCategoryBuilder
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
class MedicalExpenseJpaSpec extends Specification {

    @Autowired
    protected MedicalExpenseRepository medicalExpenseRepository

    @Autowired
    protected TransactionRepository transactionRepository

    @Autowired
    protected AccountRepository accountRepository

    @Autowired
    protected TestEntityManager entityManager

    @Autowired
    protected CategoryRepository categoryRepository

    @Shared
    protected String testOwner = "jpa_${UUID.randomUUID().toString().replace('-', '')[0..7]}"

    private void ensureCategoryExists(String name) {
        if (!categoryRepository.findByCategoryName(name).isPresent()) {
            def cat = SmartCategoryBuilder.builderForOwner(testOwner)
                .withCategoryName(name)
                .asActive()
                .buildAndValidate()
            entityManager.persist(cat)
        }
    }

    void 'test MedicalExpense - valid insert'() {
        given:
        long beforeMedicalExpense = medicalExpenseRepository.count()
        long beforeTransaction = transactionRepository.count()
        long beforeAccount = accountRepository.count()
        
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('medicaltest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        // ensure category FK
        ensureCategoryExists('online')

        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('online')
            .withAmount(125.50G)
            .buildAndValidate()
        Transaction transactionResult = entityManager.persist(transaction)
        
        MedicalExpense medicalExpense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
            .withTransactionId(transactionResult.transactionId)
            .withBilledAmount(new BigDecimal('125.50'))
            .buildAndValidate()

        when:
        MedicalExpense result = entityManager.persist(medicalExpense)

        then:
        medicalExpenseRepository.count() == beforeMedicalExpense + 1
        transactionRepository.count() == beforeTransaction + 1
        accountRepository.count() == beforeAccount + 1
        result.transactionId == transactionResult.transactionId
        result.billedAmount.compareTo(new BigDecimal('125.50')) == 0
    }

    void 'test MedicalExpense - two expenses different amounts'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('providerstest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        ensureCategoryExists('online')
        Transaction transaction1 = SmartTransactionBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('online')
            .withAmount(100.00G)
            .buildAndValidate()
        Transaction transactionResult1 = entityManager.persist(transaction1)
        
        Transaction transaction2 = SmartTransactionBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('online')
            .withAmount(200.00G)
            .buildAndValidate()
        Transaction transactionResult2 = entityManager.persist(transaction2)
        
        MedicalExpense me1 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
            .withTransactionId(transactionResult1.transactionId)
            .withBilledAmount(new BigDecimal('100.00'))
            .buildAndValidate()
            
        MedicalExpense me2 = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
            .withTransactionId(transactionResult2.transactionId)
            .withBilledAmount(new BigDecimal('200.00'))
            .buildAndValidate()

        when:
        MedicalExpense r1 = entityManager.persist(me1)
        MedicalExpense r2 = entityManager.persist(me2)

        then:
        r1.billedAmount.compareTo(new BigDecimal('100.00')) == 0
        r2.billedAmount.compareTo(new BigDecimal('200.00')) == 0
    }

    void 'test MedicalExpense - find by transaction id'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('findtxntest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        ensureCategoryExists('online')
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('online')
            .withAmount(75.25G)
            .buildAndValidate()
        Transaction transactionResult = entityManager.persist(transaction)
        
        MedicalExpense medicalExpense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
            .withTransactionId(transactionResult.transactionId)
            .withBilledAmount(new BigDecimal('75.25'))
            .buildAndValidate()
        entityManager.persist(medicalExpense)

        when:
        def found = medicalExpenseRepository.findByTransactionId(transactionResult.transactionId)

        then:
        found != null
        found.transactionId == transactionResult.transactionId
        found.billedAmount.compareTo(new BigDecimal('75.25')) == 0
    }

    void 'test MedicalExpense - delete record'() {
        given:
        long beforeMedicalExpense = medicalExpenseRepository.count()
        long beforeTransaction = transactionRepository.count()
        
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('deletetest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        ensureCategoryExists('online')
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('online')
            .withAmount(50.00G)
            .buildAndValidate()
        Transaction transactionResult = entityManager.persist(transaction)
        
        MedicalExpense medicalExpense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
            .withTransactionId(transactionResult.transactionId)
            .withBilledAmount(new BigDecimal('50.00'))
            .buildAndValidate()
        entityManager.persist(medicalExpense)

        when:
        medicalExpenseRepository.delete(medicalExpense)

        then:
        medicalExpenseRepository.count() == beforeMedicalExpense
        transactionRepository.count() == beforeTransaction + 1  // Transaction should remain
    }

    void 'test MedicalExpense - invalid FK constraint with non-existent transaction'() {
        given:
        MedicalExpense medicalExpense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
            .withTransactionId(99999L)  // Non-existent transaction
            .withBilledAmount(new BigDecimal('100.00'))
            .buildAndValidate()

        when:
        entityManager.persist(medicalExpense)

        then:
        PersistenceException ex = thrown()
        ex.message.contains('constraint') || ex.message.contains('foreign key')
        0 * _
    }

    void 'test MedicalExpense - invalid amount precision'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('precisiontest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        
        ensureCategoryExists('online')
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAccountNameOwner(accountResult.accountNameOwner)
            .withCategory('online')
            .withAmount(25.12G)
            .buildAndValidate()
        Transaction transactionResult = entityManager.persist(transaction)
        
        MedicalExpense medicalExpense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
            .withTransactionId(transactionResult.transactionId)
            .withBilledAmount(new BigDecimal('25.123456'))  // Too many decimal places
            .build()  // Use build() instead of buildAndValidate() to allow constraint violation

        when:
        entityManager.persist(medicalExpense)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.toLowerCase().contains('decimal') || ex.message.toLowerCase().contains('2')
        0 * _
    }
}
