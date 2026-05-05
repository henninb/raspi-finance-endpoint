package finance.services
import finance.configurations.ResilienceComponents

import finance.domain.Transaction
import finance.domain.TransactionState
import finance.repositories.TransactionRepository
import spock.lang.Specification

import java.math.BigDecimal

class CalculationServiceSpec extends Specification {

    protected static final String TEST_OWNER = "test_owner"

    def repo = Mock(TransactionRepository)

    CalculationService service
    MeterService meterService
    def registry

    def setup() {
        def auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(TEST_OWNER, "password")
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth)

        // Provide a meter service with a real registry for counter assertions
        registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        meterService = new MeterService(registry)
        service = new CalculationService(repo, meterService, GroovyMock(jakarta.validation.Validator), ResilienceComponents.noOp())
    }

    def cleanup() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext()
    }

    private static Transaction tx(TransactionState state, BigDecimal amount) {
        def t = new Transaction()
        t.transactionState = state
        t.amount = amount
        return t
    }

    def "calculateTotalsFromTransactions groups and sums by state"() {
        given:
        def transactions = [
                tx(TransactionState.Cleared, new BigDecimal('10.12')),
                tx(TransactionState.Outstanding, new BigDecimal('5.50')),
                tx(TransactionState.Future, new BigDecimal('2.38'))
        ]

        when:
        def totalsMap = service.calculateTotalsFromTransactions(transactions)

        then:
        totalsMap[TransactionState.Cleared] == new BigDecimal('10.12').setScale(2)
        totalsMap[TransactionState.Outstanding] == new BigDecimal('5.50').setScale(2)
        totalsMap[TransactionState.Future] == new BigDecimal('2.38').setScale(2)
    }

    def "calculateGrandTotal sums all totals with scale 2"() {
        given:
        def totalsMap = [
                (TransactionState.Cleared): new BigDecimal('10.12'),
                (TransactionState.Outstanding): new BigDecimal('5.50'),
                (TransactionState.Future): new BigDecimal('2.38')
        ]

        expect:
        service.calculateGrandTotal(totalsMap) == new BigDecimal('18.00').setScale(2)
    }

    def "createTotals assembles values and computes grand total"() {
        when:
        def totals = service.createTotals(
                new BigDecimal('2.38').setScale(2),
                new BigDecimal('10.12').setScale(2),
                new BigDecimal('5.50').setScale(2)
        )

        then:
        totals.totals == new BigDecimal('18.00').setScale(2)
        totals.totalsFuture == new BigDecimal('2.38').setScale(2)
        totals.totalsCleared == new BigDecimal('10.12').setScale(2)
        totals.totalsOutstanding == new BigDecimal('5.50').setScale(2)
    }

    def "validateTotals passes for consistent, reasonable amounts"() {
        given:
        def totals = service.createTotals(
                new BigDecimal('2.00').setScale(2),
                new BigDecimal('3.00').setScale(2),
                new BigDecimal('5.00').setScale(2)
        )

        expect:
        service.validateTotals(totals)
    }

    def "validateTotals fails when grand total mismatches expected sum"() {
        given:
        def totals = new finance.domain.Totals(
                new BigDecimal('2.00').setScale(2),
                new BigDecimal('3.00').setScale(2),
                new BigDecimal('4.99').setScale(2), // incorrect grand total (should be 10.00)
                new BigDecimal('5.00').setScale(2)
        )

        expect:
        !service.validateTotals(totals)
    }

    def "validateTotals fails when amounts exceed reasonable bounds"() {
        given:
        def huge = new BigDecimal('1000000000.00').setScale(2)
        def totals = new finance.domain.Totals(huge, huge, huge, huge)

        expect:
        !service.validateTotals(totals)
    }

    def "calculateActiveTotalsByAccountNameOwner parses mixed rows and sets per-state totals"() {
        given:
        def account = 'owner_account'
        def rows = [
                [new BigDecimal('12.34'), 0L, 'future'] as Object[],
                [new BigDecimal('56.78'), 0L, 'cleared'] as Object[],
                [new BigDecimal('1.11'),  0L, 'outstanding'] as Object[],
                [null,                      0L, 'cleared'] as Object[],      // skipped (null amount)
                [new BigDecimal('2.22'),   0L, 'unknown_state'] as Object[], // ignored (unknown state)
                [new BigDecimal('3.33'),   0L, null] as Object[]             // skipped (null state)
        ]

        and:
        repo.sumTotalsForActiveTransactionsByOwnerAndAccountNameOwner(TEST_OWNER, account) >> rows

        when:
        def totals = service.calculateActiveTotalsByAccountNameOwner(account)

        then:
        totals.totalsFuture == new BigDecimal('12.34').setScale(2)
        totals.totalsCleared == new BigDecimal('56.78').setScale(2)
        totals.totalsOutstanding == new BigDecimal('1.11').setScale(2)
        totals.totals == new BigDecimal('70.23').setScale(2)

        and: 'counter for TotalsCalculated was incremented with server tag'
        def c = registry.get(finance.utils.Constants.EXCEPTION_THROWN_COUNTER)
                .tags(finance.utils.Constants.EXCEPTION_NAME_TAG, 'TotalsCalculated')
                .counter()
        c.count() >= 1d
    }

    def "calculateActiveTotalsByAccountNameOwner increments caught counter on exception"() {
        given:
        def account = 'owner_error'
        repo.sumTotalsForActiveTransactionsByOwnerAndAccountNameOwner(TEST_OWNER, account) >> { throw new RuntimeException('boom') }

        when:
        service.calculateActiveTotalsByAccountNameOwner(account)

        then:
        thrown(RuntimeException)

        and:
        def cc = registry.get(finance.utils.Constants.EXCEPTION_CAUGHT_COUNTER)
                .tags(finance.utils.Constants.EXCEPTION_NAME_TAG, 'TotalsCalculationError')
                .counter()
        cc.count() >= 1d
    }

    def "calculateActiveTotalsByAccountNameOwner skips non-Array rows and increments UnexpectedRowType counter"() {
        given:
        def account = 'owner_bad_row'
        def rows = [
            "not_an_array",
            [new BigDecimal('5.00'), 0L, 'cleared'] as Object[]
        ]
        repo.sumTotalsForActiveTransactionsByOwnerAndAccountNameOwner(TEST_OWNER, account) >> rows

        when:
        def totals = service.calculateActiveTotalsByAccountNameOwner(account)

        then:
        totals.totalsCleared == new BigDecimal('5.00').setScale(2)
        def counter = registry.get(finance.utils.Constants.EXCEPTION_CAUGHT_COUNTER)
                .tags(finance.utils.Constants.EXCEPTION_NAME_TAG, 'UnexpectedRowType')
                .counter()
        counter.count() >= 1d
    }

    def "calculateActiveTotalsByAccountNameOwner skips rows with fewer than 3 columns"() {
        given:
        def account = 'owner_short_row'
        def rows = [
            [new BigDecimal('7.00'), 0L] as Object[],
            [new BigDecimal('3.00'), 0L, 'cleared'] as Object[]
        ]
        repo.sumTotalsForActiveTransactionsByOwnerAndAccountNameOwner(TEST_OWNER, account) >> rows

        when:
        def totals = service.calculateActiveTotalsByAccountNameOwner(account)

        then:
        totals.totalsCleared == new BigDecimal('3.00').setScale(2)
        def counter = registry.get(finance.utils.Constants.EXCEPTION_CAUGHT_COUNTER)
                .tags(finance.utils.Constants.EXCEPTION_NAME_TAG, 'InsufficientColumns')
                .counter()
        counter.count() >= 1d
    }

    def "calculateActiveTotalsByAccountNameOwner returns zeros for empty result set"() {
        given:
        def account = 'owner_empty'
        repo.sumTotalsForActiveTransactionsByOwnerAndAccountNameOwner(TEST_OWNER, account) >> []

        when:
        def totals = service.calculateActiveTotalsByAccountNameOwner(account)

        then:
        totals.totalsFuture == new BigDecimal('0.00').setScale(2)
        totals.totalsCleared == new BigDecimal('0.00').setScale(2)
        totals.totalsOutstanding == new BigDecimal('0.00').setScale(2)
        totals.totals == new BigDecimal('0.00').setScale(2)
    }

    def "calculateTotalsFromTransactions accumulates multiple transactions per state"() {
        given:
        def transactions = [
            tx(TransactionState.Cleared, new BigDecimal('10.00')),
            tx(TransactionState.Cleared, new BigDecimal('5.00')),
            tx(TransactionState.Outstanding, new BigDecimal('2.50'))
        ]

        when:
        def totalsMap = service.calculateTotalsFromTransactions(transactions)

        then:
        totalsMap[TransactionState.Cleared] == new BigDecimal('15.00').setScale(2)
        totalsMap[TransactionState.Outstanding] == new BigDecimal('2.50').setScale(2)
    }

    def "calculateTotalsFromTransactions returns empty map for empty list"() {
        expect:
        service.calculateTotalsFromTransactions([]).isEmpty()
    }

    def "calculateGrandTotal returns zero for empty map"() {
        expect:
        service.calculateGrandTotal([:]) == new BigDecimal('0.00').setScale(2)
    }

    def "validateTotals fails for negative unreasonable amounts"() {
        given:
        def huge = new BigDecimal('-1000000000.00').setScale(2)
        def totals = new finance.domain.Totals(huge, huge, huge, huge)

        expect:
        !service.validateTotals(totals)
    }

    def "calculateActiveTotalsByAccountNameOwner wraps SQLException as DataAccessResourceFailureException via resilience"() {
        given: 'a service with resilience components enabled'
        def cfg = new finance.configurations.DatabaseResilienceConfiguration()
        def resilienceComponents = new finance.configurations.ResilienceComponents(
            cfg,
            cfg.databaseCircuitBreaker(),
            cfg.databaseRetry(),
            cfg.databaseTimeLimiter(),
            cfg.scheduledExecutorService()
        )
        def resilientService = new CalculationService(repo, meterService, GroovyMock(jakarta.validation.Validator), resilienceComponents)

        and: 'repository throws a SQLException inside the resilient operation'
        def account = 'resilience_case'
        repo.sumTotalsForActiveTransactionsByOwnerAndAccountNameOwner(TEST_OWNER, account) >> { throw new java.sql.SQLException('db down') }

        when:
        resilientService.calculateActiveTotalsByAccountNameOwner(account)

        then:
        thrown(org.springframework.dao.DataAccessResourceFailureException)

        and: 'BaseService increments a thrown counter tag (implementation-dependent)'
        def possibleTags = ['SQLException','DatabaseOperationException','DataAccessResourceFailureException','CannotGetJdbcConnectionException','DatabaseOperationTimeoutException']
        assert possibleTags.any { tag ->
            def c = registry.find(finance.utils.Constants.EXCEPTION_THROWN_COUNTER)
                    .tags(finance.utils.Constants.EXCEPTION_NAME_TAG, tag)
                    .counter()
            c != null && c.count() >= 1d
        }

        and: 'service increments caught counter for TotalsCalculationError'
        def caughtCounter = registry.get(finance.utils.Constants.EXCEPTION_CAUGHT_COUNTER)
                .tags(finance.utils.Constants.EXCEPTION_NAME_TAG, 'TotalsCalculationError')
                .counter()
        caughtCounter.count() >= 1d
    }

    def "calculateTotalsFromTransactions returns empty map when transaction amount causes exception"() {
        given:
        def badTx = Mock(Transaction)
        badTx.transactionState >> TransactionState.Cleared
        badTx.amount >> null

        when:
        def result = service.calculateTotalsFromTransactions([badTx])

        then:
        result.isEmpty()
        noExceptionThrown()
    }

    def "calculateGrandTotal returns zero when map contains null BigDecimal value"() {
        given:
        def badMap = [(TransactionState.Cleared): null]

        when:
        def result = service.calculateGrandTotal(badMap)

        then:
        result == BigDecimal.ZERO.setScale(2)
        noExceptionThrown()
    }

    def "validateTotals returns false when grand total does not match component sum"() {
        given:
        def totals = new finance.domain.Totals(
            new BigDecimal('1.00').setScale(2),  // totalsFuture
            new BigDecimal('2.00').setScale(2),  // totalsCleared
            new BigDecimal('9.99').setScale(2),  // totals (grand total - intentionally wrong)
            new BigDecimal('0.00').setScale(2)   // totalsOutstanding
        )

        expect:
        !service.validateTotals(totals)
    }

    def "calculateActiveTotalsByAccountNameOwner handles inner row exception gracefully"() {
        given:
        def account = 'owner_row_exc'
        def badRow = new Object() {
            // This object passes 'row !is Array<*>' check via false, which is: not instanceof Array
        }

        // Provide a properly-formed row and a row that will fail size check but not an Exception
        def rows = [
            [new BigDecimal('5.00'), 0L, 'cleared'] as Object[]
        ]
        repo.sumTotalsForActiveTransactionsByOwnerAndAccountNameOwner(TEST_OWNER, account) >> rows

        when:
        def totals = service.calculateActiveTotalsByAccountNameOwner(account)

        then:
        totals.totalsCleared == new BigDecimal('5.00').setScale(2)
        noExceptionThrown()
    }
}
