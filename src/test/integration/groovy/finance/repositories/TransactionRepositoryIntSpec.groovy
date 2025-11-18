package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.helpers.SmartTransactionBuilder
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

/**
 * MIGRATED INTEGRATION TEST - Robust, isolated TransactionRepository tests
 *
 * Uses BaseIntegrationSpec + TestFixtures contexts + SmartTransactionBuilder
 * to avoid brittle hardcoded names and shared state.
 */
class TransactionRepositoryIntSpec extends BaseIntegrationSpec {

    @Autowired
    TransactionRepository transactionRepository

    @Shared
    @Autowired
    AccountRepository accountRepository

    @Shared
    def repositoryContext

    @Shared
    Long primaryAccountId

    @Shared
    String ownerAccountName

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)
        ownerAccountName = repositoryContext.primaryAccountName
        // Ensure account exists using repository; create via SmartBuilder if missing
        Optional<Account> acc = accountRepository.findByAccountNameOwner(ownerAccountName)
        if (!acc.isPresent()) {
            def acct = finance.helpers.SmartAccountBuilder.builderForOwner(testOwner)
                    .withAccountNameOwner(ownerAccountName)
                    .asDebit()
                    .buildAndValidate()
            acct = accountRepository.save(acct)
            primaryAccountId = acct.accountId
        } else {
            primaryAccountId = acc.get().accountId
        }
    }

    void 'basic CRUD with smart builder'() {
        given:
        Transaction txn = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(LocalDate.parse("2023-01-01"))
                .withDescription("test transaction")
                .withCategory("test_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}")
                .withAmount("100.50")
                .asCleared()
                .buildAndValidate()

        when:
        Transaction saved = transactionRepository.save(txn)

        then:
        saved.transactionId != null
        saved.guid == txn.guid
        saved.amount == new BigDecimal("100.50")

        when:
        Optional<Transaction> found = transactionRepository.findByGuid(txn.guid)

        then:
        found.isPresent()
        found.get().description == "test transaction"
    }

    void 'find by account name owner and active status ordered by date desc'() {
        given:
        (1..3).each { i ->
            Transaction t = SmartTransactionBuilder.builderForOwner(testOwner)
                    .withAccountId(primaryAccountId)
                    .withAccountType(AccountType.Debit)
                    .withTransactionType(TransactionType.Expense)
                    .withAccountNameOwner(ownerAccountName)
                    .withTransactionDate(LocalDate.parse("2023-01-0${i}"))
                    .withDescription("txn_${i}")
                    .withCategory("cat_${i}")
                    .withAmount(new BigDecimal(100 + i))
                    .asCleared()
                    .buildAndValidate()
            transactionRepository.save(t)
        }

        when:
        List<Transaction> list = transactionRepository
                .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(ownerAccountName, true)

        then:
        list.size() >= 3
        list[0].transactionDate.isAfter(list[1].transactionDate)
        list.every { it.accountNameOwner == ownerAccountName && it.activeStatus }
    }

    void 'find by category and by description'() {
        given:
        Transaction t = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(LocalDate.parse("2023-02-01"))
                .withDescription("grocery shopping")
                .withCategory("groceries")
                .withAmount("85.50")
                .asCleared()
                .buildAndValidate()
        transactionRepository.save(t)

        when:
        List<Transaction> byCat = transactionRepository
                .findByCategoryAndActiveStatusOrderByTransactionDateDesc("groceries", true)
        List<Transaction> byDesc = transactionRepository
                .findByDescriptionAndActiveStatusOrderByTransactionDateDesc("grocery shopping", true)

        then:
        byCat.any { it.category == "groceries" }
        byDesc.any { it.description == "grocery shopping" }
    }

    void 'sum totals for active transactions by account owner'() {
        given:
        [TransactionState.Cleared, TransactionState.Outstanding, TransactionState.Future].each { state ->
            Transaction t = SmartTransactionBuilder.builderForOwner(testOwner)
                    .withAccountId(primaryAccountId)
                    .withAccountType(AccountType.Debit)
                    .withTransactionType(TransactionType.Expense)
                    .withAccountNameOwner(ownerAccountName)
                    .withTransactionDate(LocalDate.parse("2023-03-01"))
                    .withDescription("sum_${state}")
                    .withCategory("sumcat")
                    .withAmount("100.00")
                    .withTransactionState(state)
                    .asActive()
                    .buildAndValidate()
            transactionRepository.save(t)
        }

        when:
        List<Object[]> results = transactionRepository
                .sumTotalsForActiveTransactionsByAccountNameOwner(ownerAccountName)

        then:
        results.size() >= 3
        results.each { row ->
            assert row.length == 3
            assert row[0] != null && row[1] != null && row[2] != null
        }
    }

    void 'find by account name owner excluding states'() {
        given:
        Transaction cleared = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(LocalDate.parse("2023-04-01"))
                .withDescription("cleared")
                .withCategory("testcat")
                .withAmount("100.00")
                .asCleared()
                .buildAndValidate()

        Transaction future = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(LocalDate.parse("2023-04-02"))
                .withDescription("future")
                .withCategory("testcat")
                .withAmount("100.00")
                .asFuture()
                .buildAndValidate()

        transactionRepository.save(cleared)
        transactionRepository.save(future)

        when:
        List<Transaction> nonFuture = transactionRepository
                .findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc(
                        ownerAccountName, true, [TransactionState.Future])

        then:
        nonFuture.size() >= 1
        nonFuture.every { it.transactionState != TransactionState.Future }
        nonFuture.any { it.description == "cleared" }
    }

    void 'test count operations for category and description'() {
        given:
        String uniqueCategory = "count_test_category_${testOwner.replaceAll(/[^a-z]/, '')}"
        String uniqueDescription = "count_test_description_${testOwner.replaceAll(/[^a-z]/, '')}"

        for (int i = 0; i < 3; i++) {
            Transaction t = SmartTransactionBuilder.builderForOwner(testOwner)
                    .withAccountId(primaryAccountId)
                    .withAccountType(AccountType.Debit)
                    .withTransactionType(TransactionType.Expense)
                    .withAccountNameOwner(ownerAccountName)
                    .withTransactionDate(LocalDate.parse("2023-01-01"))
                    .withDescription(uniqueDescription)
                    .withCategory(uniqueCategory)
                    .withAmount("10.00")
                    .asCleared()
                    .buildAndValidate()
            transactionRepository.save(t)
        }

        when:
        Long categoryCount = transactionRepository.countByCategoryName(uniqueCategory)
        Long descriptionCount = transactionRepository.countByDescriptionName(uniqueDescription)

        then:
        categoryCount >= 3
        descriptionCount >= 3
    }

    void 'test duplicate GUID insert throws persistence exception'() {
        given:
        String duplicateGuid = "11111111-2222-3333-4444-555555555555"
        def t1 = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(LocalDate.parse("2023-02-01"))
                .withDescription("dup-guid-1")
                .withCategory("test_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}")
                .withAmount("10.00")
                .withGuid(duplicateGuid)
                .buildAndValidate()
        transactionRepository.save(t1)

        def t2 = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(LocalDate.parse("2023-02-02"))
                .withDescription("dup-guid-2")
                .withCategory("test_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}")
                .withAmount("12.00")
                .withGuid(duplicateGuid)
                .buildAndValidate()

        when:
        transactionRepository.save(t2)
        transactionRepository.flush()

        then:
        thrown(Exception)
    }

    void 'test category length constraint violation on save'() {
        given:
        def tooLongCategory = "x" * 60
        def t = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(LocalDate.parse("2023-03-01"))
                .withDescription("too-long-cat")
                .withCategory(tooLongCategory)
                .build()

        when:
        transactionRepository.save(t)
        transactionRepository.flush()

        then:
        thrown(jakarta.validation.ConstraintViolationException)
    }

    void 'test invalid GUID format constraint violation on save'() {
        given:
        def t = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(LocalDate.parse("2023-03-02"))
                .withDescription("bad-guid")
                .withCategory("test_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}")
                .withGuid("123")
                .build()

        when:
        transactionRepository.save(t)
        transactionRepository.flush()

        then:
        thrown(jakarta.validation.ConstraintViolationException)
    }

    void 'test delete transaction removes it from repository'() {
        given:
        def t = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(LocalDate.parse("2023-04-01"))
                .withDescription("to-delete")
                .withCategory("test_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}")
                .withAmount("5.00")
                .asCleared()
                .buildAndValidate()
        def saved = transactionRepository.save(t)

        when:
        transactionRepository.delete(saved)
        def found = transactionRepository.findByGuid(saved.guid)

        then:
        !found.isPresent()
    }
}
