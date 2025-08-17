package finance.configurations

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.security.Keys
import javax.crypto.SecretKey
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.Collections
import org.springframework.security.core.authority.SimpleGrantedAuthority
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tag
import org.slf4j.LoggerFactory

@Component
class JwtAuthenticationFilter(
    private val environment: Environment,
    private val meterRegistry: MeterRegistry
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
        filterChain: FilterChain
    ) {
        var token: String? = null

        super.logger.info("Cookies received: ${request.cookies?.joinToString { it.name + "=" + it.value }}")

        // Extract token from cookies
        request.cookies?.forEach { cookie ->
            if ("token" == cookie.name) {
                token = cookie.value
                return@forEach
            }
        }

        if (token != null) {
            try {
                // Validate token using your signing key
                val key: SecretKey = Keys.hmacShaKeyFor(jwtKey.toByteArray())
                val claims: Claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload

                // Create an authentication token using the token claims (e.g., subject and roles)
                val username = claims.get("username", String::class.java)
                val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
                val auth = UsernamePasswordAuthenticationToken(username, null, authorities)
                // Set the authenticated user in the security context
                SecurityContextHolder.getContext().authentication = auth

                // Log successful authentication and increment success counter
                securityLogger.info("Authentication successful for user: {} from IP: {}",
                    username, getClientIpAddress(request))
                Counter
                    .builder("authentication.success")
                    .description("Number of successful authentication attempts")
                    .tags(
                        listOfNotNull(
                            Tag.of("username", username ?: "unknown"),
                            Tag.of("ip_address", getClientIpAddress(request))
                        )
                    )
                    .register(meterRegistry)
                    .increment()

            } catch (ex: JwtException) {
                // Log security event for failed authentication
                val clientIp = getClientIpAddress(request)
                val userAgent = request.getHeader("User-Agent") ?: "unknown"

                securityLogger.warn("JWT authentication failed from IP: {} with User-Agent: {}. Reason: {}",
                    clientIp, userAgent, ex.message)

                // Increment failure counter with detailed tags
                Counter
                    .builder("authentication.failure")
                    .description("Number of failed authentication attempts")
                    .tags(
                        listOfNotNull(
                            Tag.of("reason", ex.javaClass.simpleName),
                            Tag.of("ip_address", clientIp),
                            Tag.of("user_agent", userAgent.take(100)) // Limit user agent length
                        )
                    )
                    .register(meterRegistry)
                    .increment()

                // If token is invalid or expired, clear the context
                SecurityContextHolder.clearContext()
            }
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response)
    }

    /**
     * Extracts the real client IP address from the request, considering proxy headers
     */
    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        val xRealIp = request.getHeader("X-Real-IP")
        val xForwardedProto = request.getHeader("X-Forwarded-Proto")

        return when {
            !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
            !xRealIp.isNullOrBlank() -> xRealIp
            else -> request.remoteAddr ?: "unknown"
        }
    }
}