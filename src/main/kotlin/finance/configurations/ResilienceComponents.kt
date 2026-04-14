package finance.configurations

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter
import java.util.concurrent.ScheduledExecutorService

/** Bundles all optional resilience4j beans into a single injectable unit. */
data class ResilienceComponents(
    val databaseResilienceConfig: DatabaseResilienceConfiguration,
    val circuitBreaker: CircuitBreaker,
    val retry: Retry,
    val timeLimiter: TimeLimiter,
    val scheduledExecutorService: ScheduledExecutorService,
)
