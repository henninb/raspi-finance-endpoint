package finance.configurations

import finance.services.MeterService
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * AOP Aspect for monitoring service method performance.
 * Logs execution time and publishes metrics for all service layer methods.
 *
 * Performance Impact: ~1-2% overhead per method call
 */
@Aspect
@Component
class PerformanceMonitoringAspect(
    private val meterRegistry: MeterRegistry,
    private val meterService: MeterService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PerformanceMonitoringAspect::class.java)

        // Configurable thresholds (in milliseconds)
        private const val WARN_THRESHOLD_MS = 500L
        private const val ERROR_THRESHOLD_MS = 2000L
    }

    /**
     * Pointcut for all service layer methods
     */
    @Pointcut("execution(* finance.services..*(..))")
    fun serviceMethods() {
    }

    /**
     * Pointcut for all repository methods
     */
    @Pointcut("execution(* finance.repositories..*(..))")
    fun repositoryMethods() {
    }

    /**
     * Around advice for service methods - tracks execution time and logs performance
     */
    @Around("serviceMethods()")
    fun monitorServiceMethodPerformance(joinPoint: ProceedingJoinPoint): Any? = monitorMethodExecution(joinPoint, "service")

    /**
     * Around advice for repository methods - tracks execution time and logs performance
     */
    @Around("repositoryMethods()")
    fun monitorRepositoryMethodPerformance(joinPoint: ProceedingJoinPoint): Any? = monitorMethodExecution(joinPoint, "repository")

    /**
     * Core monitoring logic for method execution
     */
    private fun monitorMethodExecution(
        joinPoint: ProceedingJoinPoint,
        layer: String,
    ): Any? {
        val className = joinPoint.signature.declaringTypeName.substringAfterLast('.')
        val methodName = joinPoint.signature.name
        val fullMethodName = "$className.$methodName"

        // Track execution time
        val startTime = System.nanoTime()
        var exception: Throwable? = null
        var result: Any? = null

        try {
            result = joinPoint.proceed()
            return result
        } catch (ex: Throwable) {
            exception = ex
            throw ex
        } finally {
            val durationNanos = System.nanoTime() - startTime
            val durationMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos)

            // Record metrics
            recordMetrics(layer, className, methodName, durationNanos, exception)

            // Log performance based on thresholds
            logPerformance(layer, fullMethodName, durationMillis, exception)
        }
    }

    /**
     * Record metrics to Micrometer registry
     */
    private fun recordMetrics(
        layer: String,
        className: String,
        methodName: String,
        durationNanos: Long,
        exception: Throwable?,
    ) {
        Timer
            .builder("method.execution.time")
            .tag("layer", layer)
            .tag("class", className)
            .tag("method", methodName)
            .tag("status", if (exception == null) "success" else "failure")
            .register(meterRegistry)
            .record(durationNanos, TimeUnit.NANOSECONDS)
    }

    /**
     * Log performance with appropriate log level based on execution time
     */
    private fun logPerformance(
        layer: String,
        fullMethodName: String,
        durationMillis: Long,
        exception: Throwable?,
    ) {
        val correlationId = MDC.get("correlationId") ?: "N/A"

        when {
            exception != null -> {
                logger.error(
                    "[PERF] [{}] {}.{} FAILED after {}ms - Exception: {}",
                    correlationId,
                    layer.uppercase(),
                    fullMethodName,
                    durationMillis,
                    exception.javaClass.simpleName,
                )
            }

            durationMillis >= ERROR_THRESHOLD_MS -> {
                logger.error(
                    "[PERF] [{}] {}.{} took {}ms (CRITICAL - exceeds {}ms threshold)",
                    correlationId,
                    layer.uppercase(),
                    fullMethodName,
                    durationMillis,
                    ERROR_THRESHOLD_MS,
                )
            }

            durationMillis >= WARN_THRESHOLD_MS -> {
                logger.warn(
                    "[PERF] [{}] {}.{} took {}ms (exceeds {}ms threshold)",
                    correlationId,
                    layer.uppercase(),
                    fullMethodName,
                    durationMillis,
                    WARN_THRESHOLD_MS,
                )
            }

            else -> {
                logger.info(
                    "[PERF] [{}] {}.{} took {}ms",
                    correlationId,
                    layer.uppercase(),
                    fullMethodName,
                    durationMillis,
                )
            }
        }
    }
}
