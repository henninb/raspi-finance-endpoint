package finance.configurations

import finance.utils.IpAddressValidator
import jakarta.annotation.PreDestroy
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class RateLimitingFilter : OncePerRequestFilter() {
    companion object {
        private val securityLogger = LoggerFactory.getLogger("SECURITY.${RateLimitingFilter::class.java.simpleName}")
        private const val DEFAULT_RATE_LIMIT = 500
        private const val DEFAULT_WINDOW_SIZE_MINUTES = 1L
        private const val CLEANUP_INTERVAL_MINUTES = 5L
        private const val MAX_TRACKED_IPS = 100_000
    }

    @Value("\${custom.security.rate-limit.requests-per-minute:500}")
    private var rateLimitPerMinute: Int = DEFAULT_RATE_LIMIT

    @Value("\${custom.security.rate-limit.window-size-minutes:1}")
    private var windowSizeMinutes: Long = DEFAULT_WINDOW_SIZE_MINUTES

    @Value("\${custom.security.rate-limit.enabled:true}")
    private var rateLimitingEnabled: Boolean = true

    private val requestCounts = ConcurrentHashMap<String, RequestCounter>()
    private val cleanupExecutor: ScheduledExecutorService =
        Executors.newScheduledThreadPool(1) { r ->
            Thread(r, "rate-limit-cleanup").apply { isDaemon = true }
        }

    init {
        cleanupExecutor.scheduleWithFixedDelay(
            ::cleanupOldEntries,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
    }

    @PreDestroy
    fun shutdown() {
        cleanupExecutor.shutdown()
        if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            cleanupExecutor.shutdownNow()
        }
    }

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (!rateLimitingEnabled) {
            filterChain.doFilter(request, response)
            return
        }

        val clientIp = IpAddressValidator.getClientIpAddress(request)
        val currentTime = System.currentTimeMillis()
        val windowStart = currentTime - TimeUnit.MINUTES.toMillis(windowSizeMinutes)

        if (requestCounts.size >= MAX_TRACKED_IPS && !requestCounts.containsKey(clientIp)) {
            securityLogger.warn("Rate limit map at capacity ({}) — dropping tracking for IP: {}", MAX_TRACKED_IPS, clientIp)
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json"
            response.writer.write("""{"error":"Rate limit exceeded","message":"Too many requests"}""")
            return
        }

        val counter = requestCounts.computeIfAbsent(clientIp) { RequestCounter() }

        if (counter.isRateLimitExceeded(windowStart, rateLimitPerMinute)) {
            securityLogger.warn(
                "Rate limit exceeded for IP: {} - {} requests in {} minutes (limit: {})",
                clientIp,
                counter.getRequestCount(windowStart),
                windowSizeMinutes,
                rateLimitPerMinute,
            )

            val resetEpochSecs = (currentTime + TimeUnit.MINUTES.toMillis(windowSizeMinutes)) / 1000
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.setHeader("X-RateLimit-Limit", rateLimitPerMinute.toString())
            response.setHeader("X-RateLimit-Remaining", "0")
            response.setHeader("X-RateLimit-Reset", resetEpochSecs.toString())
            response.setHeader("Retry-After", TimeUnit.MINUTES.toSeconds(windowSizeMinutes).toString())
            response.contentType = "application/json"
            response.writer.write("""{"error":"Rate limit exceeded","message":"Too many requests"}""")
            return
        }

        counter.recordRequest(currentTime)

        val remainingRequests = maxOf(0, rateLimitPerMinute - counter.getRequestCount(windowStart))
        response.setHeader("X-RateLimit-Limit", rateLimitPerMinute.toString())
        response.setHeader("X-RateLimit-Remaining", remainingRequests.toString())
        response.setHeader(
            "X-RateLimit-Reset",
            ((currentTime + TimeUnit.MINUTES.toMillis(windowSizeMinutes)) / 1000).toString(),
        )

        filterChain.doFilter(request, response)
    }

    private fun cleanupOldEntries() {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(windowSizeMinutes * 2)
        val iterator = requestCounts.iterator()
        var cleanedCount = 0

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val counter = entry.value

            counter.cleanup(cutoffTime)

            if (counter.isEmpty(cutoffTime)) {
                iterator.remove()
                cleanedCount++
            }
        }

        if (cleanedCount > 0) {
            securityLogger.debug("Cleaned up {} old rate limiting entries", cleanedCount)
        }
    }

    private class RequestCounter {
        private val requests = mutableListOf<Long>()
        private val lock = Any()

        fun recordRequest(timestamp: Long) {
            synchronized(lock) {
                requests.add(timestamp)
            }
        }

        fun isRateLimitExceeded(
            windowStart: Long,
            limit: Int,
        ): Boolean = getRequestCount(windowStart) >= limit

        fun getRequestCount(windowStart: Long): Int {
            synchronized(lock) {
                return requests.count { it >= windowStart }
            }
        }

        fun cleanup(cutoffTime: Long) {
            synchronized(lock) {
                requests.removeIf { it < cutoffTime }
            }
        }

        fun isEmpty(cutoffTime: Long): Boolean {
            synchronized(lock) {
                return requests.none { it >= cutoffTime }
            }
        }
    }
}
