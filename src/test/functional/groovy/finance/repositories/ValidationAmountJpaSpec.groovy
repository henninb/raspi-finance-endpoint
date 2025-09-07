package finance.repositories

import finance.Application
import finance.domain.Account
import finance.domain.ValidationAmount
import finance.domain.TransactionState
import finance.helpers.SmartAccountBuilder
import finance.helpers.SmartValidationAmountBuilder
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
class ValidationAmountJpaSpec extends Specification {

    @Autowired
    protected ValidationAmountRepository validationAmountRepository

    @Autowired
    protected AccountRepository accountRepository

    @Autowired
    protected TestEntityManager entityManager

    @Shared
    protected String testOwner = "jpa_${UUID.randomUUID().toString().replace('-', '')[0..7]}"

    void 'test ValidationAmount - valid insert'() {
        given:
        long beforeValidationAmount = validationAmountRepository.count()
        long beforeAccount = accountRepository.count()
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('valtest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        ValidationAmount validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAmount(150.75G)
            .asCleared()
            .buildAndValidate()

        when:
        ValidationAmount result = entityManager.persist(validationAmount)

        then:
        validationAmountRepository.count() == beforeValidationAmount + 1
        accountRepository.count() == beforeAccount + 1
        result.amount == validationAmount.amount
        result.accountId == validationAmount.accountId
    }

    void 'test ValidationAmount - different transaction states'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('valstatetest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)

        ValidationAmount clearedAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAmount(100.00G)
            .asCleared()
            .buildAndValidate()

        ValidationAmount outstandingAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAmount(200.00G)
            .asOutstanding()
            .buildAndValidate()

        when:
        ValidationAmount clearedResult = entityManager.persist(clearedAmount)
        ValidationAmount outstandingResult = entityManager.persist(outstandingAmount)

        then:
        clearedResult.transactionState == TransactionState.Cleared
        outstandingResult.transactionState == TransactionState.Outstanding
        clearedResult.amount.compareTo(100.00G) == 0
        outstandingResult.amount.compareTo(200.00G) == 0
    }

    void 'test ValidationAmount - different active status'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('valactivetest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)

        ValidationAmount activeAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAmount(75.25G)
            .asActive()
            .asCleared()
            .buildAndValidate()

        ValidationAmount inactiveAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAmount(125.50G)
            .asInactive()
            .asCleared()
            .buildAndValidate()

        when:
        ValidationAmount activeResult = entityManager.persist(activeAmount)
        ValidationAmount inactiveResult = entityManager.persist(inactiveAmount)

        then:
        activeResult.activeStatus == true
        inactiveResult.activeStatus == false
        activeResult.amount.compareTo(75.25G) == 0
        inactiveResult.amount.compareTo(125.50G) == 0
    }

    void 'test ValidationAmount - delete record'() {
        given:
        long beforeValidationAmount = validationAmountRepository.count()
        long beforeAccount = accountRepository.count()
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('valdeletetest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        ValidationAmount validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAmount(99.99G)
            .asCleared()
            .buildAndValidate()
        entityManager.persist(validationAmount)

        when:
        validationAmountRepository.delete(validationAmount)

        then:
        validationAmountRepository.count() == beforeValidationAmount
        accountRepository.count() == beforeAccount + 1
    }

    void 'test ValidationAmount - invalid amount precision'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('valprecisiontest')
            .asCredit()
            .buildAndValidate()
        Account accountResult = entityManager.persist(account)
        ValidationAmount validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountResult.accountId)
            .withAmount(25.123456G)  // Too many decimal places
            .asCleared()
            .build()  // Use build() instead of buildAndValidate() to allow constraint violation

        when:
        entityManager.persist(validationAmount)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.toLowerCase().contains('dollar') || ex.message.toLowerCase().contains('currency')
        0 * _
    }

    void 'test ValidationAmount - invalid FK constraint with non-existent account'() {
        given:
        ValidationAmount validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(99999L)  // Non-existent account
            .withAmount(50.00G)
            .asCleared()
            .buildAndValidate()

        when:
        entityManager.persist(validationAmount)
        entityManager.flush()  // Force constraint validation in Hibernate 7.1.0

        then:
        PersistenceException ex = thrown()
        ex.message.contains('constraint') || ex.message.contains('foreign key')
        0 * _
    }
}
