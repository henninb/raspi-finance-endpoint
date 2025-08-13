package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.catalina.connector.ClientAbortException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import jakarta.validation.ValidationException

open class BaseController {

    @ExceptionHandler(
        value = [ConstraintViolationException::class, NumberFormatException::class, EmptyResultDataAccessException::class,
            MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class, HttpMediaTypeNotSupportedException::class,
            IllegalArgumentException::class, DataIntegrityViolationException::class]
    )
    fun handleBadHttpRequests(throwable: Throwable): ResponseEntity<Map<String, String>> {
        logSecureError("BAD_REQUEST", throwable, HttpStatus.BAD_REQUEST)
        val response = mapOf("response" to "BAD_REQUEST: ${throwable.javaClass.simpleName}")
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }
    
    @ExceptionHandler(value = [ValidationException::class])
    fun handleValidationException(throwable: ValidationException): ResponseEntity<Map<String, String>> {
        logSecureError("VALIDATION_EXCEPTION", throwable, HttpStatus.BAD_REQUEST)
        val response = mapOf("response" to "BAD_REQUEST: ${throwable.javaClass.simpleName}")
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [ResponseStatusException::class])
    fun handleResponseStatusException(throwable: ResponseStatusException): ResponseEntity<Map<String, String>> {
        val httpStatus = HttpStatus.valueOf(throwable.statusCode.value())
        logSecureError("RESPONSE_STATUS_EXCEPTION", throwable, httpStatus)
        val response = mapOf("response" to "${throwable.statusCode}: ${throwable.javaClass.simpleName}")
        return ResponseEntity(response, throwable.statusCode)
    }

    @ExceptionHandler(value = [AuthenticationException::class])
    fun handleUnauthorized(throwable: Throwable): ResponseEntity<Map<String, String>> {
        logSecureError("UNAUTHORIZED", throwable, HttpStatus.UNAUTHORIZED)
        val response = mapOf("response" to "UNAUTHORIZED: ${throwable.javaClass.simpleName}")
        return ResponseEntity(response, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(value = [ClientAbortException::class])
    fun handleServiceUnavailable(throwable: Throwable): ResponseEntity<Map<String, String>> {
        logSecureError("SERVICE_UNAVAILABLE", throwable, HttpStatus.SERVICE_UNAVAILABLE)
        val response = mapOf("response" to "SERVICE_UNAVAILABLE: ${throwable.javaClass.simpleName}")
        return ResponseEntity(response, HttpStatus.SERVICE_UNAVAILABLE)
    }

    @ExceptionHandler(value = [Exception::class])
    fun handleHttpInternalError(throwable: Throwable): ResponseEntity<Map<String, String>> {
        logSecureError("INTERNAL_SERVER_ERROR", throwable, HttpStatus.INTERNAL_SERVER_ERROR)
        val response = mapOf("response" to "INTERNAL_SERVER_ERROR: ${throwable.javaClass.simpleName}")
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private fun logSecureError(errorType: String, throwable: Throwable, statusCode: HttpStatus) {
        val request = getCurrentHttpRequest()
        val clientIp = getClientIpAddress(request)
        val method = request?.method ?: "unknown"
        val uri = request?.requestURI ?: "unknown"
        val userAgent = sanitizeHeader(request?.getHeader("User-Agent"))
        
        val logMessage = buildString {
            append("CONTROLLER_ERROR type=$errorType status=${statusCode.value()} ")
            append("method=$method uri=$uri ip=$clientIp ")
            append("exception=${throwable.javaClass.simpleName}")
            if (!userAgent.isNullOrBlank()) append(" userAgent='$userAgent'")
        }
        
        when {
            statusCode.is5xxServerError -> securityLogger.error(logMessage, throwable)
            statusCode == HttpStatus.UNAUTHORIZED || statusCode == HttpStatus.FORBIDDEN -> 
                securityLogger.warn(logMessage)
            else -> logger.info(logMessage)
        }
    }
    
    private fun getCurrentHttpRequest(): HttpServletRequest? {
        return try {
            val requestAttributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
            requestAttributes.request
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getClientIpAddress(request: HttpServletRequest?): String {
        if (request == null) return "unknown"
        
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        val xRealIp = request.getHeader("X-Real-IP")
        
        return when {
            !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
            !xRealIp.isNullOrBlank() -> xRealIp
            else -> request.remoteAddr ?: "unknown"
        }
    }
    
    private fun sanitizeHeader(header: String?): String? {
        return header?.take(200)?.replace(Regex("[\\r\\n\\t]"), " ")
    }

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
        private val securityLogger = LoggerFactory.getLogger("SECURITY.${BaseController::class.java.simpleName}")
    }
}