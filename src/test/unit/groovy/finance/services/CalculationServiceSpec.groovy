package finance.services

import finance.domain.Totals
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.helpers.TransactionBuilder
import spock.lang.Shared
import spock.lang.Unroll

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * TDD Specification for CalculationService
 * Tests the extracted calculation functionality from TransactionService
 */
class CalculationServiceSpec extends BaseServiceSpec {

    protected CalculationService calculationService = new CalculationService(transactionRepositoryMock)

    @Shared
    List<Transaction> testTransactions

    void setup() {
        calculationService.meterService = meterService
        calculationService.validator = validatorMock
    }

    def setupSpec() {
        // Create test transactions for calculation testing
        testTransactions = createTestTransactions()
    }

    // ===== TDD Tests for calculateActiveTotalsByAccountNameOwner() =====

    def "should calculate totals by account name using repository aggregation"() {
        given: "an account name and mocked repository result"
        String accountName = "test_account"
        // Mock repository result: [amount, id, transactionState]
        List<Object[]> mockResult = [
            [new BigDecimal("100.50"), 1, "future"] as Object[],
            [new BigDecimal("250.75"), 1, "cleared"] as Object[],
            [new BigDecimal("-50.25"), 1, "outstanding"] as Object[]
        ]

        when: "calculating totals"
        Totals result = calculationService.calculateActiveTotalsByAccountNameOwner(accountName)

        then: "repository is called and totals are calculated correctly"
        1 * transactionRepositoryMock.sumTotalsForActiveTransactionsByAccountNameOwner(accountName) >> mockResult
        result != null
        result.totalsFuture == new BigDecimal("100.50")
        result.totalsCleared == new BigDecimal("250.75")
        result.totalsOutstanding == new BigDecimal("-50.25")
        result.totals == new BigDecimal("301.00") // Grand total: 100.50 + 250.75 + (-50.25) = 301.00
        0 * _
    }

    def "should handle empty repository result gracefully"() {
        given: "an account name with no transactions"
        String accountName = "empty_account"
        List<Object[]> emptyResult = []

        when: "calculating totals"
        Totals result = calculationService.calculateActiveTotalsByAccountNameOwner(accountName)

        then: "all totals should be zero"
        1 * transactionRepositoryMock.sumTotalsForActiveTransactionsByAccountNameOwner(accountName) >> emptyResult
        result != null
        result.totalsFuture == BigDecimal.ZERO
        result.totalsCleared == BigDecimal.ZERO
        result.totalsOutstanding == BigDecimal.ZERO
        result.totals == BigDecimal.ZERO
        0 * _
    }

    def "should handle null values in repository result"() {
        given: "repository result with null values"
        String accountName = "test_account"
        List<Object[]> resultWithNulls = [
            [null, 1, "future"] as Object[],
            [new BigDecimal("100.00"), 1, "cleared"] as Object[],
            [new BigDecimal("50.00"), 1, null] as Object[]
        ]

        when: "calculating totals"
        Totals result = calculationService.calculateActiveTotalsByAccountNameOwner(accountName)

        then: "null values are handled gracefully"
        1 * transactionRepositoryMock.sumTotalsForActiveTransactionsByAccountNameOwner(accountName) >> resultWithNulls
        result != null
        result.totalsFuture == BigDecimal.ZERO // null handled
        result.totalsCleared == new BigDecimal("100.00")
        result.totalsOutstanding == BigDecimal.ZERO // unknown state handled
        result.totals == new BigDecimal("100.00")
        0 * _
    }

    // ===== TDD Tests for calculateTotalsFromTransactions() =====

    def "should calculate totals from transaction list correctly"() {
        given: "a list of transactions with different states"
        List<Transaction> transactions = [
            createTransaction(TransactionState.Future, new BigDecimal("50.00")),
            createTransaction(TransactionState.Future, new BigDecimal("25.00")),
            createTransaction(TransactionState.Cleared, new BigDecimal("100.00")),
            createTransaction(TransactionState.Outstanding, new BigDecimal("-30.00"))
        ]

        when: "calculating totals from transactions"
        Map<TransactionState, BigDecimal> result = calculationService.calculateTotalsFromTransactions(transactions)

        then: "totals are calculated correctly by state"
        result[TransactionState.Future] == new BigDecimal("75.00")
        result[TransactionState.Cleared] == new BigDecimal("100.00")
        result[TransactionState.Outstanding] == new BigDecimal("-30.00")
        result.size() == 3
    }

    def "should handle empty transaction list"() {
        given: "an empty list of transactions"
        List<Transaction> emptyTransactions = []

        when: "calculating totals"
        Map<TransactionState, BigDecimal> result = calculationService.calculateTotalsFromTransactions(emptyTransactions)

        then: "result should be empty map"
        result != null
        result.isEmpty()
    }

    // ===== TDD Tests for calculateGrandTotal() =====

    @Unroll
    def "should calculate grand total correctly for #description"() {
        given: "totals map with various amounts"
        Map<TransactionState, BigDecimal> totalsMap = inputTotals

        when: "calculating grand total"
        BigDecimal result = calculationService.calculateGrandTotal(totalsMap)

        then: "grand total is calculated with proper rounding"
        result == expectedTotal

        where:
        description              | inputTotals                                                                                           | expectedTotal
        "positive amounts"       | [(TransactionState.Future): new BigDecimal("100.50"), (TransactionState.Cleared): new BigDecimal("200.25")] | new BigDecimal("300.75")
        "mixed positive/negative"| [(TransactionState.Future): new BigDecimal("100.00"), (TransactionState.Outstanding): new BigDecimal("-25.50")] | new BigDecimal("74.50")
        "rounding required"      | [(TransactionState.Future): new BigDecimal("100.333"), (TransactionState.Cleared): new BigDecimal("200.666")] | new BigDecimal("301.00")
        "empty map"              | [:]                                                                                                   | BigDecimal.ZERO
    }

    // ===== TDD Tests for createTotals() =====

    def "should create Totals object with calculated grand total"() {
        given: "individual total amounts"
        BigDecimal totalsFuture = new BigDecimal("150.50")
        BigDecimal totalsCleared = new BigDecimal("300.25")
        BigDecimal totalsOutstanding = new BigDecimal("-25.75")

        when: "creating Totals object"
        Totals result = calculationService.createTotals(totalsFuture, totalsCleared, totalsOutstanding)

        then: "Totals object is created with correct grand total"
        result != null
        result.totalsFuture == totalsFuture
        result.totalsCleared == totalsCleared
        result.totalsOutstanding == totalsOutstanding
        result.totals == new BigDecimal("425.00") // 150.50 + 300.25 + (-25.75) = 425.00
    }

    // ===== TDD Tests for validateTotals() =====

    @Unroll
    def "should validate totals correctly for #description"() {
        given: "a Totals object with various values"
        Totals totals = inputTotals

        when: "validating totals"
        boolean result = calculationService.validateTotals(totals)

        then: "validation result matches expectation"
        result == expectedResult

        where:
        description           | inputTotals                                                                                                        | expectedResult
        "valid positive totals" | new Totals(new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("350.00"), new BigDecimal("50.00")) | true
        "valid with negatives"  | new Totals(new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("250.00"), new BigDecimal("-50.00")) | true
        "invalid grand total"   | new Totals(new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("400.00"), new BigDecimal("50.00")) | false  // Grand total doesn't match sum
        "extremely large values"| new Totals(new BigDecimal("999999999.99"), new BigDecimal("1.00"), new BigDecimal("1000000000.99"), BigDecimal.ZERO) | false  // Beyond reasonable limits
    }

    // ===== TDD Tests for Integration with Existing TransactionService Behavior =====

    def "should maintain compatibility with existing transaction totals calculation"() {
        given: "repository result matching existing TransactionService format"
        String accountName = "compatibility_test"
        List<Object[]> repositoryResult = [
            [new BigDecimal("150.00"), 1, "future"] as Object[],
            [new BigDecimal("350.50"), 1, "cleared"] as Object[],
            [new BigDecimal("75.25"), 1, "outstanding"] as Object[]
        ]

        when: "calculating totals using new service"
        Totals result = calculationService.calculateActiveTotalsByAccountNameOwner(accountName)

        then: "result matches existing TransactionService behavior"
        1 * transactionRepositoryMock.sumTotalsForActiveTransactionsByAccountNameOwner(accountName) >> repositoryResult
        result != null

        // Verify each component matches expected format
        result.totalsFuture.scale() <= 2
        result.totalsCleared.scale() <= 2
        result.totalsOutstanding.scale() <= 2
        result.totals.scale() <= 2

        // Verify grand total calculation
        BigDecimal expectedGrandTotal = result.totalsFuture + result.totalsCleared + result.totalsOutstanding
        result.totals == expectedGrandTotal.setScale(2, RoundingMode.HALF_UP)
        0 * _
    }

    // ===== Test Data Helper Methods =====

    private List<Transaction> createTestTransactions() {
        return [
            createTransaction(TransactionState.Future, new BigDecimal("100.00")),
            createTransaction(TransactionState.Cleared, new BigDecimal("200.00")),
            createTransaction(TransactionState.Outstanding, new BigDecimal("-50.00"))
        ]
    }

    private Transaction createTransaction(TransactionState state, BigDecimal amount) {
        return TransactionBuilder.builder()
            .withTransactionState(state)
            .withAmount(amount)
            .withAccountNameOwner("test_account")
            .build()
    }
}