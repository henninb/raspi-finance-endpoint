package finance.configurations

import finance.services.TokenBlacklistService
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import javax.crypto.SecretKey

class JwtAuthenticationFilter(
    private val meterRegistry: MeterRegistry,
    private val tokenBlacklistService: TokenBlacklistService,
) : OncePerRequestFilter() {
    @Value("\${custom.project.jwt.key}")
    private lateinit var jwtKey: String

    companion object {
        private val securityLogger = LoggerFactory.getLogger("SECURITY.${JwtAuthenticationFilter::class.java.simpleName}")
    }

    // Cache counters to avoid recreation on every request
    private val authSuccessCounter: Counter by lazy {
        Counter
            .builder("authentication.success")
            .description("Number of successful authentication attempts")
            .register(meterRegistry)
    }

    private val authFailureCounter: Counter by lazy {
        Counter
            .builder("authentication.failure")
            .description("Number of failed authentication attempts")
            .register(meterRegistry)
    }

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        var token: String? =
            run {
                request.cookies?.firstOrNull { it.name == "token" }?.value
                    ?: request
                        .getHeader("Cookie")
                        ?.split(';')
                        ?.map { it.trim() }
                        ?.firstOrNull { it.startsWith("token=") }
                        ?.substringAfter("token=")
            }

        if (token.isNullOrBlank()) {
            val authHeader = request.getHeader("Authorization")
            if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ")) {
                token = authHeader.removePrefix("Bearer ").trim()
            }
        }

        if (!token.isNullOrBlank()) {
            // Check if token is blacklisted before processing
            if (tokenBlacklistService.isBlacklisted(token)) {
                val clientIp = getClientIpAddress(request)
                securityLogger.warn("Blacklisted token used from IP: {}", clientIp)
                authFailureCounter.increment()
                meterRegistry
                    .counter(
                        "authentication.failure.details",
                        "reason",
                        "BlacklistedToken",
                        "ip_address",
                        clientIp,
                    ).increment()
                SecurityContextHolder.clearContext()
            } else {
                try {
                    val key: SecretKey = Keys.hmacShaKeyFor(jwtKey.toByteArray())
                    val claims: Claims =
                        Jwts
                            .parser()
                            .verifyWith(key)
                            .build()
                            .parseSignedClaims(token)
                            .payload

                    val username = claims.get("username", String::class.java)
                    if (username.isNullOrBlank()) {
                        securityLogger.warn("JWT token missing username claim from IP: {}", getClientIpAddress(request))
                        authFailureCounter.increment()
                        SecurityContextHolder.clearContext()
                    } else {
                        val authorities =
                            listOf(
                                SimpleGrantedAuthority("ROLE_USER"),
                                SimpleGrantedAuthority("USER"),
                            )
                        val auth = UsernamePasswordAuthenticationToken(username, null, authorities)
                        SecurityContextHolder.getContext().authentication = auth

                        val clientIp = getClientIpAddress(request)
                        securityLogger.info("Authentication successful for user: {} from IP: {}", username, clientIp)
                        authSuccessCounter.increment()
                        meterRegistry
                            .counter(
                                "authentication.success.details",
                                "username",
                                username.take(50),
                                "ip_address",
                                clientIp,
                            ).increment()
                    }
                } catch (ex: JwtException) {
                    val clientIp = getClientIpAddress(request)
                    val userAgent = request.getHeader("User-Agent") ?: "unknown"
                    securityLogger.warn(
                        "JWT authentication failed from IP: {} with User-Agent: {}. Reason: {}",
                        clientIp,
                        userAgent,
                        ex.message,
                    )
                    authFailureCounter.increment()
                    meterRegistry
                        .counter(
                            "authentication.failure.details",
                            "reason",
                            ex.javaClass.simpleName,
                            "ip_address",
                            clientIp,
                            "user_agent",
                            userAgent.take(50),
                        ).increment()
                    SecurityContextHolder.clearContext()
                } catch (ex: Exception) {
                    val clientIp = getClientIpAddress(request)
                    securityLogger.error(
                        "Unexpected error during JWT authentication from IP: {}. Error: {}",
                        clientIp,
                        ex.message,
                        ex,
                    )
                    authFailureCounter.increment()
                    SecurityContextHolder.clearContext()
                }
            }
        }

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
}
