package finance.configurations

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/** Bundles all resilience4j beans into a single injectable unit. */
data class ResilienceComponents(
    val databaseResilienceConfig: DatabaseResilienceConfiguration,
    val circuitBreaker: CircuitBreaker,
    val retry: Retry,
    val timeLimiter: TimeLimiter,
    val scheduledExecutorService: ScheduledExecutorService,
) {
    companion object {
        private val noOpInstance: ResilienceComponents by lazy {
            val config = NoOpDatabaseResilienceConfiguration()
            val executor =
                Executors.newSingleThreadScheduledExecutor { r ->
                    Thread(r, "no-op-resilience").apply { isDaemon = true }
                }
            ResilienceComponents(
                databaseResilienceConfig = config,
                circuitBreaker = CircuitBreaker.ofDefaults("noOp"),
                retry = Retry.ofDefaults("noOp"),
                timeLimiter = TimeLimiter.of("noOp", TimeLimiterConfig.ofDefaults()),
                scheduledExecutorService = executor,
            )
        }

        /** Returns a shared no-op instance that bypasses all resilience patterns — use in tests only. */
        @JvmStatic
        fun noOp(): ResilienceComponents = noOpInstance
    }
}

/** Passes all operations through directly, bypassing circuit-breaker, retry, and time-limiter. */
class NoOpDatabaseResilienceConfiguration : DatabaseResilienceConfiguration() {
    override fun <T> executeWithResilience(
        operation: () -> T,
        circuitBreaker: CircuitBreaker,
        retry: Retry,
        timeLimiter: TimeLimiter,
        executor: ScheduledExecutorService,
    ): CompletableFuture<T> = CompletableFuture.completedFuture(operation())
}
