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
import java.sql.Date
import java.util.Optional

/**
 * MIGRATED INTEGRATION TEST - Transaction Repository Simple with robust, isolated architecture
 *
 * This is the migrated version of TransactionRepositorySimpleIntSpec showing:
 * ✅ No hardcoded account names - all use testOwner for uniqueness
 * ✅ SmartBuilder pattern with constraint validation
 * ✅ Test isolation - each test gets its own test data
 * ✅ Proper FK relationship management with Account setup
 * ✅ Financial validation and consistency
 * ✅ Eliminated shared global state (testAccountId)
 */
class TransactionRepositorySimpleIntSpec extends BaseIntegrationSpec {

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
                    .withUniqueAccountName("testchecking")
                    .asDebit()
                    .buildAndValidate()
            acct = accountRepository.save(acct)
            primaryAccountId = acct.accountId
            ownerAccountName = acct.accountNameOwner
        } else {
            primaryAccountId = acc.get().accountId
        }
    }

    def "test transaction repository basic CRUD operations"() {
        given:
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(Date.valueOf("2023-01-01"))
                .withDescription("test transaction")
                .withCategory(repositoryContext.categoryName)
                .withAmount(new BigDecimal("100.50"))
                .asCleared()
                .buildAndValidate()

        when:
        Transaction savedTransaction = transactionRepository.save(transaction)

        then:
        savedTransaction.transactionId != null
        savedTransaction.guid == transaction.guid
        savedTransaction.amount == new BigDecimal("100.50")

        when:
        Optional<Transaction> foundTransaction = transactionRepository.findByGuid(transaction.guid)

        then:
        foundTransaction.isPresent()
        foundTransaction.get().description == "test transaction"
        foundTransaction.get().category == repositoryContext.categoryName
    }

    def "test find transactions by account name owner and active status"() {
        given:
        List<Transaction> testTransactions = []
        for (int i = 0; i < 3; i++) {
            Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                    .withAccountId(primaryAccountId)
                    .withAccountType(AccountType.Debit)
                    .withTransactionType(TransactionType.Expense)
                    .withAccountNameOwner(ownerAccountName)
                    .withTransactionDate(Date.valueOf("2023-01-0${i + 1}"))
                    .withDescription("test-transaction-${i}")
                    .withCategory(repositoryContext.categoryName)
                    .withAmount(new BigDecimal("100.00").add(new BigDecimal(i)))
                    .asCleared()
                    .buildAndValidate()
            testTransactions.add(transactionRepository.save(transaction))
        }

        when:
        List<Transaction> foundTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(ownerAccountName, true)

        then:
        foundTransactions.size() >= 3
        foundTransactions.every { it.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, '')) }
        foundTransactions.every { it.activeStatus == true }
    }

    def "test find transactions by category and description"() {
        given:
        Transaction categoryTransaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(Date.valueOf("2023-01-01"))
                .withDescription("grocery-shopping")
                .withCategory("groceries")
                .withAmount(new BigDecimal("85.50"))
                .asCleared()
                .buildAndValidate()
        transactionRepository.save(categoryTransaction)

        when:
        List<Transaction> categoryTransactions = transactionRepository
            .findByCategoryAndActiveStatusOrderByTransactionDateDesc("groceries", true)
        List<Transaction> descriptionTransactions = transactionRepository
            .findByDescriptionAndActiveStatusOrderByTransactionDateDesc("grocery-shopping", true)

        then:
        categoryTransactions.size() >= 1
        categoryTransactions.any { it.category == "groceries" }
        descriptionTransactions.size() >= 1
        descriptionTransactions.any { it.description == "grocery-shopping" }
    }

    def "test count operations for category and description"() {
        given:
        // Create multiple transactions with same category and description
        String uniqueCategory = "count-test-category-${testOwner.replaceAll(/[^a-z]/, '')}"
        String uniqueDescription = "count-test-description-${testOwner.replaceAll(/[^a-z]/, '')}"

        for (int i = 0; i < 3; i++) {
            Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                    .withAccountId(primaryAccountId)
                    .withAccountType(AccountType.Debit)
                    .withTransactionType(TransactionType.Expense)
                    .withAccountNameOwner(ownerAccountName)
                    .withTransactionDate(Date.valueOf("2023-01-01"))
                    .withDescription(uniqueDescription)
                    .withCategory(uniqueCategory)
                    .withAmount(new BigDecimal("10.00"))
                    .asCleared()
                    .buildAndValidate()
            transactionRepository.save(transaction)
        }

        when:
        Long categoryCount = transactionRepository.countByCategoryName(uniqueCategory)
        Long descriptionCount = transactionRepository.countByDescriptionName(uniqueDescription)

        then:
        categoryCount >= 3
        descriptionCount >= 3
    }

    def "test sum totals for active transactions by account name owner"() {
        given:
        // Create transactions with different states
        [TransactionState.Cleared, TransactionState.Outstanding, TransactionState.Future].each { state ->
            Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                    .withAccountId(primaryAccountId)
                    .withAccountType(AccountType.Debit)
                    .withTransactionType(TransactionType.Expense)
                    .withAccountNameOwner(ownerAccountName)
                    .withTransactionDate(Date.valueOf("2023-01-01"))
                    .withDescription("sum-test-${state}")
                    .withCategory(repositoryContext.categoryName)
                    .withAmount(new BigDecimal("100.00"))
                    .withTransactionState(state)
                    .buildAndValidate()
            transactionRepository.save(transaction)
        }

        when:
        List<Object[]> results = transactionRepository
            .sumTotalsForActiveTransactionsByAccountNameOwner(ownerAccountName)

        then:
        results.size() >= 1  // At least one result for the account
        results.each { result ->
            assert result.length == 3  // amount, count, transaction_state
            assert result[0] != null   // sum amount
            assert result[1] != null   // count
            assert result[2] != null   // transaction state
        }
    }

    def "test find by account name owner excluding transaction states"() {
        given:
        // Create transactions with different states
        Transaction clearedTransaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(Date.valueOf("2023-01-01"))
                .withDescription("cleared-transaction")
                .withCategory(repositoryContext.categoryName)
                .withAmount(new BigDecimal("100.00"))
                .asCleared()
                .buildAndValidate()

        Transaction futureTransaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(Date.valueOf("2023-12-31"))
                .withDescription("future-transaction")
                .withCategory(repositoryContext.categoryName)
                .withAmount(new BigDecimal("200.00"))
                .asFuture()
                .buildAndValidate()

        transactionRepository.save(clearedTransaction)
        transactionRepository.save(futureTransaction)

        when:
        List<Transaction> nonFutureTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc(
                ownerAccountName, true, [TransactionState.Future])

        then:
        nonFutureTransactions.size() >= 1
        nonFutureTransactions.every { it.transactionState != TransactionState.Future }
        nonFutureTransactions.any { it.description == "cleared-transaction" }
    }

    def "test transaction query performance with multiple transactions"() {
        given:
        // Create a moderate dataset to test query performance
        List<Transaction> transactions = []
        for (int i = 0; i < 50; i++) {
            Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                    .withAccountId(primaryAccountId)
                    .withAccountType(AccountType.Debit)
                    .withTransactionType(TransactionType.Expense)
                    .withAccountNameOwner(ownerAccountName)
                    .withTransactionDate(Date.valueOf("2023-01-01"))
                    .withDescription("performance-test-transaction-${i}")
                    .withCategory(i % 5 == 0 ? "category-a" : "category-b")
                    .withAmount(new BigDecimal(String.format("%.2f", Math.random() * 1000)))
                    .withTransactionState(i % 3 == 0 ? TransactionState.Cleared : TransactionState.Outstanding)
                    .buildAndValidate()
            transactions.add(transaction)
        }
        transactionRepository.saveAll(transactions)

        when:
        long startTime = System.currentTimeMillis()
        List<Transaction> foundTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(ownerAccountName, true)
        long endTime = System.currentTimeMillis()

        then:
        foundTransactions.size() >= 50
        (endTime - startTime) < 5000  // Query should complete within 5 seconds
        foundTransactions.every { it.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, '')) }
    }

    def "test SmartBuilder constraint validation and FK relationships"() {
        when: "Creating transaction with valid account relationship"
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(Date.valueOf("2023-01-01"))
                .withDescription("constraint validation test")
                .withCategory(repositoryContext.categoryName)
                .withAmount(new BigDecimal("50.00"))
                .asCleared()
                .buildAndValidate()

        then: "SmartBuilder creates valid transaction with proper FK relationships"
        transaction != null
        transaction.accountId == primaryAccountId
        transaction.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))
        transaction.category == repositoryContext.categoryName
        transaction.guid != null
        transaction.guid.length() > 10  // UUID format
    }

    def "test isolated test data with unique testOwner"() {
        given: "Multiple transactions with testOwner-based naming"
        Transaction transaction1 = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(Date.valueOf("2023-01-15"))
                .withDescription("isolation-test-one")
                .withCategory(repositoryContext.categoryName)
                .withAmount(new BigDecimal("25.00"))
                .asCleared()
                .buildAndValidate()

        Transaction transaction2 = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Income)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(Date.valueOf("2023-01-16"))
                .withDescription("isolation-test-two")
                .withCategory(repositoryContext.categoryName)
                .withAmount(new BigDecimal("75.00"))
                .asOutstanding()
                .buildAndValidate()

        when: "Saving transactions"
        Transaction saved1 = transactionRepository.save(transaction1)
        Transaction saved2 = transactionRepository.save(transaction2)

        then: "Both transactions have unique GUIDs and testOwner-based account names"
        saved1.guid != saved2.guid
        saved1.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))
        saved2.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))
        saved1.accountNameOwner == saved2.accountNameOwner  // Same account for this test owner

        and: "Transaction types are correctly set"
        saved1.transactionType == TransactionType.Expense
        saved2.transactionType == TransactionType.Income
        saved1.transactionState == TransactionState.Cleared
        saved2.transactionState == TransactionState.Outstanding
    }
}
