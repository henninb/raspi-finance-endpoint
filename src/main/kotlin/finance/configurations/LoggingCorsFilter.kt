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

    private val log = LoggerFactory.getLogger(LoggingCorsFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val origin = request.getHeader("Origin")
        if (origin != null) {
            val config = corsConfigurationSource.getCorsConfiguration(request)
            if (config != null && config.allowedOrigins?.contains(origin) == false) {
                val message = "CORS error: Origin '$origin' is not allowed for request '${request.requestURI}'. " +
                        "Allowed origins: ${config.allowedOrigins}. Request Method: '${request.method}', " +
                        "Remote Address: '${request.remoteAddr}'"
                log.error(message)
            }
        }
        // Continue with the filter chain
        super.doFilterInternal(request, response, filterChain)
    }


}