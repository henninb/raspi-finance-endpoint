package finance.configurations

import finance.utils.IpAddressValidator
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
        private val securityLogger = LoggerFactory.getLogger("SECURITY.${SecurityAuditFilter::class.java.simpleName}")
        private val SENSITIVE_ENDPOINTS =
            setOf(
                "/select/active",
                "/select/totals",
                "/payment/required",
            )
    }

    private data class AuditContext(
        val username: String,
        val clientIp: String,
        val userAgent: String?,
        val isAuthenticated: Boolean,
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val startTime = System.currentTimeMillis()
        val requestUri = request.requestURI
        val method = request.method
        val isSensitiveEndpoint = SENSITIVE_ENDPOINTS.any { requestUri.contains(it) }

        if (isSensitiveEndpoint) {
            logSensitiveAccess(request, "BEFORE_REQUEST")
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            val responseTime = System.currentTimeMillis() - startTime

            if (isSensitiveEndpoint) {
                logSensitiveAccess(request, response, "AFTER_REQUEST", responseTime)
                Counter
                    .builder("security.audit.endpoint.access")
                    .description("Security audit events for sensitive endpoint access")
                    .tags(
                        listOf(
                            Tag.of("endpoint", sanitizeEndpoint(requestUri)),
                            Tag.of("method", method),
                            Tag.of("status", response.status.toString()),
                            Tag.of(
                                "authenticated",
                                SecurityContextHolder
                                    .getContext()
                                    .authentication
                                    ?.isAuthenticated
                                    .toString(),
                            ),
                        ),
                    ).register(meterRegistry)
                    .increment()
            }

            if (response.status in 400..499) {
                val ctx = buildAuditContext(request)
                securityLogger.info(
                    "SECURITY_HTTP_4XX status={} method={} endpoint={} user={} ip={} responseTime={}ms",
                    response.status,
                    method,
                    requestUri,
                    ctx.username,
                    ctx.clientIp,
                    responseTime,
                )
                Counter
                    .builder("security.audit.http.4xx")
                    .description("Count of 4xx responses observed by security audit filter")
                    .tags(
                        listOf(
                            Tag.of("status", response.status.toString()),
                            Tag.of("method", method),
                            Tag.of("endpoint", sanitizeEndpoint(requestUri)),
                            Tag.of("authenticated", ctx.isAuthenticated.toString()),
                        ),
                    ).register(meterRegistry)
                    .increment()
            }
        }
    }

    private fun buildAuditContext(request: HttpServletRequest): AuditContext {
        val authentication = SecurityContextHolder.getContext().authentication
        val isAuthenticated = authentication?.isAuthenticated ?: false
        return AuditContext(
            username = if (isAuthenticated) authentication?.name ?: "unknown" else "anonymous",
            clientIp = IpAddressValidator.getClientIpAddress(request),
            userAgent = sanitizeUserAgent(request.getHeader("User-Agent")),
            isAuthenticated = isAuthenticated,
        )
    }

    private fun logSensitiveAccess(
        request: HttpServletRequest,
        phase: String,
    ) {
        val ctx = buildAuditContext(request)
        val auditType = if (ctx.isAuthenticated) "AUTHORIZED_ACCESS" else "UNAUTHORIZED_ACCESS"
        securityLogger.info(
            "SECURITY_AUDIT type={} phase={} user={} endpoint={} method={} ip={} userAgent='{}'",
            auditType,
            phase,
            ctx.username,
            request.requestURI,
            request.method,
            ctx.clientIp,
            ctx.userAgent ?: "unknown",
        )
    }

    private fun logSensitiveAccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        phase: String,
        responseTime: Long,
    ) {
        val ctx = buildAuditContext(request)
        val auditType = if (response.status == 200 && ctx.isAuthenticated) "AUTHORIZED_ACCESS" else "UNAUTHORIZED_ACCESS"

        if (response.status == 403 || response.status == 401) {
            securityLogger.warn(
                "SECURITY_VIOLATION type=UNAUTHORIZED_SENSITIVE_ACCESS user={} endpoint={} method={} ip={} status={} responseTime={}ms",
                ctx.username,
                request.requestURI,
                request.method,
                ctx.clientIp,
                response.status,
                responseTime,
            )
        }

        securityLogger.info(
            "SECURITY_AUDIT type={} phase={} user={} endpoint={} method={} ip={} status={} responseTime={}ms userAgent='{}'",
            auditType,
            phase,
            ctx.username,
            request.requestURI,
            request.method,
            ctx.clientIp,
            response.status,
            responseTime,
            ctx.userAgent ?: "unknown",
        )
    }

    private fun sanitizeUserAgent(userAgent: String?): String? = userAgent?.take(200)?.replace(Regex("[\\r\\n\\t]"), " ")

    private fun sanitizeEndpoint(uri: String): String =
        uri
            .replace(Regex("/\\d+"), "/{id}")
            .replace(Regex("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"), "/{uuid}")
            .take(50)
}
