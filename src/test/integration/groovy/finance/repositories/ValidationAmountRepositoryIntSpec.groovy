package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.AccountType
import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.helpers.SmartValidationAmountBuilder
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.math.BigDecimal
import java.sql.Timestamp

/**
 * INTEGRATION TEST - ValidationAmountRepository using robust, isolated architecture
 *
 * Demonstrates the new integration test patterns:
 * - Unique, isolated data via testOwner
 * - SmartBuilder with proactive constraint validation
 * - Relationship-aware setup using RepositoryTestContext/TestDataManager
 */
class ValidationAmountRepositoryIntSpec extends BaseIntegrationSpec {

    @Autowired
    ValidationAmountRepository validationAmountRepository

    @Shared
    def repositoryContext

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)
    }

    void 'test validation amount basic CRUD operations'() {
        given:
        Long accountId = repositoryContext.createTestAccount("validation", AccountType.Debit)
        ValidationAmount va = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .withValidationDate(new Timestamp(System.currentTimeMillis()))
                .asCleared()
                .withAmount(new BigDecimal("100.50"))
                .buildAndValidate()

        when:
        ValidationAmount saved = validationAmountRepository.save(va)

        then:
        saved.validationId != null
        saved.accountId == accountId
        saved.transactionState == TransactionState.Cleared
        saved.amount == new BigDecimal("100.50")
        saved.activeStatus == true
        saved.dateAdded != null
        saved.dateUpdated != null

        when:
        def foundById = validationAmountRepository.findById(saved.validationId)
        def foundForAccount = validationAmountRepository.findByAccountId(accountId)

        then:
        foundById.isPresent()
        foundById.get().validationId == saved.validationId
        foundForAccount*.validationId.contains(saved.validationId)
    }

    void 'test find by account id returns all for account'() {
        given:
        Long accountId = repositoryContext.createTestAccount("va_all", AccountType.Debit)
        def va1 = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .withAmount(new BigDecimal("10.00"))
                .buildAndValidate()
        def va2 = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asOutstanding()
                .withAmount(new BigDecimal("20.00"))
                .buildAndValidate()
        def va3 = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asFuture()
                .withAmount(new BigDecimal("30.00"))
                .buildAndValidate()

        def s1 = validationAmountRepository.save(va1)
        def s2 = validationAmountRepository.save(va2)
        def s3 = validationAmountRepository.save(va3)

        when:
        List<ValidationAmount> allForAccount = validationAmountRepository.findByAccountId(accountId)

        then:
        allForAccount.size() >= 3
        allForAccount*.validationId.containsAll([s1.validationId, s2.validationId, s3.validationId])
        allForAccount.every { it.accountId == accountId }
    }

    void 'test find by transaction state and account id'() {
        given:
        Long accountId = repositoryContext.createTestAccount("va_state", AccountType.Debit)
        def cleared = validationAmountRepository.save(
                SmartValidationAmountBuilder.builderForOwner(testOwner)
                        .withAccountId(accountId)
                        .asCleared()
                        .withAmount(new BigDecimal("5.00"))
                        .buildAndValidate()
        )
        def outstanding = validationAmountRepository.save(
                SmartValidationAmountBuilder.builderForOwner(testOwner)
                        .withAccountId(accountId)
                        .asOutstanding()
                        .withAmount(new BigDecimal("6.00"))
                        .buildAndValidate()
        )
        def future = validationAmountRepository.save(
                SmartValidationAmountBuilder.builderForOwner(testOwner)
                        .withAccountId(accountId)
                        .asFuture()
                        .withAmount(new BigDecimal("7.00"))
                        .buildAndValidate()
        )

        when:
        def clearedList = validationAmountRepository.findByTransactionStateAndAccountId(TransactionState.Cleared, accountId)
        def outstandingList = validationAmountRepository.findByTransactionStateAndAccountId(TransactionState.Outstanding, accountId)
        def futureList = validationAmountRepository.findByTransactionStateAndAccountId(TransactionState.Future, accountId)

        then:
        clearedList.size() >= 1 && clearedList*.validationId.contains(cleared.validationId)
        outstandingList.size() >= 1 && outstandingList*.validationId.contains(outstanding.validationId)
        futureList.size() >= 1 && futureList*.validationId.contains(future.validationId)
    }

    void 'test validation amount constraint validation at build time'() {
        given:
        Long accountId = repositoryContext.createTestAccount("va_constraints", AccountType.Debit)

        when: 'amount has more than 2 decimal places'
        SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .withAmount(new BigDecimal("10.123"))
                .buildAndValidate()

        then:
        def ex1 = thrown(IllegalStateException)
        ex1.message.contains('at most 2 decimal places')

        when: 'amount exceeds allowed precision'
        SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .withAmount(new BigDecimal("100000000.00"))
                .buildAndValidate()

        then:
        def ex2 = thrown(IllegalStateException)
        ex2.message.contains('exceeds allowed precision')

        when: 'invalid account id'
        SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(-1L)
                .withAmount(new BigDecimal("1.00"))
                .buildAndValidate()

        then:
        def ex3 = thrown(IllegalStateException)
        ex3.message.contains('accountId must be >= 0')

        when: 'null amount'
        SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .withAmount(null)
                .buildAndValidate()

        then:
        def ex4 = thrown(IllegalStateException)
        ex4.message.contains('amount must not be null')
    }

    void 'test update and deletion of validation amount'() {
        given:
        Long accountId = repositoryContext.createTestAccount("va_update", AccountType.Debit)
        def va = validationAmountRepository.save(
                SmartValidationAmountBuilder.builderForOwner(testOwner)
                        .withAccountId(accountId)
                        .asOutstanding()
                        .withAmount(new BigDecimal("50.00"))
                        .buildAndValidate()
        )

        when: 'update fields'
        va.amount = new BigDecimal('75.25')
        va.transactionState = TransactionState.Cleared
        va.activeStatus = false
        def updated = validationAmountRepository.save(va)

        then:
        updated.validationId == va.validationId
        updated.amount == new BigDecimal('75.25')
        updated.transactionState == TransactionState.Cleared
        updated.activeStatus == false

        when: 'delete and verify removal'
        validationAmountRepository.delete(updated)
        def byId = validationAmountRepository.findById(updated.validationId)
        def byAccount = validationAmountRepository.findByAccountId(accountId)

        then:
        !byId.isPresent()
        !byAccount*.validationId.contains(updated.validationId)
    }
}

