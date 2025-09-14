package finance.configurations



import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets

@Component
open class RequestLoggingFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(request, response)
        } finally {
            // Simple logging without caching request body to avoid Spring Boot 4.0 compatibility issues
            logger.info("Request URI: ${request.requestURI}")
        }
    }
}
