package finance.configurations

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.text.DecimalFormat

/**
 * Periodically logs performance metrics to the console for easy monitoring.
 * Provides a human-readable summary of application performance.
 *
 * Enable with: performance.console-reporting.enabled=true
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    prefix = "performance.console-reporting",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class ConsoleMetricsReporter(
    private val meterRegistry: MeterRegistry,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ConsoleMetricsReporter::class.java)
        private val decimalFormat = DecimalFormat("#,##0.00")
        private val intFormat = DecimalFormat("#,##0")
    }

    /**
     * Report metrics every 60 seconds (configurable via cron expression)
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    fun reportMetrics() {
        logger.info("=".repeat(80))
        logger.info("📊 PERFORMANCE METRICS REPORT")
        logger.info("=".repeat(80))

        reportMethodExecutionMetrics()
        reportHttpMetrics()
        reportDatabaseMetrics()
        reportJvmMetrics()
        reportSystemMetrics()

        logger.info("=".repeat(80))
    }

    /**
     * Report method execution time metrics
     */
    private fun reportMethodExecutionMetrics() {
        logger.info("\n🔧 METHOD EXECUTION METRICS:")

        val timers =
            meterRegistry.meters
                .filter { it.id.name == "method.execution.time" }
                .groupBy { it.id.getTag("layer") }

        if (timers.isEmpty()) {
            logger.info("  No method execution data yet")
            return
        }

        timers.forEach { (layer, meters) ->
            logger.info("  [$layer Layer]")

            meters
                .sortedByDescending { it.id.getTag("class") }
                .take(10) // Top 10 slowest methods per layer
                .forEach { meter ->
                    val className = meter.id.getTag("class") ?: "Unknown"
                    val methodName = meter.id.getTag("method") ?: "Unknown"
                    val status = meter.id.getTag("status") ?: "unknown"

                    val timer = meter as? io.micrometer.core.instrument.Timer
                    if (timer != null) {
                        val count = timer.count()
                        val totalTime = timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)
                        val avgTime = if (count > 0) totalTime / count else 0.0
                        val maxTime = timer.max(java.util.concurrent.TimeUnit.MILLISECONDS)

                        val statusIcon = if (status == "success") "✅" else "❌"
                        logger.info(
                            "    $statusIcon $className.$methodName: " +
                                "count=${intFormat.format(count)}, " +
                                "avg=${decimalFormat.format(avgTime)}ms, " +
                                "max=${decimalFormat.format(maxTime)}ms",
                        )
                    }
                }
        }
    }

    /**
     * Report HTTP request metrics
     */
    private fun reportHttpMetrics() {
        logger.info("\n🌐 HTTP REQUEST METRICS:")

        val httpTimers =
            meterRegistry.meters
                .filter { it.id.name == "http.server.requests" }

        if (httpTimers.isEmpty()) {
            logger.info("  No HTTP request data yet")
            return
        }

        val totalRequests =
            httpTimers.sumOf { meter ->
                (meter as? io.micrometer.core.instrument.Timer)?.count() ?: 0L
            }

        logger.info("  Total Requests: ${intFormat.format(totalRequests)}")

        // Group by status
        val byStatus =
            httpTimers.groupBy { meter ->
                meter.id.getTag("status") ?: "unknown"
            }

        byStatus.forEach { (status, meters) ->
            val count = meters.sumOf { (it as? io.micrometer.core.instrument.Timer)?.count() ?: 0L }
            val statusIcon =
                when {
                    status.startsWith("2") -> "✅"
                    status.startsWith("3") -> "➡️"
                    status.startsWith("4") -> "⚠️"
                    status.startsWith("5") -> "❌"
                    else -> "❓"
                }
            logger.info("    $statusIcon HTTP $status: ${intFormat.format(count)} requests")
        }

        // Top 5 slowest endpoints
        logger.info("  Slowest Endpoints:")
        httpTimers
            .mapNotNull { it as? io.micrometer.core.instrument.Timer }
            .sortedByDescending { it.max(java.util.concurrent.TimeUnit.MILLISECONDS) }
            .take(5)
            .forEach { timer ->
                val uri = timer.id.getTag("uri") ?: "unknown"
                val method = timer.id.getTag("method") ?: "unknown"
                val maxTime = timer.max(java.util.concurrent.TimeUnit.MILLISECONDS)
                val avgTime = timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS)

                logger.info(
                    "    $method $uri: max=${decimalFormat.format(maxTime)}ms, " +
                        "avg=${decimalFormat.format(avgTime)}ms",
                )
            }
    }

    /**
     * Report database connection pool metrics
     */
    private fun reportDatabaseMetrics() {
        logger.info("\n💾 DATABASE METRICS:")

        // HikariCP metrics
        val activeConnections = meterRegistry.find("hikari.connections.active").gauge()
        val idleConnections = meterRegistry.find("hikari.connections.idle").gauge()
        val pendingConnections = meterRegistry.find("hikari.connections.pending").gauge()
        val totalConnections = meterRegistry.find("hikari.connections.total").gauge()

        if (activeConnections != null) {
            logger.info("  HikariCP Connection Pool:")
            logger.info("    Active: ${intFormat.format(activeConnections.value())}")
            logger.info("    Idle: ${intFormat.format(idleConnections?.value() ?: 0.0)}")
            logger.info("    Pending: ${intFormat.format(pendingConnections?.value() ?: 0.0)}")
            logger.info("    Total: ${intFormat.format(totalConnections?.value() ?: 0.0)}")
        } else {
            logger.info("  No database connection metrics available")
        }

        // Circuit breaker state
        val circuitBreakerState =
            meterRegistry.meters
                .firstOrNull { it.id.name == "resilience4j.circuitbreaker.state" }
        if (circuitBreakerState != null) {
            val state = circuitBreakerState.id.getTag("state") ?: "unknown"
            val stateIcon =
                when (state.lowercase()) {
                    "closed" -> "✅"
                    "open" -> "❌"
                    "half_open" -> "⚠️"
                    else -> "❓"
                }
            logger.info("  Circuit Breaker: $stateIcon $state")
        }
    }

    /**
     * Report JVM memory metrics
     */
    private fun reportJvmMetrics() {
        logger.info("\n☕ JVM METRICS:")

        val heapUsed = meterRegistry.find("jvm.memory.used").tag("area", "heap").gauge()
        val heapMax = meterRegistry.find("jvm.memory.max").tag("area", "heap").gauge()

        if (heapUsed != null && heapMax != null) {
            val usedMB = heapUsed.value() / 1024 / 1024
            val maxMB = heapMax.value() / 1024 / 1024
            val usagePercent = (usedMB / maxMB) * 100

            logger.info("  Heap Memory: ${intFormat.format(usedMB)}MB / ${intFormat.format(maxMB)}MB (${decimalFormat.format(usagePercent)}%)")
        }

        val gcPauseTime = meterRegistry.find("jvm.gc.pause").timer()
        if (gcPauseTime != null) {
            val totalPauseMs = gcPauseTime.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)
            val gcCount = gcPauseTime.count()
            val avgPauseMs = if (gcCount > 0) totalPauseMs / gcCount else 0.0

            logger.info("  GC Pauses: ${intFormat.format(gcCount)} collections, avg=${decimalFormat.format(avgPauseMs)}ms")
        }

        val threadCount = meterRegistry.find("jvm.threads.live").gauge()
        if (threadCount != null) {
            logger.info("  Active Threads: ${intFormat.format(threadCount.value())}")
        }
    }

    /**
     * Report system CPU and memory metrics
     */
    private fun reportSystemMetrics() {
        logger.info("\n💻 SYSTEM METRICS:")

        val cpuUsage = meterRegistry.find("system.cpu.usage").gauge()
        if (cpuUsage != null) {
            val cpuPercent = cpuUsage.value() * 100
            logger.info("  CPU Usage: ${decimalFormat.format(cpuPercent)}%")
        }

        val processCpu = meterRegistry.find("process.cpu.usage").gauge()
        if (processCpu != null) {
            val processPercent = processCpu.value() * 100
            logger.info("  Process CPU: ${decimalFormat.format(processPercent)}%")
        }
    }
}
