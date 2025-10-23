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
                Counter
                    .builder("authentication.failure")
                    .description("Number of failed authentication attempts")
                    .tags(
                        listOfNotNull(
                            Tag.of("reason", "BlacklistedToken"),
                            Tag.of("ip_address", clientIp),
                        ),
                    ).register(meterRegistry)
                    .increment()
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
                    val authorities =
                        listOf(
                            SimpleGrantedAuthority("ROLE_USER"),
                            SimpleGrantedAuthority("USER"),
                        )
                    val auth = UsernamePasswordAuthenticationToken(username, null, authorities)
                    SecurityContextHolder.getContext().authentication = auth

                    securityLogger.info("Authentication successful for user: {} from IP: {}", username, getClientIpAddress(request))
                    Counter
                        .builder("authentication.success")
                        .description("Number of successful authentication attempts")
                        .tags(
                            listOfNotNull(
                                Tag.of("username", username ?: "unknown"),
                                Tag.of("ip_address", getClientIpAddress(request)),
                            ),
                        ).register(meterRegistry)
                        .increment()
                } catch (ex: JwtException) {
                    val clientIp = getClientIpAddress(request)
                    val userAgent = request.getHeader("User-Agent") ?: "unknown"
                    securityLogger.warn(
                        "JWT authentication failed from IP: {} with User-Agent: {}. Reason: {}",
                        clientIp,
                        userAgent,
                        ex.message,
                    )
                    Counter
                        .builder("authentication.failure")
                        .description("Number of failed authentication attempts")
                        .tags(
                            listOfNotNull(
                                Tag.of("reason", ex.javaClass.simpleName),
                                Tag.of("ip_address", clientIp),
                                Tag.of("user_agent", userAgent.take(100)),
                            ),
                        ).register(meterRegistry)
                        .increment()
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
