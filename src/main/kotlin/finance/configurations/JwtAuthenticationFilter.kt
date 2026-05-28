package finance.configurations

import finance.services.JwtTokenService
import finance.services.TokenBlacklistService
import finance.utils.IpAddressValidator
import io.jsonwebtoken.JwtException
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.logging.log4j.LogManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

class JwtAuthenticationFilter(
    private val meterRegistry: MeterRegistry,
    private val tokenBlacklistService: TokenBlacklistService,
    private val jwtTokenService: JwtTokenService,
    private val customProperties: CustomProperties,
) : OncePerRequestFilter() {
    companion object {
        private val securityLogger = LogManager.getLogger("SECURITY.${JwtAuthenticationFilter::class.java.simpleName}")
    }

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
        val token = jwtTokenService.extractToken(request)

        if (!token.isNullOrBlank()) {
            if (tokenBlacklistService.isBlacklisted(token)) {
                val clientIp = IpAddressValidator.getClientIpAddress(request)
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
                    val claims = jwtTokenService.parseClaims(token)
                    val username = claims.get(JwtTokenService.CLAIM_USERNAME, String::class.java)
                    if (username.isNullOrBlank()) {
                        securityLogger.warn("JWT token missing username claim from IP: {}", IpAddressValidator.getClientIpAddress(request))
                        authFailureCounter.increment()
                        SecurityContextHolder.clearContext()
                    } else {
                        val authorities =
                            buildList {
                                add(SimpleGrantedAuthority("ROLE_USER"))
                                add(SimpleGrantedAuthority("USER"))
                                if (customProperties.adminUsers.any { it.equals(username, ignoreCase = true) }) {
                                    add(SimpleGrantedAuthority("ROLE_ADMIN"))
                                }
                            }
                        val auth = UsernamePasswordAuthenticationToken(username, null, authorities)
                        SecurityContextHolder.getContext().authentication = auth

                        val clientIp = IpAddressValidator.getClientIpAddress(request)
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
                    val clientIp = IpAddressValidator.getClientIpAddress(request)
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
                    val clientIp = IpAddressValidator.getClientIpAddress(request)
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
}
