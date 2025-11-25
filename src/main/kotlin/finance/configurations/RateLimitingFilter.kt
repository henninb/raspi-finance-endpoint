package finance.configurations

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class RateLimitingFilter : OncePerRequestFilter() {
    companion object {
        private val securityLogger = LoggerFactory.getLogger("SECURITY.${RateLimitingFilter::class.java.simpleName}")
        private const val DEFAULT_RATE_LIMIT = 500
        private const val DEFAULT_WINDOW_SIZE_MINUTES = 1L
        private const val CLEANUP_INTERVAL_MINUTES = 5L
    }

    @Value("\${custom.security.rate-limit.requests-per-minute:500}")
    private var rateLimitPerMinute: Int = DEFAULT_RATE_LIMIT

    @Value("\${custom.security.rate-limit.window-size-minutes:1}")
    private var windowSizeMinutes: Long = DEFAULT_WINDOW_SIZE_MINUTES

    @Value("\${custom.security.rate-limit.enabled:true}")
    private var rateLimitingEnabled: Boolean = true

    // Store request counts per IP address
    private val requestCounts = ConcurrentHashMap<String, RequestCounter>()

    init {
        // Scheduled cleanup of old entries every 5 minutes
        val executor =
            Executors.newScheduledThreadPool(1) { r ->
                Thread(r, "rate-limit-cleanup").apply { isDaemon = true }
            }
        executor.scheduleWithFixedDelay(
            ::cleanupOldEntries,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
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

        val clientIp = getClientIpAddress(request)

        // Log suspicious activity if proxy headers were ignored
        val hasProxyHeaders =
            !request.getHeader("X-Forwarded-For").isNullOrBlank() ||
                !request.getHeader("X-Real-IP").isNullOrBlank()
        if (hasProxyHeaders && !isFromTrustedProxy(request.remoteAddr ?: "unknown")) {
            securityLogger.warn(
                "Proxy headers ignored from untrusted source: {} - X-Forwarded-For: {}, X-Real-IP: {}",
                request.remoteAddr,
                request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Real-IP"),
            )
        }
        val currentTime = System.currentTimeMillis()
        val windowStart = currentTime - TimeUnit.MINUTES.toMillis(windowSizeMinutes)

        val counter = requestCounts.computeIfAbsent(clientIp) { RequestCounter() }

        // Check if rate limit exceeded
        if (counter.isRateLimitExceeded(windowStart, rateLimitPerMinute)) {
            securityLogger.warn(
                "Rate limit exceeded for IP: {} - {} requests in {} minutes (limit: {})",
                clientIp,
                counter.getRequestCount(windowStart),
                windowSizeMinutes,
                rateLimitPerMinute,
            )

            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.setHeader("X-RateLimit-Limit", rateLimitPerMinute.toString())
            response.setHeader("X-RateLimit-Remaining", "0")
            response.setHeader(
                "X-RateLimit-Reset",
                ((currentTime + TimeUnit.MINUTES.toMillis(windowSizeMinutes)) / 1000).toString(),
            )
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
        response.setHeader(
            "X-RateLimit-Reset",
            ((currentTime + TimeUnit.MINUTES.toMillis(windowSizeMinutes)) / 1000).toString(),
        )

        filterChain.doFilter(request, response)
    }

    private fun getClientIpAddress(request: HttpServletRequest): String {
        val clientIp = request.remoteAddr ?: "unknown"

        // Only trust proxy headers from known trusted networks
        return if (isFromTrustedProxy(clientIp)) {
            val xForwardedFor = request.getHeader("X-Forwarded-For")
            val xRealIp = request.getHeader("X-Real-IP")
            when {
                !xForwardedFor.isNullOrBlank() -> {
                    // Take the first IP from X-Forwarded-For chain and validate it
                    val forwardedIp = xForwardedFor.split(",")[0].trim()
                    if (isValidIpAddress(forwardedIp)) forwardedIp else clientIp
                }

                !xRealIp.isNullOrBlank() -> {
                    if (isValidIpAddress(xRealIp)) xRealIp else clientIp
                }

                else -> {
                    clientIp
                }
            }
        } else {
            // For untrusted sources, only use the direct connection IP
            securityLogger.debug("Ignoring proxy headers from untrusted IP: {}", clientIp)
            clientIp
        }
    }

    private fun isFromTrustedProxy(clientIp: String): Boolean {
        if (clientIp == "unknown") return false

        val trustedNetworks =
            listOf(
                // Private Class A
                "10.0.0.0/8",
                // Private Class B
                "172.16.0.0/12",
                // Private Class C
                "192.168.0.0/16",
                // Loopback
                "127.0.0.0/8",
            )

        return try {
            trustedNetworks.any { network -> isIpInNetwork(clientIp, network) }
        } catch (e: Exception) {
            securityLogger.warn("Error checking trusted proxy for IP: {} - {}", clientIp, e.message)
            false
        }
    }

    private fun isIpInNetwork(
        ip: String,
        network: String,
    ): Boolean {
        try {
            val parts = network.split("/")
            val networkAddress = parts[0]
            val prefixLength = parts[1].toInt()

            val ipAddr = InetAddress.getByName(ip)
            val networkAddr = InetAddress.getByName(networkAddress)

            val ipBytes = ipAddr.address
            val networkBytes = networkAddr.address

            if (ipBytes.size != networkBytes.size) return false

            val bytesToCheck = prefixLength / 8
            val bitsInLastByte = prefixLength % 8

            // Check full bytes
            for (i in 0 until bytesToCheck) {
                if (ipBytes[i] != networkBytes[i]) return false
            }

            // Check remaining bits in the last byte if needed
            if (bitsInLastByte > 0 && bytesToCheck < ipBytes.size) {
                val mask = (0xFF shl (8 - bitsInLastByte)).toByte()
                val ipByte = (ipBytes[bytesToCheck].toInt() and 0xFF) and mask.toInt()
                val networkByte = (networkBytes[bytesToCheck].toInt() and 0xFF) and mask.toInt()
                if (ipByte != networkByte) return false
            }

            return true
        } catch (e: Exception) {
            securityLogger.debug("Error checking IP {} against network {} - {}", ip, network, e.message)
            return false
        }
    }

    private fun isValidIpAddress(ip: String): Boolean =
        try {
            InetAddress.getByName(ip)
            // Basic validation - reject obvious spoofing attempts
            ip.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$".toRegex()) ||
                ip.matches("^([0-9a-fA-F]{0,4}:){1,7}[0-9a-fA-F]{0,4}$".toRegex()) // Basic IPv6
        } catch (e: Exception) {
            false
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

        fun isRateLimitExceeded(
            windowStart: Long,
            limit: Int,
        ): Boolean = getRequestCount(windowStart) >= limit

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
