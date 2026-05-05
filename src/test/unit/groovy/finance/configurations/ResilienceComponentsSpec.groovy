package finance.configurations

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class ResilienceComponentsSpec extends Specification {

    def "noOp returns non-null ResilienceComponents with all fields set"() {
        when:
        def comp = ResilienceComponents.noOp()

        then:
        comp != null
        comp.circuitBreaker != null
        comp.retry != null
        comp.timeLimiter != null
        comp.scheduledExecutorService != null
        comp.databaseResilienceConfig != null
    }

    def "noOp is a singleton - repeated calls return same instance"() {
        when:
        def c1 = ResilienceComponents.noOp()
        def c2 = ResilienceComponents.noOp()

        then:
        c1.is(c2)
    }

    def "noOp databaseResilienceConfig is NoOpDatabaseResilienceConfiguration"() {
        when:
        def comp = ResilienceComponents.noOp()

        then:
        comp.databaseResilienceConfig instanceof NoOpDatabaseResilienceConfiguration
    }

    def "noOp circuitBreaker name is noOp"() {
        when:
        def comp = ResilienceComponents.noOp()

        then:
        comp.circuitBreaker.name == "noOp"
    }

    def "noOp retry name is noOp"() {
        when:
        def comp = ResilienceComponents.noOp()

        then:
        comp.retry.name == "noOp"
    }

    def "NoOpDatabaseResilienceConfiguration executeWithResilience completes immediately"() {
        given:
        def config = new NoOpDatabaseResilienceConfiguration()
        def cb = CircuitBreaker.ofDefaults("test")
        def retry = Retry.ofDefaults("test")
        def tl = TimeLimiter.ofDefaults("test")
        def executor = Executors.newSingleThreadScheduledExecutor()

        when:
        CompletableFuture<String> future = config.executeWithResilience(
            { "no-op-result" },
            cb, retry, tl, executor
        )

        then:
        future.isDone()
        future.get() == "no-op-result"

        cleanup:
        executor?.shutdown()
    }

    def "NoOpDatabaseResilienceConfiguration executeWithResilience calls the operation directly"() {
        given:
        def config = new NoOpDatabaseResilienceConfiguration()
        def cb = CircuitBreaker.ofDefaults("test2")
        def retry = Retry.ofDefaults("test2")
        def tl = TimeLimiter.ofDefaults("test2")
        def executor = Executors.newSingleThreadScheduledExecutor()
        boolean called = false

        when:
        CompletableFuture<Integer> future = config.executeWithResilience(
            { called = true; 42 },
            cb, retry, tl, executor
        )

        then:
        called
        future.get() == 42

        cleanup:
        executor?.shutdown()
    }

    def "NoOpDatabaseResilienceConfiguration bypasses circuit-breaker for open state"() {
        given:
        def config = new NoOpDatabaseResilienceConfiguration()
        def cb = CircuitBreaker.ofDefaults("forced-open")
        cb.transitionToOpenState()
        def retry = Retry.ofDefaults("forced-open")
        def tl = TimeLimiter.ofDefaults("forced-open")
        def executor = Executors.newSingleThreadScheduledExecutor()

        when:
        CompletableFuture<String> future = config.executeWithResilience(
            { "bypassed" },
            cb, retry, tl, executor
        )

        then:
        future.get() == "bypassed"

        cleanup:
        executor?.shutdown()
    }
}
