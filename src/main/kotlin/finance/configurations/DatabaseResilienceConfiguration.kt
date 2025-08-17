package finance.configurations

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import io.micrometer.core.instrument.MeterRegistry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.Status
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.jdbc.CannotGetJdbcConnectionException
import java.sql.SQLException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import javax.sql.DataSource

@Configuration
open class DatabaseResilienceConfiguration {

    companion object {
        private val logger: Logger = LogManager.getLogger(DatabaseResilienceConfiguration::class.java)
    }

    @Bean
    open fun databaseCircuitBreaker(): CircuitBreaker {
        val config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .permittedNumberOfCallsInHalfOpenState(3)
            .slowCallRateThreshold(50.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(30))
            .recordExceptions(
                SQLException::class.java,
                DataAccessResourceFailureException::class.java,
                CannotGetJdbcConnectionException::class.java
            )
            .build()

        val circuitBreaker = CircuitBreaker.of("database", config)

        circuitBreaker.eventPublisher
            .onStateTransition { event ->
                logger.warn("Circuit breaker state transition: {} -> {}",
                    event.stateTransition.fromState, event.stateTransition.toState)
            }
            .onFailureRateExceeded { event ->
                logger.error("Circuit breaker failure rate exceeded: {}%", event.failureRate)
            }
            .onCallNotPermitted { event ->
                logger.warn("Circuit breaker call not permitted: {}", event.creationTime)
            }

        return circuitBreaker
    }

    @Bean
    open fun databaseRetry(): Retry {
        val config = RetryConfig.custom<Any>()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(1))
            .retryExceptions(
                SQLException::class.java,
                DataAccessResourceFailureException::class.java,
                CannotGetJdbcConnectionException::class.java
            )
            .ignoreExceptions(
                jakarta.validation.ValidationException::class.java,
                IllegalArgumentException::class.java
            )
            .build()

        val retry = Retry.of("database", config)

        retry.eventPublisher
            .onRetry { event ->
                logger.warn("Database operation retry attempt: {}, exception: {}",
                    event.numberOfRetryAttempts, event.lastThrowable?.message)
            }

        return retry
    }

    @Bean
    open fun databaseTimeLimiter(): TimeLimiter {
        val config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(30))
            .cancelRunningFuture(true)
            .build()

        val timeLimiter = TimeLimiter.of("database", config)

        timeLimiter.eventPublisher
            .onTimeout { event ->
                logger.error("Database operation timed out after: {}ms", event.creationTime)
            }

        return timeLimiter
    }

    @Bean
    open fun scheduledExecutorService(): ScheduledExecutorService {
        return Executors.newScheduledThreadPool(4) { runnable ->
            Thread(runnable).apply {
                isDaemon = true
                name = "db-resilience-scheduler"
            }
        }
    }

    @Bean
    open fun databaseHealthIndicator(dataSource: DataSource): HealthIndicator {
        return HealthIndicator {
            try {
                dataSource.connection.use { connection ->
                    val isValid = connection.isValid(5)
                    if (isValid) {
                        org.springframework.boot.actuate.health.Health.up()
                            .withDetail("database", "Connection pool healthy")
                            .build()
                    } else {
                        org.springframework.boot.actuate.health.Health.down()
                            .withDetail("database", "Connection validation failed")
                            .build()
                    }
                }
            } catch (ex: Exception) {
                logger.error("Database health check failed", ex)
                org.springframework.boot.actuate.health.Health.down(ex)
                    .withDetail("database", "Health check failed: ${ex.message}")
                    .build()
            }
        }
    }

    @Bean
    open fun connectionPoolMetrics(meterRegistry: MeterRegistry, dataSource: DataSource): String {
        // Register custom metrics for connection pool monitoring
        if (dataSource is com.zaxxer.hikari.HikariDataSource) {
            val hikariDataSource = dataSource

            meterRegistry.gauge("hikari.connections.active", hikariDataSource) { ds ->
                ds.hikariPoolMXBean?.activeConnections?.toDouble() ?: 0.0
            }

            meterRegistry.gauge("hikari.connections.idle", hikariDataSource) { ds ->
                ds.hikariPoolMXBean?.idleConnections?.toDouble() ?: 0.0
            }

            meterRegistry.gauge("hikari.connections.pending", hikariDataSource) { ds ->
                ds.hikariPoolMXBean?.threadsAwaitingConnection?.toDouble() ?: 0.0
            }

            meterRegistry.gauge("hikari.connections.total", hikariDataSource) { ds ->
                ds.hikariPoolMXBean?.totalConnections?.toDouble() ?: 0.0
            }
        }
        return "connectionPoolMetricsConfigured"
    }

    /**
     * Utility function to execute database operations with resilience patterns
     */
    open fun <T> executeWithResilience(
        operation: () -> T,
        circuitBreaker: CircuitBreaker = databaseCircuitBreaker(),
        retry: Retry = databaseRetry(),
        timeLimiter: TimeLimiter = databaseTimeLimiter(),
        executor: ScheduledExecutorService = scheduledExecutorService()
    ): CompletableFuture<T> {
        val decoratedOperation = CircuitBreaker.decorateSupplier(circuitBreaker) {
            Retry.decorateSupplier(retry, operation).get()
        }

        val futureOperation = CompletableFuture.supplyAsync(decoratedOperation, executor)

        return timeLimiter.executeCompletionStage(executor) { futureOperation }.toCompletableFuture()
    }

    /**
     * Overloaded method for Groovy/Java Supplier compatibility
     */
    open fun <T> executeWithResilience(
        operation: java.util.function.Supplier<T>
    ): CompletableFuture<T> {
        val decoratedOperation = CircuitBreaker.decorateSupplier(databaseCircuitBreaker()) {
            Retry.decorateSupplier(databaseRetry(), operation).get()
        }

        val futureOperation = CompletableFuture.supplyAsync(decoratedOperation, scheduledExecutorService())

        return databaseTimeLimiter().executeCompletionStage(scheduledExecutorService()) { futureOperation }.toCompletableFuture()
    }
}