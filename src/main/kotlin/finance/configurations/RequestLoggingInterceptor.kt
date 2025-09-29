package finance.configurations

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

@Component
class RequestLoggingInterceptor : HandlerInterceptor {
    companion object {
        private val logger: Logger = LogManager.getLogger()
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (request.requestURI.contains("graphql")) {
            logger.info("Incoming graphql request to URI: {}", request.requestURI)
        }
        return true
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?,
    ) {
        if (request.requestURI.contains("graphql")) {
            logger.info("Sending response with status: {}", response.status)
        }
    }
}
