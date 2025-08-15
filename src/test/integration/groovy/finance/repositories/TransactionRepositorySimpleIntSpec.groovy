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

import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
@Transactional
class TransactionRepositorySimpleIntSpec extends Specification {

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    AccountRepository accountRepository

    void setup() {
        // Create test account for transaction testing
        Account testAccount = new Account()
        testAccount.accountNameOwner = "test_checking_brian"
        testAccount.accountType = AccountType.Checking
        testAccount.activeStatus = true
        testAccount.moniker = "0000"
        testAccount.outstanding = new BigDecimal("0.00")
        testAccount.future = new BigDecimal("0.00")
        testAccount.cleared = new BigDecimal("0.00")
        testAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        testAccount.validationDate = new Timestamp(System.currentTimeMillis())
        accountRepository.save(testAccount)
    }

    void 'test transaction repository basic CRUD operations'() {
        given:
        Transaction transaction = new Transaction()
        transaction.guid = UUID.randomUUID().toString()
        transaction.accountNameOwner = "test_checking_brian"
        transaction.accountType = AccountType.Checking
        transaction.description = "Test Transaction"
        transaction.category = "Test Category"
        transaction.amount = new BigDecimal("100.50")
        transaction.transactionDate = Date.valueOf("2023-01-01")
        transaction.transactionState = TransactionState.Cleared
        transaction.transactionType = TransactionType.Debit
        transaction.notes = "Integration test transaction"
        transaction.activeStatus = true
        transaction.accountId = 1L

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
        foundTransaction.get().description == "Test Transaction"
        foundTransaction.get().category == "Test Category"
    }

    void 'test find transactions by account name owner and active status'() {
        given:
        List<Transaction> testTransactions = []
        for (int i = 0; i < 3; i++) {
            Transaction transaction = new Transaction()
            transaction.guid = UUID.randomUUID().toString()
            transaction.accountNameOwner = "test_checking_brian"
            transaction.accountType = AccountType.Checking
            transaction.description = "Test Transaction ${i}"
            transaction.category = "Test Category"
            transaction.amount = new BigDecimal("100.00").add(new BigDecimal(i))
            transaction.transactionDate = Date.valueOf("2023-01-0${i + 1}")
            transaction.transactionState = TransactionState.Cleared
            transaction.transactionType = TransactionType.Debit
            transaction.activeStatus = true
            transaction.notes = "Test note"
            transaction.accountId = 1L
            testTransactions.add(transactionRepository.save(transaction))
        }

        when:
        List<Transaction> foundTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc("test_checking_brian", true)

        then:
        foundTransactions.size() >= 3
        foundTransactions.every { it.accountNameOwner == "test_checking_brian" }
        foundTransactions.every { it.activeStatus == true }
    }

    void 'test find transactions by category and description'() {
        given:
        Transaction categoryTransaction = new Transaction()
        categoryTransaction.guid = UUID.randomUUID().toString()
        categoryTransaction.accountNameOwner = "test_checking_brian"
        categoryTransaction.accountType = AccountType.Checking
        categoryTransaction.description = "Grocery Shopping"
        categoryTransaction.category = "Groceries"
        categoryTransaction.amount = new BigDecimal("85.50")
        categoryTransaction.transactionDate = Date.valueOf("2023-01-01")
        categoryTransaction.transactionState = TransactionState.Cleared
        categoryTransaction.transactionType = TransactionType.Debit
        categoryTransaction.activeStatus = true
        categoryTransaction.notes = "Test grocery transaction"
        categoryTransaction.accountId = 1L
        transactionRepository.save(categoryTransaction)

        when:
        List<Transaction> categoryTransactions = transactionRepository
            .findByCategoryAndActiveStatusOrderByTransactionDateDesc("Groceries", true)
        List<Transaction> descriptionTransactions = transactionRepository
            .findByDescriptionAndActiveStatusOrderByTransactionDateDesc("Grocery Shopping", true)

        then:
        categoryTransactions.size() >= 1
        categoryTransactions.any { it.category == "Groceries" }
        descriptionTransactions.size() >= 1
        descriptionTransactions.any { it.description == "Grocery Shopping" }
    }

    void 'test count operations for category and description'() {
        given:
        // Create multiple transactions with same category and description
        for (int i = 0; i < 3; i++) {
            Transaction transaction = new Transaction()
            transaction.guid = UUID.randomUUID().toString()
            transaction.accountNameOwner = "test_checking_brian"
            transaction.accountType = AccountType.Checking
            transaction.description = "Count Test Description"
            transaction.category = "Count Test Category"
            transaction.amount = new BigDecimal("10.00")
            transaction.transactionDate = Date.valueOf("2023-01-01")
            transaction.transactionState = TransactionState.Cleared
            transaction.transactionType = TransactionType.Debit
            transaction.activeStatus = true
            transaction.notes = "Count test"
            transaction.accountId = 1L
            transactionRepository.save(transaction)
        }

        when:
        Long categoryCount = transactionRepository.countByCategoryName("Count Test Category")
        Long descriptionCount = transactionRepository.countByDescriptionName("Count Test Description")

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
            transaction.accountNameOwner = "test_checking_brian"
            transaction.accountType = AccountType.Checking
            transaction.description = "Sum Test ${state}"
            transaction.category = "Test Category"
            transaction.amount = new BigDecimal("100.00")
            transaction.transactionDate = Date.valueOf("2023-01-01")
            transaction.transactionState = state
            transaction.transactionType = TransactionType.Debit
            transaction.activeStatus = true
            transaction.notes = "Sum test"
            transaction.accountId = 1L
            transactionRepository.save(transaction)
        }

        when:
        List<Object[]> results = transactionRepository
            .sumTotalsForActiveTransactionsByAccountNameOwner("test_checking_brian")

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
        clearedTransaction.accountNameOwner = "test_checking_brian"
        clearedTransaction.accountType = AccountType.Checking
        clearedTransaction.description = "Cleared Transaction"
        clearedTransaction.category = "Test Category"
        clearedTransaction.amount = new BigDecimal("100.00")
        clearedTransaction.transactionDate = Date.valueOf("2023-01-01")
        clearedTransaction.transactionState = TransactionState.Cleared
        clearedTransaction.transactionType = TransactionType.Debit
        clearedTransaction.activeStatus = true
        clearedTransaction.notes = "Cleared test"
        clearedTransaction.accountId = 1L

        Transaction futureTransaction = new Transaction()
        futureTransaction.guid = UUID.randomUUID().toString()
        futureTransaction.accountNameOwner = "test_checking_brian"
        futureTransaction.accountType = AccountType.Checking
        futureTransaction.description = "Future Transaction"
        futureTransaction.category = "Test Category"
        futureTransaction.amount = new BigDecimal("200.00")
        futureTransaction.transactionDate = Date.valueOf("2023-12-31")
        futureTransaction.transactionState = TransactionState.Future
        futureTransaction.transactionType = TransactionType.Debit
        futureTransaction.activeStatus = true
        futureTransaction.notes = "Future test"
        futureTransaction.accountId = 1L

        transactionRepository.save(clearedTransaction)
        transactionRepository.save(futureTransaction)

        when:
        List<Transaction> nonFutureTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc(
                "test_checking_brian", true, [TransactionState.Future])

        then:
        nonFutureTransactions.size() >= 1
        nonFutureTransactions.every { it.transactionState != TransactionState.Future }
        nonFutureTransactions.any { it.description == "Cleared Transaction" }
    }

    void 'test transaction query performance with multiple transactions'() {
        given:
        // Create a moderate dataset to test query performance
        List<Transaction> transactions = []
        for (int i = 0; i < 50; i++) {
            Transaction transaction = new Transaction()
            transaction.guid = UUID.randomUUID().toString()
            transaction.accountNameOwner = "test_checking_brian"
            transaction.accountType = AccountType.Checking
            transaction.description = "Performance Test Transaction ${i}"
            transaction.category = i % 5 == 0 ? "Category A" : "Category B"
            transaction.amount = new BigDecimal(Math.random() * 1000)
            transaction.transactionDate = Date.valueOf("2023-01-01")
            transaction.transactionState = i % 3 == 0 ? TransactionState.Cleared : TransactionState.Outstanding
            transaction.transactionType = TransactionType.Debit
            transaction.activeStatus = true
            transaction.notes = "Performance test"
            transaction.accountId = 1L
            transactions.add(transaction)
        }
        transactionRepository.saveAll(transactions)

        when:
        long startTime = System.currentTimeMillis()
        List<Transaction> foundTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc("test_checking_brian", true)
        long endTime = System.currentTimeMillis()

        then:
        foundTransactions.size() >= 50
        (endTime - startTime) < 5000  // Query should complete within 5 seconds
    }
}