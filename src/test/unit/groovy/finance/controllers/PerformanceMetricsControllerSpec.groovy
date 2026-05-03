package finance.controllers

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.TimeUnit

class PerformanceMetricsControllerSpec extends Specification {

    MeterRegistry meterRegistry = new SimpleMeterRegistry()

    @Subject
    PerformanceMetricsController controller = new PerformanceMetricsController(meterRegistry)

    def "getPerformanceSummary returns 200 with all sections"() {
        when:
        ResponseEntity<PerformanceMetricsController.PerformanceSummary> response = controller.getPerformanceSummary()

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.methodExecutionMetrics != null
        response.body.httpRequestMetrics != null
        response.body.databaseMetrics != null
        response.body.jvmMetrics != null
        response.body.timestamp > 0
    }

    def "getMethodMetrics returns metrics object with empty lists when no timers"() {
        when:
        PerformanceMetricsController.MethodExecutionMetrics result = controller.getMethodMetrics()

        then:
        result != null
        result.serviceLayerTop20 != null
        result.repositoryLayerTop20 != null
        result.slowest10Overall != null
        result.mostCalled10 != null
        result.serviceLayerTop20.isEmpty()
        result.repositoryLayerTop20.isEmpty()
    }

    def "getMethodMetrics includes service layer timers"() {
        given:
        Timer.builder("method.execution.time")
            .tag("class", "AccountService")
            .tag("method", "findAll")
            .tag("layer", "service")
            .tag("status", "success")
            .register(meterRegistry)
            .record(50, TimeUnit.MILLISECONDS)

        when:
        PerformanceMetricsController.MethodExecutionMetrics result = controller.getMethodMetrics()

        then:
        result.serviceLayerTop20.size() == 1
        result.serviceLayerTop20[0].className == "AccountService"
        result.serviceLayerTop20[0].methodName == "findAll"
        result.serviceLayerTop20[0].layer == "service"
        result.serviceLayerTop20[0].count == 1L
    }

    def "getMethodMetrics includes repository layer timers"() {
        given:
        Timer.builder("method.execution.time")
            .tag("class", "AccountRepository")
            .tag("method", "findById")
            .tag("layer", "repository")
            .tag("status", "success")
            .register(meterRegistry)
            .record(10, TimeUnit.MILLISECONDS)

        when:
        PerformanceMetricsController.MethodExecutionMetrics result = controller.getMethodMetrics()

        then:
        result.repositoryLayerTop20.size() == 1
        result.repositoryLayerTop20[0].methodName == "findById"
        result.repositoryLayerTop20[0].layer == "repository"
    }

    def "getDatabaseMetrics returns defaults when no hikari metrics"() {
        when:
        PerformanceMetricsController.DatabaseMetrics result = controller.getDatabaseMetrics()

        then:
        result != null
        result.connectionPool.active == 0
        result.connectionPool.idle == 0
        result.connectionPool.pending == 0
        result.connectionPool.total == 0
        result.connectionPool.utilizationPercent == 0.0
        result.circuitBreakerState == "unknown"
    }

    def "getDatabaseMetrics calculates utilization correctly when connections present"() {
        given:
        meterRegistry.gauge("hikari.connections.active", [], { 3.0 })
        meterRegistry.gauge("hikari.connections.total", [], { 10.0 })
        meterRegistry.gauge("hikari.connections.idle", [], { 7.0 })
        meterRegistry.gauge("hikari.connections.pending", [], { 0.0 })

        when:
        PerformanceMetricsController.DatabaseMetrics result = controller.getDatabaseMetrics()

        then:
        result != null
        result.connectionPool != null
    }

    def "getHttpMetrics returns defaults when no HTTP timers"() {
        when:
        PerformanceMetricsController.HttpRequestMetrics result = controller.getHttpMetrics()

        then:
        result != null
        result.totalRequests == 0
        result.statusCounts.isEmpty()
        result.slowestEndpoints.isEmpty()
        result.mostCalledEndpoints.isEmpty()
    }

    def "getHttpMetrics aggregates HTTP request timers"() {
        given:
        Timer.builder("http.server.requests")
            .tag("method", "GET")
            .tag("uri", "/api/account/active")
            .tag("status", "200")
            .register(meterRegistry)
            .record(100, TimeUnit.MILLISECONDS)

        Timer.builder("http.server.requests")
            .tag("method", "POST")
            .tag("uri", "/api/transaction")
            .tag("status", "201")
            .register(meterRegistry)
            .record(200, TimeUnit.MILLISECONDS)

        when:
        PerformanceMetricsController.HttpRequestMetrics result = controller.getHttpMetrics()

        then:
        result.totalRequests == 2
        result.statusCounts.containsKey("200")
        result.statusCounts.containsKey("201")
        result.slowestEndpoints.size() == 2
        result.mostCalledEndpoints.size() == 2
    }

    def "getJvmMetrics returns defaults when no JVM metrics"() {
        when:
        PerformanceMetricsController.JvmMetrics result = controller.getJvmMetrics()

        then:
        result != null
        result.heapMemory != null
        result.nonHeapMemory != null
        result.garbageCollection != null
        result.threads != null
        result.cpu != null
        result.heapMemory.usedMB == 0.0
        result.heapMemory.maxMB == 0.0
        result.heapMemory.usagePercent == 0.0
        result.garbageCollection.totalCollections == 0L
        result.threads.live == 0
        result.threads.daemon == 0
    }

    def "getPerformanceSummary timestamp is recent"() {
        given:
        long before = System.currentTimeMillis()

        when:
        ResponseEntity<PerformanceMetricsController.PerformanceSummary> response = controller.getPerformanceSummary()
        long after = System.currentTimeMillis()

        then:
        response.body.timestamp >= before
        response.body.timestamp <= after
    }
}
