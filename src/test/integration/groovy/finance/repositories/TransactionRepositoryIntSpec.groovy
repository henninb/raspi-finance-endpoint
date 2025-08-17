package finance.repositories

import finance.Application
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.domain.ReoccurringType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.UUID

@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
@Transactional
class TransactionRepositoryIntSpec extends Specification {

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    AccountRepository accountRepository

    Long testAccountId

    void setup() {
        // Create test account for transaction testing - use minimal constructor to avoid validation issues
        Account testAccount = new Account()
        testAccount.accountNameOwner = "test_brian"
        testAccount.accountType = AccountType.Debit
        testAccount.activeStatus = true
        testAccount.moniker = "0000"
        testAccount.outstanding = new BigDecimal("0.00")
        testAccount.future = new BigDecimal("0.00")
        testAccount.cleared = new BigDecimal("0.00")
        testAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        testAccount.validationDate = new Timestamp(System.currentTimeMillis())
        testAccount.dateAdded = new Timestamp(System.currentTimeMillis())
        testAccount.dateUpdated = new Timestamp(System.currentTimeMillis())
        
        Account savedAccount = accountRepository.save(testAccount)
        testAccountId = savedAccount.accountId
    }

    void 'test transaction repository basic CRUD operations'() {
        given:
        Transaction transaction = new Transaction(
            transactionId: 0L,
            guid: UUID.randomUUID().toString(),
            accountId: testAccountId,
            accountType: AccountType.Debit,
            transactionType: TransactionType.Expense,
            accountNameOwner: "test_brian",
            transactionDate: Date.valueOf("2023-01-01"),
            description: "test transaction",
            category: "test_category",
            amount: new BigDecimal("100.50"),
            transactionState: TransactionState.Cleared,
            activeStatus: true,
            reoccurringType: ReoccurringType.Undefined,
            notes: "integration test transaction"
        )

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
        foundTransaction.get().category == "test_category"
    }

    void 'test find transactions by account name owner and active status'() {
        given:
        List<Transaction> testTransactions = []
        for (int i = 0; i < 3; i++) {
            Transaction transaction = new Transaction(
                transactionId: 0L,
                guid: UUID.randomUUID().toString(),
                accountId: testAccountId,
                accountType: AccountType.Debit,
                transactionType: TransactionType.Expense,
                accountNameOwner: "test_brian",
                transactionDate: Date.valueOf("2023-01-0${i + 1}"),
                description: "test transaction ${i}",
                category: "test_category",
                amount: new BigDecimal(100.00 + i),
                transactionState: TransactionState.Cleared,
                activeStatus: true,
                reoccurringType: ReoccurringType.Undefined,
                notes: ""
            )
            testTransactions.add(transactionRepository.save(transaction))
        }

        when:
        List<Transaction> foundTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc("test_brian", true)

        then:
        foundTransactions.size() == 3
        foundTransactions[0].transactionDate.after(foundTransactions[1].transactionDate)
        foundTransactions.every { it.accountNameOwner == "test_brian" }
        foundTransactions.every { it.activeStatus == true }
    }

    void 'test find transactions by category and description'() {
        given:
        Transaction categoryTransaction = new Transaction(
            transactionId: 0L,
            guid: UUID.randomUUID().toString(),
            accountId: testAccountId,
            accountType: AccountType.Debit,
            transactionType: TransactionType.Expense,
            accountNameOwner: "test_brian",
            transactionDate: Date.valueOf("2023-01-01"),
            description: "grocery shopping",
            category: "groceries",
            amount: new BigDecimal("85.50"),
            transactionState: TransactionState.Cleared,
            activeStatus: true,
            reoccurringType: ReoccurringType.Undefined,
            notes: ""
        )
        transactionRepository.save(categoryTransaction)

        when:
        List<Transaction> categoryTransactions = transactionRepository
            .findByCategoryAndActiveStatusOrderByTransactionDateDesc("groceries", true)
        List<Transaction> descriptionTransactions = transactionRepository
            .findByDescriptionAndActiveStatusOrderByTransactionDateDesc("grocery shopping", true)

        then:
        categoryTransactions.size() == 1
        categoryTransactions[0].category == "groceries"
        descriptionTransactions.size() == 1
        descriptionTransactions[0].description == "grocery shopping"
    }

    void 'test count operations for category and description'() {
        given:
        // Create multiple transactions with same category and description
        for (int i = 0; i < 5; i++) {
            Transaction transaction = new Transaction(
                transactionId: 0L,
                guid: UUID.randomUUID().toString(),
                accountId: testAccountId,
                accountType: AccountType.Debit,
                transactionType: TransactionType.Expense,
                accountNameOwner: "test_brian",
                transactionDate: Date.valueOf("2023-01-01"),
                description: "count test description",
                category: "count_test_category",
                amount: new BigDecimal("10.00"),
                transactionState: TransactionState.Cleared,
                activeStatus: true,
                reoccurringType: ReoccurringType.Undefined,
                notes: ""
            )
            transactionRepository.save(transaction)
        }

        when:
        Long categoryCount = transactionRepository.countByCategoryName("count_test_category")
        Long descriptionCount = transactionRepository.countByDescriptionName("count test description")

        then:
        categoryCount == 5
        descriptionCount == 5
    }

    void 'test sum totals for active transactions by account name owner'() {
        given:
        // Create transactions with different states
        [TransactionState.Cleared, TransactionState.Outstanding, TransactionState.Future].each { state ->
            Transaction transaction = new Transaction(
                transactionId: 0L,
                guid: UUID.randomUUID().toString(),
                accountId: testAccountId,
                accountType: AccountType.Debit,
                transactionType: TransactionType.Expense,
                accountNameOwner: "test_brian",
                transactionDate: Date.valueOf("2023-01-01"),
                description: "sum test ${state}",
                category: "test_category",
                amount: new BigDecimal("100.00"),
                transactionState: state,
                activeStatus: true,
                reoccurringType: ReoccurringType.Undefined,
                notes: ""
            )
            transactionRepository.save(transaction)
        }

        when:
        List<Object[]> results = transactionRepository
            .sumTotalsForActiveTransactionsByAccountNameOwner("test_brian")

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
            transactionId: 0L,
            guid: UUID.randomUUID().toString(),
            accountId: testAccountId,
            accountType: AccountType.Debit,
            transactionType: TransactionType.Expense,
            accountNameOwner: "test_brian",
            transactionDate: Date.valueOf("2023-01-01"),
            description: "cleared transaction",
            category: "test_category",
            amount: new BigDecimal("100.00"),
            transactionState: TransactionState.Cleared,
            activeStatus: true,
            reoccurringType: ReoccurringType.Undefined,
            notes: ""
        )

        Transaction futureTransaction = new Transaction(
            transactionId: 0L,
            guid: UUID.randomUUID().toString(),
            accountId: testAccountId,
            accountType: AccountType.Debit,
            transactionType: TransactionType.Expense,
            accountNameOwner: "test_brian",
            transactionDate: Date.valueOf("2023-12-31"),
            description: "future transaction",
            category: "test_category",
            amount: new BigDecimal("200.00"),
            transactionState: TransactionState.Future,
            activeStatus: true,
            reoccurringType: ReoccurringType.Undefined,
            notes: ""
        )

        transactionRepository.save(clearedTransaction)
        transactionRepository.save(futureTransaction)

        when:
        List<Transaction> nonFutureTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc(
                "test_brian", true, [TransactionState.Future])

        then:
        nonFutureTransactions.size() >= 1
        nonFutureTransactions.every { it.transactionState != TransactionState.Future }
        nonFutureTransactions.any { it.description == "cleared transaction" }
    }

    void 'test transaction constraint violations'() {
        given:
        Transaction transactionWithBadDescription = new Transaction(
            transactionId: 0L,
            guid: UUID.randomUUID().toString(),
            accountId: testAccountId,
            accountType: AccountType.Debit,
            transactionType: TransactionType.Expense,
            accountNameOwner: "test_brian",
            transactionDate: Date.valueOf("2023-01-01"),
            description: "",  // Empty description should cause constraint violation (min 1)
            category: "test_category",
            amount: new BigDecimal("100.00"),
            transactionState: TransactionState.Cleared,
            activeStatus: true,
            reoccurringType: ReoccurringType.Undefined,
            notes: ""
        )

        when:
        transactionRepository.save(transactionWithBadDescription)
        transactionRepository.flush()

        then:
        thrown(Exception) // Could be ConstraintViolationException or DataIntegrityViolationException
    }

    void 'test transaction query performance with large dataset'() {
        given:
        // Create a larger dataset to test query performance
        List<Transaction> transactions = []
        for (int i = 0; i < 100; i++) {
            Transaction transaction = new Transaction(
                transactionId: 0L,
                guid: UUID.randomUUID().toString(),
                accountId: testAccountId,
                accountType: AccountType.Debit,
                transactionType: TransactionType.Expense,
                accountNameOwner: "test_brian",
                transactionDate: Date.valueOf("2023-01-01"),
                description: "perftest${i}",
                category: i % 5 == 0 ? "category_a" : "category_b",
                amount: new BigDecimal(Math.random() * 1000),
                transactionState: i % 3 == 0 ? TransactionState.Cleared : TransactionState.Outstanding,
                activeStatus: true,
                reoccurringType: ReoccurringType.Undefined,
                notes: ""
            )
            transactions.add(transaction)
        }
        transactionRepository.saveAll(transactions)

        when:
        long startTime = System.currentTimeMillis()
        List<Transaction> foundTransactions = transactionRepository
            .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc("test_brian", true)
        long endTime = System.currentTimeMillis()

        then:
        foundTransactions.size() >= 100
        (endTime - startTime) < 5000  // Query should complete within 5 seconds
    }
}