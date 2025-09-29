package finance.configurations

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import java.nio.charset.StandardCharsets

@Component
open class RequestLoggingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val wrappedRequest =
            if (request is ContentCachingRequestWrapper) {
                request
            } else {
                ContentCachingRequestWrapper(request, 1024)
            }

        try {
            filterChain.doFilter(wrappedRequest, response)
        } finally {
            logRequest(wrappedRequest)
        }
    }

    private fun logRequest(request: ContentCachingRequestWrapper) {
        val requestBody = String(request.contentAsByteArray, StandardCharsets.UTF_8)
        if (requestBody.isNotEmpty()) {
            logger.info("Request URI: ${request.requestURI}, Request Body: $requestBody")
        } else {
            logger.info("Request URI: ${request.requestURI}")
        }
    }
}
