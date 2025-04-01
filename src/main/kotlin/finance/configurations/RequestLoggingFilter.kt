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
        // Wrap the request so we can read the content later.
        val wrappedRequest = if (request is ContentCachingRequestWrapper) {
            request
        } else {
            ContentCachingRequestWrapper(request)
        }
        try {
            filterChain.doFilter(wrappedRequest, response)
        } finally {
            // Retrieve the cached request body as a String.
            val payload = String(wrappedRequest.contentAsByteArray, StandardCharsets.UTF_8)
            logger.info("Request URI: ${request.requestURI}, Raw Payload: $payload")
        }
    }
}
