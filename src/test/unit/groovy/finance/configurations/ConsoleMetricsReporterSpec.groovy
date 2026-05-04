package finance.configurations

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import spock.lang.Specification

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ConsoleMetricsReporterSpec extends Specification {

    MeterRegistry meterRegistry
    ConsoleMetricsReporter reporter

    def setup() {
        meterRegistry = new SimpleMeterRegistry()
        reporter = new ConsoleMetricsReporter(meterRegistry)
    }

    def "reportMetrics runs without exception when registry is empty"() {
        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles method execution timer with success status"() {
        given:
        Timer timer = Timer.builder("method.execution.time")
            .tag("layer", "service")
            .tag("class", "AccountService")
            .tag("method", "findAll")
            .tag("status", "success")
            .register(meterRegistry)
        timer.record(100, TimeUnit.MILLISECONDS)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles method execution timer with failure status"() {
        given:
        Timer timer = Timer.builder("method.execution.time")
            .tag("layer", "repository")
            .tag("class", "AccountRepository")
            .tag("method", "findById")
            .tag("status", "failure")
            .register(meterRegistry)
        timer.record(50, TimeUnit.MILLISECONDS)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles multiple service and repository layers"() {
        given:
        ["service", "repository"].each { layer ->
            Timer.builder("method.execution.time")
                .tag("layer", layer)
                .tag("class", "SomeClass")
                .tag("method", "someMethod")
                .tag("status", "success")
                .register(meterRegistry)
                .record(75, TimeUnit.MILLISECONDS)
        }

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles HTTP 2xx request timers"() {
        given:
        Timer.builder("http.server.requests")
            .tag("status", "200")
            .tag("uri", "/api/accounts")
            .tag("method", "GET")
            .register(meterRegistry)
            .record(40, TimeUnit.MILLISECONDS)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles HTTP 3xx request timers"() {
        given:
        Timer.builder("http.server.requests")
            .tag("status", "301")
            .tag("uri", "/old-path")
            .tag("method", "GET")
            .register(meterRegistry)
            .record(5, TimeUnit.MILLISECONDS)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles HTTP 4xx request timers"() {
        given:
        Timer.builder("http.server.requests")
            .tag("status", "404")
            .tag("uri", "/api/unknown")
            .tag("method", "GET")
            .register(meterRegistry)
            .record(10, TimeUnit.MILLISECONDS)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles HTTP 5xx request timers"() {
        given:
        Timer.builder("http.server.requests")
            .tag("status", "500")
            .tag("uri", "/api/error")
            .tag("method", "POST")
            .register(meterRegistry)
            .record(200, TimeUnit.MILLISECONDS)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles unknown HTTP status timers"() {
        given:
        Timer.builder("http.server.requests")
            .tag("status", "0")
            .tag("uri", "/api/unknown")
            .tag("method", "GET")
            .register(meterRegistry)
            .record(5, TimeUnit.MILLISECONDS)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles multiple HTTP request timers sorted by max time"() {
        given:
        [50L, 100L, 200L, 300L, 400L, 500L].eachWithIndex { ms, i ->
            Timer.builder("http.server.requests")
                .tag("status", "200")
                .tag("uri", "/api/endpoint${i}")
                .tag("method", "GET")
                .register(meterRegistry)
                .record(ms, TimeUnit.MILLISECONDS)
        }

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles HikariCP connection pool metrics"() {
        given:
        AtomicInteger activeRef = new AtomicInteger(5)
        AtomicInteger idleRef = new AtomicInteger(3)
        AtomicInteger pendingRef = new AtomicInteger(1)
        AtomicInteger totalRef = new AtomicInteger(10)

        Gauge.builder("hikari.connections.active", activeRef, { v -> v.doubleValue() } as java.util.function.ToDoubleFunction)
            .register(meterRegistry)
        Gauge.builder("hikari.connections.idle", idleRef, { v -> v.doubleValue() } as java.util.function.ToDoubleFunction)
            .register(meterRegistry)
        Gauge.builder("hikari.connections.pending", pendingRef, { v -> v.doubleValue() } as java.util.function.ToDoubleFunction)
            .register(meterRegistry)
        Gauge.builder("hikari.connections.total", totalRef, { v -> v.doubleValue() } as java.util.function.ToDoubleFunction)
            .register(meterRegistry)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles circuit breaker in closed state"() {
        given:
        AtomicInteger stateRef = new AtomicInteger(0)
        Gauge.builder("resilience4j.circuitbreaker.state", stateRef, { v -> v.doubleValue() } as java.util.function.ToDoubleFunction)
            .tag("state", "closed")
            .register(meterRegistry)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles circuit breaker in open state"() {
        given:
        AtomicInteger stateRef = new AtomicInteger(1)
        Gauge.builder("resilience4j.circuitbreaker.state", stateRef, { v -> v.doubleValue() } as java.util.function.ToDoubleFunction)
            .tag("state", "open")
            .register(meterRegistry)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles circuit breaker in half_open state"() {
        given:
        AtomicInteger stateRef = new AtomicInteger(1)
        Gauge.builder("resilience4j.circuitbreaker.state", stateRef, { v -> v.doubleValue() } as java.util.function.ToDoubleFunction)
            .tag("state", "half_open")
            .register(meterRegistry)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles circuit breaker in unknown state"() {
        given:
        AtomicInteger stateRef = new AtomicInteger(0)
        Gauge.builder("resilience4j.circuitbreaker.state", stateRef, { v -> v.doubleValue() } as java.util.function.ToDoubleFunction)
            .tag("state", "disabled")
            .register(meterRegistry)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles JVM heap memory metrics"() {
        given:
        AtomicInteger heapUsed = new AtomicInteger(512 * 1024 * 1024)
        AtomicInteger heapMax = new AtomicInteger(1024 * 1024 * 1024)
        Gauge.builder("jvm.memory.used", heapUsed, { v -> v.doubleValue() } as java.util.function.ToDoubleFunction)
            .tag("area", "heap")
            .register(meterRegistry)
        Gauge.builder("jvm.memory.max", heapMax, { v -> v.doubleValue() } as java.util.function.ToDoubleFunction)
            .tag("area", "heap")
            .register(meterRegistry)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles JVM GC pause timer"() {
        given:
        Timer.builder("jvm.gc.pause")
            .register(meterRegistry)
            .record(50, TimeUnit.MILLISECONDS)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles JVM thread count gauge"() {
        given:
        AtomicInteger threadCount = new AtomicInteger(42)
        Gauge.builder("jvm.threads.live", threadCount, { v -> v.doubleValue() } as java.util.function.ToDoubleFunction)
            .register(meterRegistry)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles system CPU usage gauge"() {
        given:
        AtomicInteger cpuRef = new AtomicInteger(1)
        Gauge.builder("system.cpu.usage", cpuRef, { v -> v.doubleValue() / 100.0 } as java.util.function.ToDoubleFunction)
            .register(meterRegistry)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles process CPU usage gauge"() {
        given:
        AtomicInteger cpuRef = new AtomicInteger(1)
        Gauge.builder("process.cpu.usage", cpuRef, { v -> v.doubleValue() / 100.0 } as java.util.function.ToDoubleFunction)
            .register(meterRegistry)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }

    def "reportMetrics handles all metrics together"() {
        given:
        Timer.builder("method.execution.time")
            .tag("layer", "service").tag("class", "AccountService")
            .tag("method", "findAll").tag("status", "success")
            .register(meterRegistry).record(100, TimeUnit.MILLISECONDS)

        Timer.builder("http.server.requests")
            .tag("status", "200").tag("uri", "/api/accounts").tag("method", "GET")
            .register(meterRegistry).record(50, TimeUnit.MILLISECONDS)

        AtomicInteger activeRef = new AtomicInteger(3)
        Gauge.builder("hikari.connections.active", activeRef, { v -> v.doubleValue() } as java.util.function.ToDoubleFunction)
            .register(meterRegistry)

        when:
        reporter.reportMetrics()

        then:
        noExceptionThrown()
    }
}
