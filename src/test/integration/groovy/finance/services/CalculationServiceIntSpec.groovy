package finance.services

import finance.Application
import finance.domain.*
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.TransactionRepository
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.Specification

import java.sql.Timestamp
import java.time.LocalDate

@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
class CalculationServiceIntSpec extends Specification {

    private static final String TEST_OWNER = "test-calc-user"

    @Autowired
    CalculationService calculationService

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    AccountRepository accountRepository

    @Autowired
    CategoryRepository categoryRepository

    @Autowired
    EntityManager entityManager

    @Autowired
    PlatformTransactionManager transactionManager

    Account testAccount
    Category testCategory

    @Transactional
    void setup() {
        // Set SecurityContext so TenantContext.getCurrentOwner() works
        def authorities = [new SimpleGrantedAuthority("USER")]
        def auth = new UsernamePasswordAuthenticationToken(TEST_OWNER, "N/A", authorities)
        SecurityContextHolder.getContext().setAuthentication(auth)

        // Create test category first (required due to FK constraint)
        testCategory = new Category(
            0L,                           // categoryId
            TEST_OWNER,                   // owner
            true,                         // activeStatus
            "testcategory"                // categoryName
        )
        testCategory.dateUpdated = new Timestamp(System.currentTimeMillis())
        testCategory.dateAdded = new Timestamp(System.currentTimeMillis())
        testCategory = categoryRepository.save(testCategory)

        // CRITICAL: Provide ALL constructor parameters when calling Kotlin data classes from Groovy
        testAccount = new Account(
            0L,                           // accountId
            TEST_OWNER,                   // owner
            "calc_checking",              // accountNameOwner (must match pattern: ^[a-z-]*_[a-z]*$)
            AccountType.Credit,           // accountType
            true,                         // activeStatus
            "9876",                       // moniker
            BigDecimal.ZERO,              // outstanding
            BigDecimal.ZERO,              // future
            BigDecimal.ZERO               // cleared
        )
        testAccount.dateUpdated = new Timestamp(System.currentTimeMillis())
        testAccount.dateAdded = new Timestamp(System.currentTimeMillis())
        testAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        testAccount.validationDate = new Timestamp(System.currentTimeMillis())

        testAccount = accountRepository.save(testAccount)
        entityManager.flush()
    }

    @Transactional
    void cleanup() {
        // Clean up test data
        transactionRepository.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc("calc_checking", true).each {
            transactionRepository.delete(it)
        }
        if (testAccount != null && testAccount.accountId != 0L) {
            accountRepository.delete(testAccount)
        }
        if (testCategory != null && testCategory.categoryId != 0L) {
            categoryRepository.delete(testCategory)
        }
        SecurityContextHolder.clearContext()
    }

    @Transactional
    void 'test calculateActiveTotalsByAccountNameOwner with no transactions'() {
        when:
        Totals totals = calculationService.calculateActiveTotalsByAccountNameOwner("calc_checking")

        then:
        totals != null
        totals.totals == BigDecimal.ZERO.setScale(2)
        totals.totalsCleared == BigDecimal.ZERO.setScale(2)
        totals.totalsFuture == BigDecimal.ZERO.setScale(2)
        totals.totalsOutstanding == BigDecimal.ZERO.setScale(2)
    }

@Transactional
    void 'test calculateTotalsFromTransactions with empty list'() {
        when:
        Map<TransactionState, BigDecimal> result = calculationService.calculateTotalsFromTransactions([])

        then:
        result != null
        result.isEmpty()
    }

@Transactional
    void 'test calculateTotalsFromTransactions with transactions'() {
        given:
        List<Transaction> transactions = [
            createTransaction("txn1", 100.00, TransactionState.Cleared),
            createTransaction("txn2", 200.00, TransactionState.Cleared),
            createTransaction("txn3", 50.00, TransactionState.Future)
        ]

        when:
        Map<TransactionState, BigDecimal> result = calculationService.calculateTotalsFromTransactions(transactions)

        then:
        result != null
        result[TransactionState.Cleared] == new BigDecimal("300.00").setScale(2)
        result[TransactionState.Future] == new BigDecimal("50.00").setScale(2)
    }

@Transactional
    void 'test calculateGrandTotal with empty map'() {
        when:
        BigDecimal grandTotal = calculationService.calculateGrandTotal([:])

        then:
        grandTotal == BigDecimal.ZERO.setScale(2)
    }

@Transactional
    void 'test calculateGrandTotal with multiple states'() {
        given:
        Map<TransactionState, BigDecimal> totalsMap = [
            (TransactionState.Cleared): new BigDecimal("100.00"),
            (TransactionState.Future): new BigDecimal("200.00"),
            (TransactionState.Outstanding): new BigDecimal("50.00")
        ]

        when:
        BigDecimal grandTotal = calculationService.calculateGrandTotal(totalsMap)

        then:
        grandTotal == new BigDecimal("350.00").setScale(2)
    }

@Transactional
    void 'test createTotals'() {
        when:
        Totals totals = calculationService.createTotals(
            new BigDecimal("100.00"),
            new BigDecimal("200.00"),
            new BigDecimal("50.00")
        )

        then:
        totals != null
        totals.totalsFuture == new BigDecimal("100.00")
        totals.totalsCleared == new BigDecimal("200.00")
        totals.totalsOutstanding == new BigDecimal("50.00")
        totals.totals == new BigDecimal("350.00").setScale(2)
    }

@Transactional
    void 'test validateTotals with valid totals'() {
        given:
        // CRITICAL: Provide ALL constructor parameters when calling Kotlin data classes from Groovy
        Totals validTotals = new Totals(
            new BigDecimal("100.00"),      // totalsFuture
            new BigDecimal("200.00"),      // totalsCleared
            new BigDecimal("350.00"),      // totals
            new BigDecimal("50.00")        // totalsOutstanding
        )

        when:
        boolean isValid = calculationService.validateTotals(validTotals)

        then:
        isValid == true
    }

@Transactional
    void 'test validateTotals with invalid grand total'() {
        given:
        // CRITICAL: Provide ALL constructor parameters when calling Kotlin data classes from Groovy
        Totals invalidTotals = new Totals(
            new BigDecimal("100.00"),      // totalsFuture
            new BigDecimal("200.00"),      // totalsCleared
            new BigDecimal("999.99"),      // totals (Wrong total - should be 350.00)
            new BigDecimal("50.00")        // totalsOutstanding
        )

        when:
        boolean isValid = calculationService.validateTotals(invalidTotals)

        then:
        isValid == false
    }

@Transactional
    void 'test validateTotals with excessive amounts'() {
        given:
        // CRITICAL: Provide ALL constructor parameters when calling Kotlin data classes from Groovy
        Totals excessiveTotals = new Totals(
            new BigDecimal("9999999999.99"),     // totalsFuture (excessive)
            new BigDecimal("200.00"),            // totalsCleared
            new BigDecimal("10000000250.49"),    // totals
            new BigDecimal("50.00")              // totalsOutstanding
        )

        when:
        boolean isValid = calculationService.validateTotals(excessiveTotals)

        then:
        isValid == false
    }

    private Transaction createTransaction(String description, double amount, TransactionState state) {
        // Use no-arg constructor and property assignment for Kotlin data class interop
        Transaction transaction = new Transaction()
        transaction.owner = TEST_OWNER
        transaction.guid = UUID.randomUUID().toString()
        transaction.accountNameOwner = "calc_checking"
        transaction.accountId = testAccount.accountId
        transaction.accountType = AccountType.Credit
        transaction.description = description
        transaction.category = "testcategory"
        transaction.amount = new BigDecimal(amount)
        transaction.transactionDate = LocalDate.now()
        transaction.transactionState = state
        transaction.transactionType = TransactionType.Expense
        transaction.activeStatus = true
        transaction.notes = ""
        transaction.dateUpdated = new Timestamp(System.currentTimeMillis())
        transaction.dateAdded = new Timestamp(System.currentTimeMillis())
        return transaction
    }
}
