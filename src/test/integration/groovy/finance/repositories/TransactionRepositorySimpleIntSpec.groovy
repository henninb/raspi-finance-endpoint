package finance.repositories

import finance.Application
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import spock.lang.Ignore

import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
@Transactional
@Ignore("Legacy brittle spec replaced by TransactionRepositorySimpleMigratedIntSpec; keeping for reference.")
class TransactionRepositorySimpleIntSpec extends Specification {

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    AccountRepository accountRepository

    private Long testAccountId

    void setup() {
        // Create test account for transaction testing
        Account testAccount = new Account()
        testAccount.accountNameOwner = "testchecking_brian"
        testAccount.accountType = AccountType.Debit
        testAccount.activeStatus = true
        testAccount.moniker = "0000"
        testAccount.outstanding = new BigDecimal("0.00")
        testAccount.future = new BigDecimal("0.00")
        testAccount.cleared = new BigDecimal("0.00")
        testAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        testAccount.validationDate = new Timestamp(System.currentTimeMillis())
        Account savedAccount = accountRepository.save(testAccount)
        testAccountId = savedAccount.accountId
    }

    void 'test transaction repository basic CRUD operations'() {
        given:
        Transaction transaction = new Transaction()
        transaction.guid = UUID.randomUUID().toString()
        transaction.accountNameOwner = "testchecking_brian"
        transaction.accountType = AccountType.Debit
        transaction.description = "test transaction"
        transaction.category = "testcategory"
        transaction.amount = new BigDecimal("100.50")
        transaction.transactionDate = Date.valueOf("2023-01-01")
        transaction.transactionState = TransactionState.Cleared
        transaction.transactionType = TransactionType.Expense
        transaction.notes = "integration test transaction"
        transaction.activeStatus = true
        transaction.accountId = testAccountId

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
        foundTransaction.get().category == "testcategory"
    }

    void 'test find transactions by account name owner and active status'() {
        given:
        List<Transaction> testTransactions = []
        for (int i = 0; i < 3; i++) {
            Transaction transaction = new Transaction()
            transaction.guid = UUID.randomUUID().toString()
            transaction.accountNameOwner = "testchecking_brian"
            transaction.accountType = AccountType.Debit
            transaction.description = "test-transaction-${i}"
            transaction.category = "testcategory"
            transaction.amount = new BigDecimal("100.00").add(new BigDecimal(i))
            transaction.transactionDate = Date.valueOf("2023-01-0${i + 1}")
            transaction.transactionState = TransactionState.Cleared
            transaction.transactionType = TransactionType.Expense
            transaction.activeStatus = true
            transaction.notes = "test-note"
            transaction.accountId = testAccountId
            testTransactions.add(transactionRepository.save(transaction))
        }

        when:
        List<Transaction> foundTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc("testchecking_brian", true)

        then:
        foundTransactions.size() >= 3
        foundTransactions.every { it.accountNameOwner == "testchecking_brian" }
        foundTransactions.every { it.activeStatus == true }
    }

    void 'test find transactions by category and description'() {
        given:
        Transaction categoryTransaction = new Transaction()
        categoryTransaction.guid = UUID.randomUUID().toString()
        categoryTransaction.accountNameOwner = "testchecking_brian"
        categoryTransaction.accountType = AccountType.Debit
        categoryTransaction.description = "grocery-shopping"
        categoryTransaction.category = "groceries"
        categoryTransaction.amount = new BigDecimal("85.50")
        categoryTransaction.transactionDate = Date.valueOf("2023-01-01")
        categoryTransaction.transactionState = TransactionState.Cleared
        categoryTransaction.transactionType = TransactionType.Expense
        categoryTransaction.activeStatus = true
        categoryTransaction.notes = "test-grocery-transaction"
        categoryTransaction.accountId = testAccountId
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

    void 'test count operations for category and description'() {
        given:
        // Create multiple transactions with same category and description
        for (int i = 0; i < 3; i++) {
            Transaction transaction = new Transaction()
            transaction.guid = UUID.randomUUID().toString()
            transaction.accountNameOwner = "testchecking_brian"
            transaction.accountType = AccountType.Debit
            transaction.description = "count-test-description"
            transaction.category = "count-test-category"
            transaction.amount = new BigDecimal("10.00")
            transaction.transactionDate = Date.valueOf("2023-01-01")
            transaction.transactionState = TransactionState.Cleared
            transaction.transactionType = TransactionType.Expense
            transaction.activeStatus = true
            transaction.notes = "count-test"
            transaction.accountId = testAccountId
            transactionRepository.save(transaction)
        }

        when:
        Long categoryCount = transactionRepository.countByCategoryName("count-test-category")
        Long descriptionCount = transactionRepository.countByDescriptionName("count-test-description")

        then:
        categoryCount >= 3
        descriptionCount >= 3
    }

    void 'test sum totals for active transactions by account name owner'() {
        given:
        // Create transactions with different states
        [TransactionState.Cleared, TransactionState.Outstanding, TransactionState.Future].each { state ->
            Transaction transaction = new Transaction()
            transaction.guid = UUID.randomUUID().toString()
            transaction.accountNameOwner = "testchecking_brian"
            transaction.accountType = AccountType.Debit
            transaction.description = "sum-test-${state}"
            transaction.category = "testcategory"
            transaction.amount = new BigDecimal("100.00")
            transaction.transactionDate = Date.valueOf("2023-01-01")
            transaction.transactionState = state
            transaction.transactionType = TransactionType.Expense
            transaction.activeStatus = true
            transaction.notes = "sum-test"
            transaction.accountId = testAccountId
            transactionRepository.save(transaction)
        }

        when:
        List<Object[]> results = transactionRepository
            .sumTotalsForActiveTransactionsByAccountNameOwner("testchecking_brian")

        then:
        results.size() >= 1  // At least one result for the account
        results.each { result ->
            assert result.length == 3  // amount, count, transaction_state
            assert result[0] != null   // sum amount
            assert result[1] != null   // count
            assert result[2] != null   // transaction state
        }
    }

    void 'test find by account name owner excluding transaction states'() {
        given:
        // Create transactions with different states
        Transaction clearedTransaction = new Transaction()
        clearedTransaction.guid = UUID.randomUUID().toString()
        clearedTransaction.accountNameOwner = "testchecking_brian"
        clearedTransaction.accountType = AccountType.Debit
        clearedTransaction.description = "cleared-transaction"
        clearedTransaction.category = "testcategory"
        clearedTransaction.amount = new BigDecimal("100.00")
        clearedTransaction.transactionDate = Date.valueOf("2023-01-01")
        clearedTransaction.transactionState = TransactionState.Cleared
        clearedTransaction.transactionType = TransactionType.Expense
        clearedTransaction.activeStatus = true
        clearedTransaction.notes = "cleared-test"
        clearedTransaction.accountId = testAccountId

        Transaction futureTransaction = new Transaction()
        futureTransaction.guid = UUID.randomUUID().toString()
        futureTransaction.accountNameOwner = "testchecking_brian"
        futureTransaction.accountType = AccountType.Debit
        futureTransaction.description = "future-transaction"
        futureTransaction.category = "testcategory"
        futureTransaction.amount = new BigDecimal("200.00")
        futureTransaction.transactionDate = Date.valueOf("2023-12-31")
        futureTransaction.transactionState = TransactionState.Future
        futureTransaction.transactionType = TransactionType.Expense
        futureTransaction.activeStatus = true
        futureTransaction.notes = "future-test"
        futureTransaction.accountId = testAccountId

        transactionRepository.save(clearedTransaction)
        transactionRepository.save(futureTransaction)

        when:
        List<Transaction> nonFutureTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc(
                "testchecking_brian", true, [TransactionState.Future])

        then:
        nonFutureTransactions.size() >= 1
        nonFutureTransactions.every { it.transactionState != TransactionState.Future }
        nonFutureTransactions.any { it.description == "cleared-transaction" }
    }

    void 'test transaction query performance with multiple transactions'() {
        given:
        // Create a moderate dataset to test query performance
        List<Transaction> transactions = []
        for (int i = 0; i < 50; i++) {
            Transaction transaction = new Transaction()
            transaction.guid = UUID.randomUUID().toString()
            transaction.accountNameOwner = "testchecking_brian"
            transaction.accountType = AccountType.Debit
            transaction.description = "performance-test-transaction-${i}"
            transaction.category = i % 5 == 0 ? "category-a" : "category-b"
            transaction.amount = new BigDecimal(String.format("%.2f", Math.random() * 1000))
            transaction.transactionDate = Date.valueOf("2023-01-01")
            transaction.transactionState = i % 3 == 0 ? TransactionState.Cleared : TransactionState.Outstanding
            transaction.transactionType = TransactionType.Expense
            transaction.activeStatus = true
            transaction.notes = "performance-test"
            transaction.accountId = testAccountId
            transactions.add(transaction)
        }
        transactionRepository.saveAll(transactions)

        when:
        long startTime = System.currentTimeMillis()
        List<Transaction> foundTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc("testchecking_brian", true)
        long endTime = System.currentTimeMillis()

        then:
        foundTransactions.size() >= 50
        (endTime - startTime) < 5000  // Query should complete within 5 seconds
    }
}