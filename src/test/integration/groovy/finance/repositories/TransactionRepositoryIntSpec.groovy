package finance.repositories

import finance.Application
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.sql.Date

@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
@Transactional
class TransactionRepositoryIntSpec extends Specification {

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    AccountRepository accountRepository

    void setup() {
        // Create test account for transaction testing
        Account testAccount = new Account(
            accountNameOwner: "test_checking_brian",
            accountType: AccountType.Debit,
            activeStatus: true,
            moniker: 0L,
            totals: 0.0,
            totalsBalanced: 0.0,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )
        accountRepository.save(testAccount)
    }

    void 'test transaction repository basic CRUD operations'() {
        given:
        Transaction transaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: "test_checking_brian",
            accountType: AccountType.Debit,
            description: "Test Transaction",
            category: "Test Category",
            amount: 100.50,
            transactionDate: Date.valueOf("2023-01-01"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            reoccurringType: null,
            notes: "Integration test transaction",
            activeStatus: true,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

        when:
        Transaction savedTransaction = transactionRepository.save(transaction)

        then:
        savedTransaction.transactionId != null
        savedTransaction.guid == transaction.guid
        savedTransaction.amount == 100.50

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
            Transaction transaction = new Transaction(
                guid: UUID.randomUUID().toString(),
                accountNameOwner: "test_checking_brian",
                accountType: AccountType.Debit,
                description: "Test Transaction ${i}",
                category: "Test Category",
                amount: 100.00 + i,
                transactionDate: Date.valueOf("2023-01-0${i + 1}"),
                transactionState: TransactionState.Cleared,
                transactionType: TransactionType.Expense,
                activeStatus: true,
                dateUpdated: new Date(System.currentTimeMillis()),
                dateAdded: new Date(System.currentTimeMillis())
            )
            testTransactions.add(transactionRepository.save(transaction))
        }

        when:
        List<Transaction> foundTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc("test_checking_brian", true)

        then:
        foundTransactions.size() == 3
        foundTransactions[0].transactionDate.after(foundTransactions[1].transactionDate)
        foundTransactions.every { it.accountNameOwner == "test_checking_brian" }
        foundTransactions.every { it.activeStatus == true }
    }

    void 'test find transactions by category and description'() {
        given:
        Transaction categoryTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: "test_checking_brian",
            accountType: AccountType.Debit,
            description: "Grocery Shopping",
            category: "Groceries",
            amount: 85.50,
            transactionDate: Date.valueOf("2023-01-01"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            activeStatus: true,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )
        transactionRepository.save(categoryTransaction)

        when:
        List<Transaction> categoryTransactions = transactionRepository
            .findByCategoryAndActiveStatusOrderByTransactionDateDesc("Groceries", true)
        List<Transaction> descriptionTransactions = transactionRepository
            .findByDescriptionAndActiveStatusOrderByTransactionDateDesc("Grocery Shopping", true)

        then:
        categoryTransactions.size() == 1
        categoryTransactions[0].category == "Groceries"
        descriptionTransactions.size() == 1
        descriptionTransactions[0].description == "Grocery Shopping"
    }

    void 'test count operations for category and description'() {
        given:
        // Create multiple transactions with same category and description
        for (int i = 0; i < 5; i++) {
            Transaction transaction = new Transaction(
                guid: UUID.randomUUID().toString(),
                accountNameOwner: "test_checking_brian",
                accountType: AccountType.Debit,
                description: "Count Test Description",
                category: "Count Test Category",
                amount: 10.00,
                transactionDate: Date.valueOf("2023-01-01"),
                transactionState: TransactionState.Cleared,
                transactionType: TransactionType.Expense,
                activeStatus: true,
                dateUpdated: new Date(System.currentTimeMillis()),
                dateAdded: new Date(System.currentTimeMillis())
            )
            transactionRepository.save(transaction)
        }

        when:
        Long categoryCount = transactionRepository.countByCategoryName("Count Test Category")
        Long descriptionCount = transactionRepository.countByDescriptionName("Count Test Description")

        then:
        categoryCount == 5
        descriptionCount == 5
    }

    void 'test sum totals for active transactions by account name owner'() {
        given:
        // Create transactions with different states
        [TransactionState.Cleared, TransactionState.Outstanding, TransactionState.Future].each { state ->
            Transaction transaction = new Transaction(
                guid: UUID.randomUUID().toString(),
                accountNameOwner: "test_checking_brian",
                accountType: AccountType.Debit,
                description: "Sum Test ${state}",
                category: "Test Category",
                amount: 100.00,
                transactionDate: Date.valueOf("2023-01-01"),
                transactionState: state,
                transactionType: TransactionType.Expense,
                activeStatus: true,
                dateUpdated: new Date(System.currentTimeMillis()),
                dateAdded: new Date(System.currentTimeMillis())
            )
            transactionRepository.save(transaction)
        }

        when:
        List<Object[]> results = transactionRepository
            .sumTotalsForActiveTransactionsByAccountNameOwner("test_checking_brian")

        then:
        results.size() == 3  // Three different transaction states
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
        Transaction clearedTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: "test_checking_brian",
            accountType: AccountType.Debit,
            description: "Cleared Transaction",
            category: "Test Category",
            amount: 100.00,
            transactionDate: Date.valueOf("2023-01-01"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            activeStatus: true,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

        Transaction futureTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: "test_checking_brian",
            accountType: AccountType.Debit,
            description: "Future Transaction",
            category: "Test Category",
            amount: 200.00,
            transactionDate: Date.valueOf("2023-12-31"),
            transactionState: TransactionState.Future,
            transactionType: TransactionType.Expense,
            activeStatus: true,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

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

    void 'test transaction constraint violations'() {
        given:
        Transaction transactionWithoutAccount = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: null,  // This should cause constraint violation
            accountType: AccountType.Debit,
            description: "Invalid Transaction",
            category: "Test Category",
            amount: 100.00,
            transactionDate: Date.valueOf("2023-01-01"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            activeStatus: true,
            dateUpdated: new Date(System.currentTimeMillis()),
            dateAdded: new Date(System.currentTimeMillis())
        )

        when:
        transactionRepository.save(transactionWithoutAccount)
        transactionRepository.flush()

        then:
        thrown(DataIntegrityViolationException)
    }

    void 'test transaction query performance with large dataset'() {
        given:
        // Create a larger dataset to test query performance
        List<Transaction> transactions = []
        for (int i = 0; i < 100; i++) {
            Transaction transaction = new Transaction(
                guid: UUID.randomUUID().toString(),
                accountNameOwner: "test_checking_brian",
                accountType: AccountType.Debit,
                description: "Performance Test Transaction ${i}",
                category: i % 5 == 0 ? "Category A" : "Category B",
                amount: Math.random() * 1000,
                transactionDate: Date.valueOf("2023-01-01"),
                transactionState: i % 3 == 0 ? TransactionState.Cleared : TransactionState.Outstanding,
                transactionType: TransactionType.Expense,
                activeStatus: true,
                dateUpdated: new Date(System.currentTimeMillis()),
                dateAdded: new Date(System.currentTimeMillis())
            )
            transactions.add(transaction)
        }
        transactionRepository.saveAll(transactions)

        when:
        long startTime = System.currentTimeMillis()
        List<Transaction> foundTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc("test_checking_brian", true)
        long endTime = System.currentTimeMillis()

        then:
        foundTransactions.size() >= 100
        (endTime - startTime) < 5000  // Query should complete within 5 seconds
    }
}