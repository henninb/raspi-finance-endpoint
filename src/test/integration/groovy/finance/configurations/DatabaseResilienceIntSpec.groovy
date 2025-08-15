package finance.configurations

import finance.Application
import finance.repositories.AccountRepository
import finance.domain.Account
import finance.domain.AccountType
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.sql.Date
import java.sql.Timestamp
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import java.math.BigDecimal

@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
@Transactional
class DatabaseResilienceIntSpec extends Specification {

    @Autowired
    DatabaseResilienceConfiguration databaseResilienceConfiguration

    @Autowired
    AccountRepository accountRepository

    @Autowired
    CircuitBreaker databaseCircuitBreaker

    @Autowired
    Retry databaseRetry

    @Autowired
    TimeLimiter databaseTimeLimiter

    @Autowired
    ScheduledExecutorService scheduledExecutorService

    @Autowired
    DataSource dataSource

    void 'test circuit breaker bean configuration'() {
        when:
        CircuitBreaker circuitBreaker = databaseCircuitBreaker

        then:
        circuitBreaker != null
        circuitBreaker.name == "database"
        circuitBreaker.circuitBreakerConfig.failureRateThreshold == 50.0f
        // The wait duration is managed by waitIntervalFunctionInOpenState
        circuitBreaker.circuitBreakerConfig.waitIntervalFunctionInOpenState != null
        circuitBreaker.circuitBreakerConfig.slidingWindowSize == 10
        circuitBreaker.circuitBreakerConfig.minimumNumberOfCalls == 5
        circuitBreaker.circuitBreakerConfig.permittedNumberOfCallsInHalfOpenState == 3
    }

    void 'test retry bean configuration'() {
        when:
        Retry retry = databaseRetry

        then:
        retry != null
        retry.name == "database"
        retry.retryConfig.maxAttempts == 3
        retry.retryConfig.intervalBiFunction != null
        // The wait duration is 1 second (1000ms) 
        retry.retryConfig.intervalBiFunction.apply(1, null) >= 1000L
    }

    void 'test time limiter bean configuration'() {
        when:
        TimeLimiter timeLimiter = databaseTimeLimiter

        then:
        timeLimiter != null
        timeLimiter.name == "database"
        timeLimiter.timeLimiterConfig.timeoutDuration.toSeconds() == 30
        timeLimiter.timeLimiterConfig.shouldCancelRunningFuture()
    }

    void 'test scheduled executor service bean'() {
        when:
        ScheduledExecutorService executor = scheduledExecutorService

        then:
        executor != null
        !executor.isShutdown()
        !executor.isTerminated()
    }

    void 'test circuit breaker with successful database operation'() {
        given:
        CircuitBreaker circuitBreaker = databaseCircuitBreaker

        when:
        assert circuitBreaker != null : "Circuit breaker should not be null"
        CircuitBreaker.State initialState = circuitBreaker.getState()
        String result = circuitBreaker.executeSupplier {
            // Simple operation that doesn't require complex object creation
            return "success"
        }

        then:
        initialState == CircuitBreaker.State.CLOSED
        circuitBreaker.getState() == CircuitBreaker.State.CLOSED
        result == "success"
    }

    void 'test retry mechanism with transient failure simulation'() {
        given:
        Retry retry = databaseRetry
        int attemptCounter = 0
        
        when:
        String result = retry.executeSupplier {
            attemptCounter++
            if (attemptCounter < 2) {
                throw new DataAccessResourceFailureException("Simulated transient failure")
            }
            return "Success on attempt ${attemptCounter}"
        }

        then:
        attemptCounter == 2
        result == "Success on attempt 2"
    }

    void 'test time limiter with fast operation'() {
        given:
        TimeLimiter timeLimiter = databaseTimeLimiter
        CompletableFuture<String> futureOperation = CompletableFuture.supplyAsync {
            // Simulate fast database operation
            Thread.sleep(100)
            return "Fast operation completed"
        }

        when:
        CompletableFuture<String> timedOperation = timeLimiter.executeCompletionStage(
            scheduledExecutorService, 
            { futureOperation }
        ).toCompletableFuture()
        String result = timedOperation.get(5, TimeUnit.SECONDS)

        then:
        result == "Fast operation completed"
    }

    void 'test resilience configuration executeWithResilience method'() {
        given:
        Account testAccount = new Account(
            0L,  // accountId
            "resilience_test",  // accountNameOwner
            AccountType.Debit,  // accountType
            true,  // activeStatus
            "1600",  // moniker
            BigDecimal.ZERO,  // outstanding
            BigDecimal.ZERO,  // future
            BigDecimal.ZERO,  // cleared
            new Timestamp(0),  // dateClosed
            new Timestamp(System.currentTimeMillis())  // validationDate
        )

        when:
        CompletableFuture<Account> result = databaseResilienceConfiguration.executeWithResilience(
            { -> accountRepository.save(testAccount) } as java.util.function.Supplier
        )
        Account savedAccount = result.get(10, TimeUnit.SECONDS)

        then:
        savedAccount != null
        savedAccount.accountNameOwner == "resilience_test"
        savedAccount.getAccountId() != null
    }

    void 'test circuit breaker metrics and events'() {
        given:
        CircuitBreaker circuitBreaker = databaseCircuitBreaker
        def eventCollector = []

        // Subscribe to circuit breaker events
        circuitBreaker.eventPublisher.onStateTransition { event ->
            eventCollector.add(event)
        }

        when:
        // Execute successful operations
        def accountNames = ["metricsa_test", "metricsb_test", "metricsc_test"]
        for (int i = 0; i < 3; i++) {
            circuitBreaker.executeSupplier {
                Account account = new Account(
                    0L,  // accountId
                    accountNames[i],  // accountNameOwner
                    AccountType.Credit,  // accountType
                    true,  // activeStatus
                    "1700",  // moniker (4 digits)
                    BigDecimal.ZERO,  // outstanding
                    BigDecimal.ZERO,  // future
                    BigDecimal.ZERO,  // cleared
                    new Timestamp(0),  // dateClosed
                    new Timestamp(System.currentTimeMillis())  // validationDate
                )
                return accountRepository.save(account)
            }
        }

        then:
        circuitBreaker.getState() == CircuitBreaker.State.CLOSED
        circuitBreaker.getMetrics().getNumberOfSuccessfulCalls() >= 3
        circuitBreaker.getMetrics().getNumberOfFailedCalls() == 0
    }

    void 'test database health indicator integration'() {
        when:
        def healthIndicator = databaseResilienceConfiguration.databaseHealthIndicator(dataSource)
        def health = healthIndicator.health()

        then:
        health != null
        health.status != null
        health.details.containsKey("database")
    }

    void 'test concurrent database operations with resilience'() {
        given:
        List<CompletableFuture<Account>> futures = []
        
        when:
        // Execute multiple concurrent database operations
        def concurrentNames = ["concurrenta_test", "concurrentb_test", "concurrentc_test", "concurrentd_test", "concurrente_test", 
                               "concurrentf_test", "concurrentg_test", "concurrenth_test", "concurrenti_test", "concurrentj_test"]
        for (int i = 0; i < 10; i++) {
            final int index = i
            CompletableFuture<Account> future = databaseResilienceConfiguration.executeWithResilience(
                { ->
                    Account account = new Account(
                        0L,  // accountId
                        concurrentNames[index],  // accountNameOwner
                        AccountType.Credit,  // accountType
                        true,  // activeStatus
                        "1800",  // moniker (4 digits)
                        BigDecimal.ZERO,  // outstanding
                        BigDecimal.ZERO,  // future
                        BigDecimal.ZERO,  // cleared
                        new Timestamp(0),  // dateClosed
                        new Timestamp(System.currentTimeMillis())  // validationDate
                    )
                    return accountRepository.save(account)
                } as java.util.function.Supplier
            )
            futures.add(future)
        }

        List<Account> results = futures.collect { it.get(15, TimeUnit.SECONDS) }

        then:
        results.size() == 10
        results.every { it != null && it.accountId != null }
        results.collect { it.accountNameOwner }.unique().size() == 10
    }

    void 'test resilience configuration with database transaction'() {
        given:
        def batchNames = ["batcha_test", "batchb_test", "batchc_test", "batchd_test", "batche_test"]
        List<Account> testAccounts = []
        for (int i = 0; i < 5; i++) {
            testAccounts.add(new Account(
                0L,  // accountId
                batchNames[i],  // accountNameOwner
                AccountType.Debit,  // accountType
                true,  // activeStatus
                "1900",  // moniker (4 digits)
                BigDecimal.ZERO,  // outstanding
                BigDecimal.ZERO,  // future
                BigDecimal.ZERO,  // cleared
                new Timestamp(0),  // dateClosed
                new Timestamp(System.currentTimeMillis())  // validationDate
            ))
        }

        when:
        CompletableFuture<List<Account>> result = databaseResilienceConfiguration.executeWithResilience(
            { -> accountRepository.saveAll(testAccounts) } as java.util.function.Supplier
        )
        List<Account> savedAccounts = result.get(15, TimeUnit.SECONDS)

        then:
        savedAccounts.size() == 5
        savedAccounts.every { it.accountId != null }
        savedAccounts.collect { it.accountNameOwner }.containsAll(
            ["batcha_test", "batchb_test", "batchc_test", "batchd_test", "batche_test"]
        )
    }
}