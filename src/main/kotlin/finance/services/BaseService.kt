package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.Account
import finance.domain.AccountType
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.apache.logging.log4j.LogManager
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.jdbc.CannotGetJdbcConnectionException
import java.sql.SQLException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

open class BaseService
    constructor(
        open val meterService: MeterService,
        open val validator: Validator,
        protected val resilienceComponents: ResilienceComponents,
    ) {
        protected val logger get() = LogManager.getLogger(javaClass)

        open fun handleConstraintViolations(constraintViolations: Set<ConstraintViolation<*>>) {
            if (constraintViolations.isEmpty()) return
            val details =
                constraintViolations.joinToString("; ") {
                    "${it.propertyPath}: ${it.message} (got: ${it.invalidValue})"
                }
            logger.error("Cannot insert record because of constraint violation(s): $details")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert record because of constraint violation(s): $details")
        }

        fun createDefaultAccount(
            accountNameOwner: String,
            accountType: AccountType,
        ): Account {
            val account = Account()
            account.accountNameOwner = accountNameOwner
            account.moniker = "0000"
            account.accountType = accountType
            account.activeStatus = true
            return account
        }

        /**
         * Execute database operations with resilience patterns (circuit breaker, retry, timeout)
         * @param operation The database operation to execute
         * @param operationName Name for logging and metrics
         * @return CompletableFuture with the result
         */
        protected fun <T> executeWithResilience(
            operation: () -> T,
            operationName: String = "database-operation",
        ): CompletableFuture<T> {
            val startTime = System.currentTimeMillis()

            return try {
                resilienceComponents.databaseResilienceConfig
                    .executeWithResilience(
                        operation = {
                            var result: T
                            val duration =
                                measureTimeMillis {
                                    result = operation()
                                }
                            if (duration > 100) {
                                logger.warn("Slow query detected for {}: {} ms", operationName, duration)
                                meterService.incrementExceptionThrownCounter("SlowQuery")
                            }
                            result
                        },
                        circuitBreaker = resilienceComponents.circuitBreaker,
                        retry = resilienceComponents.retry,
                        timeLimiter = resilienceComponents.timeLimiter,
                        executor = resilienceComponents.scheduledExecutorService,
                    ).whenComplete { _, throwable ->
                        val totalDuration = System.currentTimeMillis() - startTime
                        if (throwable != null) {
                            logger.error(
                                "Database operation {} failed after {} ms: {}",
                                operationName,
                                totalDuration,
                                throwable.message,
                            )
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
                logger.error(
                    "Failed to execute database operation {} with resilience patterns: {}",
                    operationName,
                    ex.message,
                    ex,
                )
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
            timeoutSeconds: Long = 30,
        ): T =
            try {
                executeWithResilience(operation, operationName)
                    .get(timeoutSeconds, TimeUnit.SECONDS)
            } catch (ex: Exception) {
                // Unwrap ExecutionException and UndeclaredThrowableException wrappers
                val cause =
                    generateSequence(ex as Throwable) { t ->
                        if (t is java.util.concurrent.ExecutionException || t is java.lang.reflect.UndeclaredThrowableException) t.cause else null
                    }.last()
                logger.error("Synchronous database operation {} failed: {}", operationName, cause.message, cause)
                when (cause) {
                    is SQLException -> {
                        meterService.incrementExceptionThrownCounter("SQLException")
                        throw DataAccessResourceFailureException("Database operation failed", cause)
                    }

                    is DataAccessResourceFailureException -> {
                        meterService.incrementExceptionThrownCounter("DataAccessResourceFailureException")
                        throw cause
                    }

                    else -> {
                        meterService.incrementExceptionThrownCounter("DatabaseOperationTimeoutException")
                        throw DataAccessResourceFailureException("Database operation timeout", cause)
                    }
                }
            }
    }
