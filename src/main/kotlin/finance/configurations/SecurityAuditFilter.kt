package finance.configurations

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class SecurityAuditFilter(
    private val meterRegistry: MeterRegistry,
) : OncePerRequestFilter() {
    companion object {
        private val securityLogger = LoggerFactory.getLogger("SECURITY.finance.controllers.AccountController")
        private val SENSITIVE_ENDPOINTS =
            setOf(
                "/select/active",
                "/select/totals",
                "/payment/required",
            )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val startTime = System.currentTimeMillis()
        val requestUri = request.requestURI
        val method = request.method
        val clientIp = getClientIpAddress(request)

        // Check if this is a sensitive endpoint access
        val isSensitiveEndpoint = SENSITIVE_ENDPOINTS.any { requestUri.contains(it) }

        if (isSensitiveEndpoint) {
            logSecurityAuditEvent(request, "BEFORE_REQUEST")
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            val responseTime = System.currentTimeMillis() - startTime

            if (isSensitiveEndpoint) {
                logSecurityAuditEvent(request, response, "AFTER_REQUEST", responseTime)

                // Increment security audit counter
                Counter.builder("security.audit.endpoint.access")
                    .description("Security audit events for sensitive endpoint access")
                    .tags(
                        listOf(
                            Tag.of("endpoint", sanitizeEndpoint(requestUri)),
                            Tag.of("method", method),
                            Tag.of("status", response.status.toString()),
                            Tag.of("authenticated", SecurityContextHolder.getContext().authentication?.isAuthenticated.toString()),
                        ),
                    )
                    .register(meterRegistry)
                    .increment()
            }

            // Log and count all 4xx responses to help trace 400 sources
            if (response.status in 400..499) {
                val authentication = SecurityContextHolder.getContext().authentication
                val isAuthenticated = authentication?.isAuthenticated ?: false
                val username = if (isAuthenticated) authentication?.name ?: "unknown" else "anonymous"

                securityLogger.info(
                    "SECURITY_HTTP_4XX status={} method={} endpoint={} user={} ip={} responseTime={}ms",
                    response.status,
                    method,
                    requestUri,
                    username,
                    clientIp,
                    responseTime,
                )

                Counter.builder("security.audit.http.4xx")
                    .description("Count of 4xx responses observed by security audit filter")
                    .tags(
                        listOf(
                            Tag.of("status", response.status.toString()),
                            Tag.of("method", method),
                            Tag.of("endpoint", sanitizeEndpoint(requestUri)),
                            Tag.of("authenticated", isAuthenticated.toString()),
                        ),
                    )
                    .register(meterRegistry)
                    .increment()
            }
        }
    }

    private fun logSecurityAuditEvent(
        request: HttpServletRequest,
        phase: String,
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        val isAuthenticated = authentication?.isAuthenticated ?: false
        val username = if (isAuthenticated) authentication?.name ?: "unknown" else "anonymous"
        val clientIp = getClientIpAddress(request)
        val userAgent = sanitizeUserAgent(request.getHeader("User-Agent"))

        val auditType = if (isAuthenticated) "AUTHORIZED_ACCESS" else "UNAUTHORIZED_ACCESS"

        securityLogger.info(
            "SECURITY_AUDIT type={} phase={} user={} endpoint={} method={} ip={} userAgent='{}'",
            auditType,
            phase,
            username,
            request.requestURI,
            request.method,
            clientIp,
            userAgent ?: "unknown",
        )
    }

    private fun logSecurityAuditEvent(
        request: HttpServletRequest,
        response: HttpServletResponse,
        phase: String,
        responseTime: Long,
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        val isAuthenticated = authentication?.isAuthenticated ?: false
        val username = if (isAuthenticated) authentication?.name ?: "unknown" else "anonymous"
        val clientIp = getClientIpAddress(request)
        val userAgent = sanitizeUserAgent(request.getHeader("User-Agent"))

        val auditType =
            if (response.status == 200 && isAuthenticated) {
                "AUTHORIZED_ACCESS"
            } else {
                "UNAUTHORIZED_ACCESS"
            }

        // Log security violations for non-API routes
        if (request.requestURI.contains("/select/active") &&
            !request.requestURI.startsWith("/api/") &&
            (response.status == 403 || response.status == 401)
        ) {
            securityLogger.warn(
                "SECURITY_VIOLATION type=NON_API_ROUTE_ACCESS user={} endpoint={} method={} ip={} status={} responseTime={}ms",
                username,
                request.requestURI,
                request.method,
                clientIp,
                response.status,
                responseTime,
            )
        }

        securityLogger.info(
            "SECURITY_AUDIT type={} phase={} user={} endpoint={} method={} ip={} status={} responseTime={}ms userAgent='{}'",
            auditType, phase, username, request.requestURI, request.method, clientIp,
            response.status, responseTime, userAgent ?: "unknown",
        )
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

    private fun sanitizeUserAgent(userAgent: String?): String? {
        return userAgent?.take(200)?.replace(Regex("[\\r\\n\\t]"), " ")
    }

    private fun sanitizeEndpoint(uri: String): String {
        return uri.replace(Regex("/\\d+"), "/{id}")
            .replace(Regex("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"), "/{uuid}")
            .take(50)
    }
}
