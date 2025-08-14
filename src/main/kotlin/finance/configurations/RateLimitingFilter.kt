package finance.configurations

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Component
class RateLimitingFilter : OncePerRequestFilter() {

    companion object {
        private val securityLogger = LoggerFactory.getLogger("SECURITY.${RateLimitingFilter::class.java.simpleName}")
        private const val DEFAULT_RATE_LIMIT = 100
        private const val DEFAULT_WINDOW_SIZE_MINUTES = 1L
        private const val CLEANUP_INTERVAL_MINUTES = 5L
    }

    @Value("\${custom.security.rate-limit.requests-per-minute:100}")
    private var rateLimitPerMinute: Int = DEFAULT_RATE_LIMIT

    @Value("\${custom.security.rate-limit.window-size-minutes:1}")
    private var windowSizeMinutes: Long = DEFAULT_WINDOW_SIZE_MINUTES

    @Value("\${custom.security.rate-limit.enabled:true}")
    private var rateLimitingEnabled: Boolean = true

    // Store request counts per IP address
    private val requestCounts = ConcurrentHashMap<String, RequestCounter>()

    init {
        // Scheduled cleanup of old entries every 5 minutes
        val executor = Executors.newScheduledThreadPool(1) { r ->
            Thread(r, "rate-limit-cleanup").apply { isDaemon = true }
        }
        executor.scheduleWithFixedDelay(
            ::cleanupOldEntries,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
    }

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!rateLimitingEnabled) {
            filterChain.doFilter(request, response)
            return
        }

        val clientIp = getClientIpAddress(request)
        val currentTime = System.currentTimeMillis()
        val windowStart = currentTime - TimeUnit.MINUTES.toMillis(windowSizeMinutes)

        val counter = requestCounts.computeIfAbsent(clientIp) { RequestCounter() }

        // Check if rate limit exceeded
        if (counter.isRateLimitExceeded(windowStart, rateLimitPerMinute)) {
            securityLogger.warn(
                "Rate limit exceeded for IP: {} - {} requests in {} minutes (limit: {})",
                clientIp, counter.getRequestCount(windowStart), windowSizeMinutes, rateLimitPerMinute
            )
            
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.setHeader("X-RateLimit-Limit", rateLimitPerMinute.toString())
            response.setHeader("X-RateLimit-Remaining", "0")
            response.setHeader("X-RateLimit-Reset", 
                ((currentTime + TimeUnit.MINUTES.toMillis(windowSizeMinutes)) / 1000).toString())
            response.contentType = "application/json"
            response.writer.write("""{"error":"Rate limit exceeded","message":"Too many requests"}""")
            return
        }

        // Record the request
        counter.recordRequest(currentTime)

        // Add rate limit headers
        val remainingRequests = maxOf(0, rateLimitPerMinute - counter.getRequestCount(windowStart))
        response.setHeader("X-RateLimit-Limit", rateLimitPerMinute.toString())
        response.setHeader("X-RateLimit-Remaining", remainingRequests.toString())
        response.setHeader("X-RateLimit-Reset", 
            ((currentTime + TimeUnit.MINUTES.toMillis(windowSizeMinutes)) / 1000).toString())

        filterChain.doFilter(request, response)
    }

    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        val xRealIp = request.getHeader("X-Real-IP")
        
        return when {
            !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
            !xRealIp.isNullOrBlank() -> xRealIp
            else -> request.remoteAddr ?: "unknown"
        }
    }

    private fun cleanupOldEntries() {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(windowSizeMinutes * 2)
        val iterator = requestCounts.iterator()
        var cleanedCount = 0
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val counter = entry.value
            
            // Clean old requests from counter
            counter.cleanup(cutoffTime)
            
            // Remove empty counters
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
        private val requests = mutableListOf<AtomicLong>()
        private val lock = Any()

        fun recordRequest(timestamp: Long) {
            synchronized(lock) {
                requests.add(AtomicLong(timestamp))
            }
        }

        fun isRateLimitExceeded(windowStart: Long, limit: Int): Boolean {
            return getRequestCount(windowStart) >= limit
        }

        fun getRequestCount(windowStart: Long): Int {
            synchronized(lock) {
                return requests.count { it.get() >= windowStart }
            }
        }

        fun cleanup(cutoffTime: Long) {
            synchronized(lock) {
                requests.removeIf { it.get() < cutoffTime }
            }
        }

        fun isEmpty(cutoffTime: Long): Boolean {
            synchronized(lock) {
                return requests.none { it.get() >= cutoffTime }
            }
        }
    }
}