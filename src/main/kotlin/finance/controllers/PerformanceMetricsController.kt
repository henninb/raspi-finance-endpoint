package finance.controllers

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

/**
 * REST endpoint for viewing performance metrics and diagnostics.
 * Provides human-readable performance summaries for troubleshooting slow performance.
 *
 * Endpoints:
 * - GET /performance/summary - Overall performance summary
 * - GET /performance/methods - Method execution metrics
 * - GET /performance/database - Database and connection pool metrics
 * - GET /performance/http - HTTP request metrics
 * - GET /performance/jvm - JVM memory and GC metrics
 */
@RestController
@RequestMapping("/performance")
class PerformanceMetricsController(
    private val meterRegistry: MeterRegistry,
) {
    companion object {
        private val decimalFormat = DecimalFormat("#,##0.00")
        private val intFormat = DecimalFormat("#,##0")
    }

    /**
     * GET /performance/summary - Overall performance summary
     */
    @GetMapping("/summary")
    fun getPerformanceSummary(): ResponseEntity<PerformanceSummary> {
        val methodMetrics = getMethodMetrics()
        val httpMetrics = getHttpMetrics()
        val dbMetrics = getDatabaseMetrics()
        val jvmMetrics = getJvmMetrics()

        return ResponseEntity.ok(
            PerformanceSummary(
                methodExecutionMetrics = methodMetrics,
                httpRequestMetrics = httpMetrics,
                databaseMetrics = dbMetrics,
                jvmMetrics = jvmMetrics,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * GET /performance/methods - Detailed method execution metrics
     */
    @GetMapping("/methods")
    fun getMethodMetrics(): MethodExecutionMetrics {
        val timers =
            meterRegistry.meters
                .filter { it.id.name == "method.execution.time" }
                .mapNotNull { it as? io.micrometer.core.instrument.Timer }

        val serviceMetrics =
            timers
                .filter { it.id.getTag("layer") == "service" }
                .map { buildMethodMetric(it) }
                .sortedByDescending { it.avgTimeMs }
                .take(20)

        val repositoryMetrics =
            timers
                .filter { it.id.getTag("layer") == "repository" }
                .map { buildMethodMetric(it) }
                .sortedByDescending { it.avgTimeMs }
                .take(20)

        val slowestMethods =
            timers
                .map { buildMethodMetric(it) }
                .sortedByDescending { it.maxTimeMs }
                .take(10)

        val mostCalledMethods =
            timers
                .map { buildMethodMetric(it) }
                .sortedByDescending { it.count }
                .take(10)

        return MethodExecutionMetrics(
            serviceLayerTop20 = serviceMetrics,
            repositoryLayerTop20 = repositoryMetrics,
            slowest10Overall = slowestMethods,
            mostCalled10 = mostCalledMethods,
        )
    }

    /**
     * GET /performance/database - Database and connection pool metrics
     */
    @GetMapping("/database")
    fun getDatabaseMetrics(): DatabaseMetrics {
        val activeConnections =
            meterRegistry
                .find("hikari.connections.active")
                .gauge()
                ?.value()
                ?.toInt() ?: 0
        val idleConnections =
            meterRegistry
                .find("hikari.connections.idle")
                .gauge()
                ?.value()
                ?.toInt() ?: 0
        val pendingConnections =
            meterRegistry
                .find("hikari.connections.pending")
                .gauge()
                ?.value()
                ?.toInt() ?: 0
        val totalConnections =
            meterRegistry
                .find("hikari.connections.total")
                .gauge()
                ?.value()
                ?.toInt() ?: 0

        val circuitBreakerState =
            meterRegistry.meters
                .firstOrNull { it.id.name.contains("circuitbreaker.state") }
                ?.id
                ?.getTag("state") ?: "unknown"

        return DatabaseMetrics(
            connectionPool =
                ConnectionPoolMetrics(
                    active = activeConnections,
                    idle = idleConnections,
                    pending = pendingConnections,
                    total = totalConnections,
                    utilizationPercent = if (totalConnections > 0) (activeConnections.toDouble() / totalConnections * 100) else 0.0,
                ),
            circuitBreakerState = circuitBreakerState,
        )
    }

    /**
     * GET /performance/http - HTTP request metrics
     */
    @GetMapping("/http")
    fun getHttpMetrics(): HttpRequestMetrics {
        val httpTimers =
            meterRegistry.meters
                .filter { it.id.name == "http.server.requests" }
                .mapNotNull { it as? io.micrometer.core.instrument.Timer }

        val totalRequests = httpTimers.sumOf { it.count() }

        val statusCounts =
            httpTimers
                .groupBy { it.id.getTag("status") ?: "unknown" }
                .mapValues { (_, timers) -> timers.sumOf { it.count() } }

        val slowestEndpoints =
            httpTimers
                .sortedByDescending { it.max(TimeUnit.MILLISECONDS) }
                .take(10)
                .map { timer ->
                    EndpointMetric(
                        method = timer.id.getTag("method") ?: "unknown",
                        uri = timer.id.getTag("uri") ?: "unknown",
                        status = timer.id.getTag("status") ?: "unknown",
                        count = timer.count(),
                        avgTimeMs = timer.mean(TimeUnit.MILLISECONDS),
                        maxTimeMs = timer.max(TimeUnit.MILLISECONDS),
                    )
                }

        val mostCalledEndpoints =
            httpTimers
                .sortedByDescending { it.count() }
                .take(10)
                .map { timer ->
                    EndpointMetric(
                        method = timer.id.getTag("method") ?: "unknown",
                        uri = timer.id.getTag("uri") ?: "unknown",
                        status = timer.id.getTag("status") ?: "unknown",
                        count = timer.count(),
                        avgTimeMs = timer.mean(TimeUnit.MILLISECONDS),
                        maxTimeMs = timer.max(TimeUnit.MILLISECONDS),
                    )
                }

        return HttpRequestMetrics(
            totalRequests = totalRequests,
            statusCounts = statusCounts,
            slowestEndpoints = slowestEndpoints,
            mostCalledEndpoints = mostCalledEndpoints,
        )
    }

    /**
     * GET /performance/jvm - JVM memory and GC metrics
     */
    @GetMapping("/jvm")
    fun getJvmMetrics(): JvmMetrics {
        val heapUsed =
            meterRegistry
                .find("jvm.memory.used")
                .tag("area", "heap")
                .gauge()
                ?.value() ?: 0.0
        val heapMax =
            meterRegistry
                .find("jvm.memory.max")
                .tag("area", "heap")
                .gauge()
                ?.value() ?: 0.0
        val heapUsedMB = heapUsed / 1024 / 1024
        val heapMaxMB = heapMax / 1024 / 1024

        val nonHeapUsed =
            meterRegistry
                .find("jvm.memory.used")
                .tag("area", "nonheap")
                .gauge()
                ?.value() ?: 0.0
        val nonHeapMax =
            meterRegistry
                .find("jvm.memory.max")
                .tag("area", "nonheap")
                .gauge()
                ?.value() ?: 0.0
        val nonHeapUsedMB = nonHeapUsed / 1024 / 1024
        val nonHeapMaxMB = nonHeapMax / 1024 / 1024

        val gcPauseTimer = meterRegistry.find("jvm.gc.pause").timer()
        val gcCount = gcPauseTimer?.count() ?: 0
        val gcTotalTimeMs = gcPauseTimer?.totalTime(TimeUnit.MILLISECONDS) ?: 0.0
        val gcAvgPauseMs = if (gcCount > 0) gcTotalTimeMs / gcCount else 0.0

        val threadCount =
            meterRegistry
                .find("jvm.threads.live")
                .gauge()
                ?.value()
                ?.toInt() ?: 0
        val daemonThreadCount =
            meterRegistry
                .find("jvm.threads.daemon")
                .gauge()
                ?.value()
                ?.toInt() ?: 0

        val cpuUsage = meterRegistry.find("system.cpu.usage").gauge()?.value() ?: 0.0
        val processCpuUsage = meterRegistry.find("process.cpu.usage").gauge()?.value() ?: 0.0

        return JvmMetrics(
            heapMemory =
                MemoryMetric(
                    usedMB = heapUsedMB,
                    maxMB = heapMaxMB,
                    usagePercent = if (heapMaxMB > 0) (heapUsedMB / heapMaxMB * 100) else 0.0,
                ),
            nonHeapMemory =
                MemoryMetric(
                    usedMB = nonHeapUsedMB,
                    maxMB = nonHeapMaxMB,
                    usagePercent = if (nonHeapMaxMB > 0) (nonHeapUsedMB / nonHeapMaxMB * 100) else 0.0,
                ),
            garbageCollection =
                GcMetrics(
                    totalCollections = gcCount,
                    totalPauseTimeMs = gcTotalTimeMs,
                    avgPauseTimeMs = gcAvgPauseMs,
                ),
            threads =
                ThreadMetrics(
                    live = threadCount,
                    daemon = daemonThreadCount,
                ),
            cpu =
                CpuMetrics(
                    systemUsagePercent = cpuUsage * 100,
                    processUsagePercent = processCpuUsage * 100,
                ),
        )
    }

    private fun buildMethodMetric(timer: io.micrometer.core.instrument.Timer): MethodMetric =
        MethodMetric(
            className = timer.id.getTag("class") ?: "Unknown",
            methodName = timer.id.getTag("method") ?: "Unknown",
            layer = timer.id.getTag("layer") ?: "unknown",
            status = timer.id.getTag("status") ?: "unknown",
            count = timer.count(),
            avgTimeMs = timer.mean(TimeUnit.MILLISECONDS),
            maxTimeMs = timer.max(TimeUnit.MILLISECONDS),
            totalTimeMs = timer.totalTime(TimeUnit.MILLISECONDS),
        )

    // Data classes for response structure
    data class PerformanceSummary(
        val methodExecutionMetrics: MethodExecutionMetrics,
        val httpRequestMetrics: HttpRequestMetrics,
        val databaseMetrics: DatabaseMetrics,
        val jvmMetrics: JvmMetrics,
        val timestamp: Long,
    )

    data class MethodExecutionMetrics(
        val serviceLayerTop20: List<MethodMetric>,
        val repositoryLayerTop20: List<MethodMetric>,
        val slowest10Overall: List<MethodMetric>,
        val mostCalled10: List<MethodMetric>,
    )

    data class MethodMetric(
        val className: String,
        val methodName: String,
        val layer: String,
        val status: String,
        val count: Long,
        val avgTimeMs: Double,
        val maxTimeMs: Double,
        val totalTimeMs: Double,
    )

    data class HttpRequestMetrics(
        val totalRequests: Long,
        val statusCounts: Map<String, Long>,
        val slowestEndpoints: List<EndpointMetric>,
        val mostCalledEndpoints: List<EndpointMetric>,
    )

    data class EndpointMetric(
        val method: String,
        val uri: String,
        val status: String,
        val count: Long,
        val avgTimeMs: Double,
        val maxTimeMs: Double,
    )

    data class DatabaseMetrics(
        val connectionPool: ConnectionPoolMetrics,
        val circuitBreakerState: String,
    )

    data class ConnectionPoolMetrics(
        val active: Int,
        val idle: Int,
        val pending: Int,
        val total: Int,
        val utilizationPercent: Double,
    )

    data class JvmMetrics(
        val heapMemory: MemoryMetric,
        val nonHeapMemory: MemoryMetric,
        val garbageCollection: GcMetrics,
        val threads: ThreadMetrics,
        val cpu: CpuMetrics,
    )

    data class MemoryMetric(
        val usedMB: Double,
        val maxMB: Double,
        val usagePercent: Double,
    )

    data class GcMetrics(
        val totalCollections: Long,
        val totalPauseTimeMs: Double,
        val avgPauseTimeMs: Double,
    )

    data class ThreadMetrics(
        val live: Int,
        val daemon: Int,
    )

    data class CpuMetrics(
        val systemUsagePercent: Double,
        val processUsagePercent: Double,
    )
}
