package finance.configurations

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Filter that assigns a unique correlation ID to each request for distributed tracing.
 * The correlation ID is:
 * - Read from X-Correlation-ID header if present
 * - Generated as a UUID if not present
 * - Added to MDC for logging context
 * - Returned in response headers
 *
 * This allows tracing a single request through all logs (service methods, SQL queries, etc.)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : Filter {
    companion object {
        private val logger = LoggerFactory.getLogger(CorrelationIdFilter::class.java)
        private const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        private const val MDC_CORRELATION_ID_KEY = "correlationId"
    }

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        try {
            // Get or generate correlation ID
            val correlationId = getOrGenerateCorrelationId(httpRequest)

            // Add to MDC for logging context
            MDC.put(MDC_CORRELATION_ID_KEY, correlationId)

            // Add to response headers
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId)

            logger.debug(
                "[{}] Request started: {} {}",
                correlationId,
                httpRequest.method,
                httpRequest.requestURI,
            )

            // Continue with the filter chain
            chain.doFilter(request, response)

            logger.debug(
                "[{}] Request completed: {} {} - Status: {}",
                correlationId,
                httpRequest.method,
                httpRequest.requestURI,
                httpResponse.status,
            )
        } finally {
            // Always clear MDC to prevent memory leaks
            MDC.clear()
        }
    }

    /**
     * Get correlation ID from request header or generate a new one
     */
    private fun getOrGenerateCorrelationId(request: HttpServletRequest): String {
        val headerValue = request.getHeader(CORRELATION_ID_HEADER)
        return if (!headerValue.isNullOrBlank()) {
            headerValue.trim()
        } else {
            UUID.randomUUID().toString()
        }
    }
}
