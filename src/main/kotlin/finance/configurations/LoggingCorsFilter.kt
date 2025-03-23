package finance.configurations

import org.slf4j.LoggerFactory
import org.springframework.web.filter.CorsFilter
import org.springframework.web.cors.CorsConfigurationSource
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration

@Configuration
open class LoggingCorsFilter(
    private val corsConfigurationSource: CorsConfigurationSource
) : CorsFilter(corsConfigurationSource) {

    private val logger = LoggerFactory.getLogger(LoggingCorsFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val origin = request.getHeader("Origin")
        if (origin != null) {
            val config = corsConfigurationSource.getCorsConfiguration(request)
            if (config != null && !config.allowedOrigins.contains(origin)) {
                val message = "CORS error: Origin '$origin' is not allowed for request '${request.requestURI}'. " +
                        "Allowed origins: ${config.allowedOrigins}. Request Method: '${request.method}', " +
                        "Remote Address: '${request.remoteAddr}'. Headers: ${getHeadersInfo(request)}"
                logger.error(message)
            }
        }
        // Continue with the filter chain
        super.doFilterInternal(request, response, filterChain)
    }

    private fun getHeadersInfo(request: HttpServletRequest): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val headerNames = request.headerNames
        while (headerNames.hasMoreElements()) {
            val headerName = headerNames.nextElement()
            headers[headerName] = request.getHeader(headerName)
        }
        return headers
    }
}