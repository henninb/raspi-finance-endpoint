package finance.configurations

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.JwtException
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

@Component
class JwtAuthenticationFilter(private val environment: Environment) : OncePerRequestFilter() {
    @Value("\${custom.project.jwt.key}")
    private lateinit var jwtKey: String

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        var token: String? = null

        logger.info("Cookies received: ${request.cookies?.joinToString { it.name + "=" + it.value }}")

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
                val claims: Claims = Jwts.parser()
                    .setSigningKey(jwtKey.toByteArray())
                    .parseClaimsJws(token)
                    .body

                // Create an authentication token using the token claims (e.g., subject and roles)
                val username = claims.get("username", String::class.java)
                val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
                val auth = UsernamePasswordAuthenticationToken(username, null, authorities)
                // Set the authenticated user in the security context
                SecurityContextHolder.getContext().authentication = auth

            } catch (ex: JwtException) {
                // If token is invalid or expired, clear the context
                SecurityContextHolder.clearContext()
            }
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response)
    }
}