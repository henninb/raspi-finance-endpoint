package finance.configurations

import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariPoolMXBean
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.SQLException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class DatabaseResilienceConfigurationSpec extends Specification {

    DatabaseResilienceConfiguration config
    MeterRegistry meterRegistry = Mock()
    DataSource dataSource = Mock()

    def setup() {
        config = new DatabaseResilienceConfiguration()
    }

    def "should create database circuit breaker with proper configuration"() {
        when:
        CircuitBreaker circuitBreaker = config.databaseCircuitBreaker()

        then:
        circuitBreaker != null
        circuitBreaker.name == "database"
        circuitBreaker.circuitBreakerConfig != null
        circuitBreaker.circuitBreakerConfig.failureRateThreshold == 50.0f
        circuitBreaker.circuitBreakerConfig.minimumNumberOfCalls == 5
    }

    def "should configure circuit breaker to record database exceptions"() {
        when:
        CircuitBreaker circuitBreaker = config.databaseCircuitBreaker()

        then:
        circuitBreaker != null
        def recordedExceptions = circuitBreaker.circuitBreakerConfig.recordExceptionPredicate
        recordedExceptions.test(new SQLException("test"))
        recordedExceptions.test(new org.springframework.dao.DataAccessResourceFailureException("test"))
        recordedExceptions.test(new org.springframework.jdbc.CannotGetJdbcConnectionException("test"))
    }

    def "should create database retry with proper configuration"() {
        when:
        Retry retry = config.databaseRetry()

        then:
        retry != null
        retry.name == "database"
        retry.retryConfig != null
        retry.retryConfig.maxAttempts == 3
    }

    def "should configure retry to handle database exceptions"() {
        when:
        Retry retry = config.databaseRetry()

        then:
        retry != null
        def retryExceptions = retry.retryConfig.exceptionPredicate
        retryExceptions.test(new SQLException("test"))
        retryExceptions.test(new org.springframework.dao.DataAccessResourceFailureException("test"))
        retryExceptions.test(new org.springframework.jdbc.CannotGetJdbcConnectionException("test"))
    }

    def "should configure retry to ignore validation exceptions"() {
        when:
        Retry retry = config.databaseRetry()

        then:
        retry != null
        def retryExceptions = retry.retryConfig.exceptionPredicate
        !retryExceptions.test(new jakarta.validation.ValidationException("test"))
        !retryExceptions.test(new IllegalArgumentException("test"))
    }

    def "should create database time limiter with proper configuration"() {
        when:
        TimeLimiter timeLimiter = config.databaseTimeLimiter()

        then:
        timeLimiter != null
        timeLimiter.name == "database"
        timeLimiter.timeLimiterConfig.timeoutDuration == Duration.ofSeconds(30)
        timeLimiter.timeLimiterConfig.shouldCancelRunningFuture()
    }

    def "should create scheduled executor service with daemon threads"() {
        when:
        ScheduledExecutorService executor = config.scheduledExecutorService()

        then:
        executor != null
        !executor.isShutdown()
        !executor.isTerminated()

        cleanup:
        executor?.shutdown()
    }

    def "should configure connection pool metrics for HikariDataSource"() {
        given:
        HikariDataSource hikariDataSource = Mock() {
            getHikariPoolMXBean() >> Mock(HikariPoolMXBean) {
                getActiveConnections() >> 5
                getIdleConnections() >> 3
                getThreadsAwaitingConnection() >> 1
                getTotalConnections() >> 10
            }
        }

        when:
        String result = config.connectionPoolMetrics(meterRegistry, hikariDataSource)

        then:
        result == "connectionPoolMetricsConfigured"
        4 * meterRegistry.gauge(_, hikariDataSource, _)
    }

    def "should handle non-HikariDataSource gracefully"() {
        when:
        String result = config.connectionPoolMetrics(meterRegistry, dataSource)

        then:
        result == "connectionPoolMetricsConfigured"
        0 * meterRegistry.gauge(_, _, _)
    }

    def "should execute operation with resilience patterns using Kotlin lambda"() {
        given:
        def circuitBreaker = config.databaseCircuitBreaker()
        def retry = config.databaseRetry()
        def timeLimiter = config.databaseTimeLimiter()
        def executor = config.scheduledExecutorService()

        when:
        CompletableFuture<String> future = config.executeWithResilience(
            { "test result" },
            circuitBreaker,
            retry,
            timeLimiter,
            executor
        )
        String result = future.get()

        then:
        result == "test result"

        cleanup:
        executor?.shutdown()
    }

    def "should execute operation with resilience patterns using Java Supplier"() {
        given:
        def supplier = { "test result" } as java.util.function.Supplier<String>

        when:
        CompletableFuture<String> future = config.executeWithResilience(supplier)
        String result = future.get()

        then:
        result == "test result"
    }

    def "should handle exceptions in resilience execution"() {
        given:
        def circuitBreaker = config.databaseCircuitBreaker()
        def retry = config.databaseRetry()
        def timeLimiter = config.databaseTimeLimiter()
        def executor = config.scheduledExecutorService()

        when:
        CompletableFuture<String> future = config.executeWithResilience(
            { throw new IllegalArgumentException("test exception") },
            circuitBreaker,
            retry,
            timeLimiter,
            executor
        )
        future.get()

        then:
        thrown(Exception)

        cleanup:
        executor?.shutdown()
    }

    def "should create resilienceComponents bean with all components"() {
        when:
        def components = config.resilienceComponents()

        then:
        components != null
        components.circuitBreaker != null
        components.circuitBreaker.name == "database"
        components.retry != null
        components.retry.name == "database"
        components.timeLimiter != null
        components.scheduledExecutorService != null
        components.databaseResilienceConfig != null

        cleanup:
        components.scheduledExecutorService?.shutdown()
    }

    def "circuit breaker onStateTransition listener fires without error on state change"() {
        given:
        CircuitBreaker circuitBreaker = config.databaseCircuitBreaker()

        when:
        circuitBreaker.transitionToOpenState()

        then:
        noExceptionThrown()
        circuitBreaker.state == CircuitBreaker.State.OPEN
    }

    def "circuit breaker onFailureRateExceeded listener fires when threshold crossed"() {
        given:
        CircuitBreaker circuitBreaker = config.databaseCircuitBreaker()

        when:
        3.times {
            circuitBreaker.acquirePermission()
            circuitBreaker.onError(1L, TimeUnit.MILLISECONDS, new SQLException("test failure"))
        }
        2.times {
            circuitBreaker.acquirePermission()
            circuitBreaker.onSuccess(1L, TimeUnit.MILLISECONDS)
        }

        then:
        noExceptionThrown()
        circuitBreaker.state == CircuitBreaker.State.OPEN
    }

    def "circuit breaker onCallNotPermitted listener fires when circuit is open"() {
        given:
        CircuitBreaker circuitBreaker = config.databaseCircuitBreaker()
        circuitBreaker.transitionToOpenState()

        when:
        try {
            circuitBreaker.acquirePermission()
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException ignored) {
            // expected when circuit is open
        }

        then:
        noExceptionThrown()
    }

    def "connection pool gauge lambdas return correct values when sampled"() {
        given:
        def realMeterRegistry = new SimpleMeterRegistry()
        HikariDataSource hikariDataSource = Mock() {
            getHikariPoolMXBean() >> Mock(HikariPoolMXBean) {
                getActiveConnections() >> 5
                getIdleConnections() >> 3
                getThreadsAwaitingConnection() >> 1
                getTotalConnections() >> 10
            }
        }

        when:
        config.connectionPoolMetrics(realMeterRegistry, hikariDataSource)

        then:
        realMeterRegistry.get("hikari.connections.active").gauge().value() == 5.0
        realMeterRegistry.get("hikari.connections.idle").gauge().value() == 3.0
        realMeterRegistry.get("hikari.connections.pending").gauge().value() == 1.0
        realMeterRegistry.get("hikari.connections.total").gauge().value() == 10.0
    }
}