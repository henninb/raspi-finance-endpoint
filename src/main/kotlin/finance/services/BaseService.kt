package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.configurations.DatabaseResilienceConfiguration
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.jdbc.CannotGetJdbcConnectionException
import org.springframework.stereotype.Service
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import java.sql.SQLException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

@Service
open class BaseService {
    @Autowired
    lateinit var meterService: MeterService

    @Autowired
    lateinit var validator: Validator

    @Autowired(required = false)
    var databaseResilienceConfig: DatabaseResilienceConfiguration? = null

    @Autowired(required = false)
    var circuitBreaker: CircuitBreaker? = null

    @Autowired(required = false)
    var retry: Retry? = null

    @Autowired(required = false)
    var timeLimiter: TimeLimiter? = null

    @Autowired(required = false)
    var scheduledExecutorService: ScheduledExecutorService? = null

    fun handleConstraintViolations(constraintViolations: Set<ConstraintViolation<*>>, meterService: MeterService) {
        if (constraintViolations.isNotEmpty()) {
            var details = ""
            constraintViolations.forEach { constraintViolation ->
                details = constraintViolation.invalidValue.toString() + ": " + constraintViolation.message
                logger.error(details)
            }
            logger.error("Cannot insert record because of constraint violation(s): $details")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert record because of constraint violation(s): $details")
        }
    }

    /**
     * Execute database operations with resilience patterns (circuit breaker, retry, timeout)
     * @param operation The database operation to execute
     * @param operationName Name for logging and metrics
     * @return CompletableFuture with the result
     */
    protected fun <T> executeWithResilience(
        operation: () -> T,
        operationName: String = "database-operation"
    ): CompletableFuture<T> {
        // If resilience components are not available (e.g., in test environment), execute directly
        if (databaseResilienceConfig == null || circuitBreaker == null || retry == null ||
            timeLimiter == null || scheduledExecutorService == null) {
            logger.debug("Resilience components not available, executing operation directly for: {}", operationName)
            return CompletableFuture.completedFuture(executeDirectly(operation, operationName))
        }

        val startTime = System.currentTimeMillis()

        return try {
            databaseResilienceConfig!!.executeWithResilience(
                operation = {
                    val duration = measureTimeMillis {
                        operation()
                    }
                    if (duration > 100) {
                        logger.warn("Slow query detected for {}: {} ms", operationName, duration)
                        meterService.incrementExceptionThrownCounter("SlowQuery")
                    }
                    operation()
                },
                circuitBreaker = circuitBreaker!!,
                retry = retry!!,
                timeLimiter = timeLimiter!!,
                executor = scheduledExecutorService!!
            ).whenComplete { result, throwable ->
                val totalDuration = System.currentTimeMillis() - startTime
                if (throwable != null) {
                    logger.error("Database operation {} failed after {} ms: {}",
                        operationName, totalDuration, throwable.message)
                    when (throwable.cause) {
                        is SQLException -> meterService.incrementExceptionThrownCounter("SQLException")
                        is DataAccessResourceFailureException -> meterService.incrementExceptionThrownCounter("DataAccessResourceFailureException")
                        is CannotGetJdbcConnectionException -> meterService.incrementExceptionThrownCounter("CannotGetJdbcConnectionException")
                        else -> meterService.incrementExceptionThrownCounter("DatabaseOperationException")
                    }
                } else {
                    logger.debug("Database operation {} completed successfully in {} ms", operationName, totalDuration)
                }
            }
        } catch (ex: Exception) {
            logger.error("Failed to execute database operation {} with resilience patterns: {}",
                operationName, ex.message, ex)
            meterService.incrementExceptionThrownCounter("ResiliencePatternException")
            CompletableFuture.failedFuture(ex)
        }
    }

    /**
     * Execute database operations synchronously with resilience patterns
     * @param operation The database operation to execute
     * @param operationName Name for logging and metrics
     * @param timeoutSeconds Timeout in seconds (default: 30)
     * @return The result of the operation
     */
    protected fun <T> executeWithResilienceSync(
        operation: () -> T,
        operationName: String = "database-operation",
        timeoutSeconds: Long = 30
    ): T {
        // If resilience components are not available, execute directly
        if (databaseResilienceConfig == null || circuitBreaker == null || retry == null ||
            timeLimiter == null || scheduledExecutorService == null) {
            logger.debug("Resilience components not available, executing operation directly for: {}", operationName)
            return executeDirectly(operation, operationName)
        }

        return try {
            executeWithResilience(operation, operationName)
                .get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (ex: Exception) {
            logger.error("Synchronous database operation {} failed: {}", operationName, ex.message, ex)
            when (ex.cause) {
                is SQLException -> {
                    meterService.incrementExceptionThrownCounter("SQLException")
                    throw DataAccessResourceFailureException("Database operation failed", ex)
                }
                is DataAccessResourceFailureException -> {
                    meterService.incrementExceptionThrownCounter("DataAccessResourceFailureException")
                    throw ex
                }
                else -> {
                    meterService.incrementExceptionThrownCounter("DatabaseOperationTimeoutException")
                    throw DataAccessResourceFailureException("Database operation timeout", ex)
                }
            }
        }
    }

    /**
     * Execute database operations directly without resilience patterns
     * Used as fallback when resilience components are not available
     */
    private fun <T> executeDirectly(operation: () -> T, operationName: String): T {
        val startTime = System.currentTimeMillis()
        return try {
            val result: T
            val duration = measureTimeMillis {
                result = operation()
            }
            if (duration > 100) {
                logger.warn("Slow query detected for {}: {} ms", operationName, duration)
                meterService.incrementExceptionThrownCounter("SlowQuery")
            }
            logger.debug("Database operation {} completed in {} ms", operationName, System.currentTimeMillis() - startTime)
            result
        } catch (ex: Exception) {
            logger.error("Database operation {} failed: {}", operationName, ex.message, ex)
            when (ex) {
                is SQLException -> meterService.incrementExceptionThrownCounter("SQLException")
                is DataAccessResourceFailureException -> meterService.incrementExceptionThrownCounter("DataAccessResourceFailureException")
                is CannotGetJdbcConnectionException -> meterService.incrementExceptionThrownCounter("CannotGetJdbcConnectionException")
                else -> meterService.incrementExceptionThrownCounter("DatabaseOperationException")
            }
            throw ex
        }
    }

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
    }
}