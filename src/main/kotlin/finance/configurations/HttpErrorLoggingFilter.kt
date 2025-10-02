package finance.configurations

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper

class HttpErrorLoggingFilter(
    private val meterRegistry: MeterRegistry,
) : OncePerRequestFilter() {
    companion object {
        private val securityLogger = LoggerFactory.getLogger("SECURITY.${HttpErrorLoggingFilter::class.java.simpleName}")
        private val httpLogger = LoggerFactory.getLogger("HTTP.${HttpErrorLoggingFilter::class.java.simpleName}")

        private val SENSITIVE_HEADERS =
            setOf(
                "authorization",
                "cookie",
                "set-cookie",
                "x-auth-token",
                "token",
                "jwt",
                "api-key",
                "x-api-key",
                "password",
            )

        private val SENSITIVE_PARAMS =
            setOf(
                "password",
                "token",
                "jwt",
                "secret",
                "key",
                "auth",
                "credential",
                "ssn",
                "credit",
                "account",
            )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val cachingResponse = ContentCachingResponseWrapper(response)
        val startTime = System.currentTimeMillis()

        try {
            filterChain.doFilter(request, cachingResponse)
        } finally {
            val responseTime = System.currentTimeMillis() - startTime
            logHttpResponse(request, cachingResponse, responseTime)
            cachingResponse.copyBodyToResponse()
        }
    }

    private fun logHttpResponse(
        request: HttpServletRequest,
        response: ContentCachingResponseWrapper,
        responseTime: Long,
    ) {
        val status = response.status

        if (status >= 400) {
            val clientIp = getClientIpAddress(request)
            val userAgent = sanitizeUserAgent(request.getHeader("User-Agent"))
            val method = request.method
            val uri = sanitizeUri(request.requestURI)
            val queryString = sanitizeQueryString(request.queryString)
            val referer = sanitizeHeader(request.getHeader("Referer"))

            val logMessage =
                buildString {
                    append("HTTP_ERROR status=$status method=$method uri=$uri")
                    if (!queryString.isNullOrBlank()) append(" query=$queryString")
                    append(" ip=$clientIp responseTime=${responseTime}ms")
                    if (!userAgent.isNullOrBlank()) append(" userAgent='$userAgent'")
                    if (!referer.isNullOrBlank()) append(" referer='$referer'")
                }

            when {
                status >= 500 -> {
                    securityLogger.error(logMessage)
                    incrementErrorCounter("5xx", status, method, uri)
                }
                status == 404 -> {
                    httpLogger.warn("$logMessage type=NOT_FOUND")
                    incrementErrorCounter("4xx", status, method, uri)
                }
                status == 403 -> {
                    securityLogger.warn("$logMessage type=FORBIDDEN")
                    incrementErrorCounter("4xx", status, method, uri)
                }
                status == 401 -> {
                    securityLogger.warn("$logMessage type=UNAUTHORIZED")
                    incrementErrorCounter("4xx", status, method, uri)
                }
                status >= 400 -> {
                    httpLogger.warn("$logMessage type=CLIENT_ERROR")
                    incrementErrorCounter("4xx", status, method, uri)
                }
            }
        }
    }

    private fun incrementErrorCounter(
        category: String,
        status: Int,
        method: String,
        uri: String,
    ) {
        Counter
            .builder("http.error.responses")
            .description("HTTP error responses by status code and endpoint")
            .tags(
                listOf(
                    Tag.of("category", category),
                    Tag.of("status", status.toString()),
                    Tag.of("method", method),
                    Tag.of("endpoint", sanitizeEndpointForMetrics(uri)),
                ),
            ).register(meterRegistry)
            .increment()
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

    private fun sanitizeUserAgent(userAgent: String?): String? = userAgent?.take(200)?.replace(Regex("[\\r\\n\\t]"), " ")

    private fun sanitizeHeader(header: String?): String? = header?.take(200)?.replace(Regex("[\\r\\n\\t]"), " ")

    private fun sanitizeUri(uri: String): String = uri.replace(Regex("[\\r\\n\\t]"), "")

    private fun sanitizeQueryString(queryString: String?): String? {
        if (queryString.isNullOrBlank()) return null

        return queryString
            .split("&")
            .map { param ->
                val parts = param.split("=", limit = 2)
                val key = parts[0].lowercase()
                if (SENSITIVE_PARAMS.any { key.contains(it) }) {
                    "${parts[0]}=***"
                } else {
                    param
                }
            }.joinToString("&")
            .replace(Regex("[\\r\\n\\t]"), "")
    }

    private fun sanitizeEndpointForMetrics(uri: String): String =
        uri
            .replace(Regex("/\\d+"), "/{id}")
            .replace(Regex("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"), "/{uuid}")
            .replace(Regex("/[0-9a-fA-F]{32,}"), "/{hash}")
            .take(100)
}
