package finance.services

import finance.domain.Transaction
import finance.domain.TransactionState
import finance.repositories.TransactionRepository
import spock.lang.Specification

import java.math.BigDecimal

class CalculationServiceSpec extends Specification {

    protected static final String TEST_OWNER = "test_owner"

    def repo = Mock(TransactionRepository)

    CalculationService service
    def registry

    def setup() {
        def auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(TEST_OWNER, "password")
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth)

        service = new CalculationService(repo)
        // Provide a meter service with a real registry for counter assertions
        registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        service.meterService = new MeterService(registry, null)
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

    def "calculateActiveTotalsByAccountNameOwner wraps SQLException as DataAccessResourceFailureException via resilience"() {
        given: 'enable resilience path in BaseService'
        def cfg = new finance.configurations.DatabaseResilienceConfiguration()
        service.databaseResilienceConfig = cfg
        service.circuitBreaker = cfg.databaseCircuitBreaker()
        service.retry = cfg.databaseRetry()
        service.timeLimiter = cfg.databaseTimeLimiter()
        service.scheduledExecutorService = cfg.scheduledExecutorService()

        and: 'repository throws a SQLException inside the resilient operation'
        def account = 'resilience_case'
        repo.sumTotalsForActiveTransactionsByOwnerAndAccountNameOwner(TEST_OWNER, account) >> { throw new java.sql.SQLException('db down') }

        when:
        service.calculateActiveTotalsByAccountNameOwner(account)

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
}
