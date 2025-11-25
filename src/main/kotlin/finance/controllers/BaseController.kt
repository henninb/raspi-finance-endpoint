package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import jakarta.validation.ValidationException
import org.apache.catalina.connector.ClientAbortException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException

open class BaseController {
    @ExceptionHandler(
        value = [
            ConstraintViolationException::class, NumberFormatException::class, EmptyResultDataAccessException::class,
            MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class, HttpMediaTypeNotSupportedException::class,
            IllegalArgumentException::class, MethodArgumentNotValidException::class,
        ],
    )
    fun handleBadHttpRequests(throwable: Throwable): ResponseEntity<Map<String, Any>> {
        val status = HttpStatus.BAD_REQUEST
        logSecureError("BAD_REQUEST", throwable, status)
        val body = buildErrorBody(status, "BAD_REQUEST", throwable.javaClass.simpleName)
        return ResponseEntity(body, status)
    }

    @ExceptionHandler(value = [ValidationException::class])
    fun handleValidationException(throwable: ValidationException): ResponseEntity<Map<String, Any>> {
        val status = HttpStatus.BAD_REQUEST
        logSecureError("VALIDATION_EXCEPTION", throwable, status)
        val body = buildErrorBody(status, "BAD_REQUEST", throwable.javaClass.simpleName)
        return ResponseEntity(body, status)
    }

    @ExceptionHandler(value = [ResponseStatusException::class])
    fun handleResponseStatusException(throwable: ResponseStatusException): ResponseEntity<Map<String, Any>> {
        val httpStatus = HttpStatus.valueOf(throwable.statusCode.value())
        logSecureError("RESPONSE_STATUS_EXCEPTION", throwable, httpStatus)
        val errorMessage = throwable.reason ?: "${throwable.javaClass.simpleName}"
        val body = buildErrorBody(httpStatus, httpStatus.name, errorMessage)
        return ResponseEntity(body, httpStatus)
    }

    @ExceptionHandler(value = [AuthenticationException::class])
    fun handleUnauthorized(throwable: Throwable): ResponseEntity<Map<String, Any>> {
        val status = HttpStatus.UNAUTHORIZED
        logSecureError("UNAUTHORIZED", throwable, status)
        val body = buildErrorBody(status, "UNAUTHORIZED", throwable.javaClass.simpleName)
        return ResponseEntity(body, status)
    }

    @ExceptionHandler(value = [ClientAbortException::class])
    fun handleServiceUnavailable(throwable: Throwable): ResponseEntity<Map<String, Any>> {
        val status = HttpStatus.SERVICE_UNAVAILABLE
        logSecureError("SERVICE_UNAVAILABLE", throwable, status)
        val body = buildErrorBody(status, "SERVICE_UNAVAILABLE", throwable.javaClass.simpleName)
        return ResponseEntity(body, status)
    }

    @ExceptionHandler(value = [Exception::class])
    fun handleHttpInternalError(throwable: Throwable): ResponseEntity<Map<String, Any>> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        logSecureError("INTERNAL_SERVER_ERROR", throwable, status)
        val body = buildErrorBody(status, "INTERNAL_SERVER_ERROR", throwable.javaClass.simpleName)
        return ResponseEntity(body, status)
    }

    private fun buildErrorBody(
        status: HttpStatus,
        code: String,
        message: String?,
    ): Map<String, Any> {
        val request = getCurrentHttpRequest()
        val method = request?.method ?: "unknown"
        val uri = request?.requestURI ?: "unknown"
        val safeMessage = sanitizeHeader(message) ?: status.reasonPhrase
        return mapOf(
            "timestamp" to System.currentTimeMillis(),
            "status" to status.value(),
            "error" to status.reasonPhrase,
            "code" to code,
            "message" to safeMessage,
            "path" to uri,
            "method" to method,
        )
    }

    private fun logSecureError(
        errorType: String,
        throwable: Throwable,
        statusCode: HttpStatus,
    ) {
        val request = getCurrentHttpRequest()
        val clientIp = getClientIpAddress(request)
        val method = request?.method ?: "unknown"
        val uri = request?.requestURI ?: "unknown"
        val userAgent = sanitizeHeader(request?.getHeader("User-Agent"))
        val exMsg = sanitizeHeader(throwable.message)?.take(180)

        val logMessage =
            buildString {
                append("CONTROLLER_ERROR type=$errorType status=${statusCode.value()} ")
                append("method=$method uri=$uri ip=$clientIp ")
                append("exception=${throwable.javaClass.simpleName}")
                if (!exMsg.isNullOrBlank()) append(" msg='$exMsg'")
                if (!userAgent.isNullOrBlank()) append(" userAgent='$userAgent'")
            }

        when {
            statusCode.is5xxServerError -> {
                securityLogger.error(logMessage, throwable)
            }

            statusCode == HttpStatus.UNAUTHORIZED || statusCode == HttpStatus.FORBIDDEN -> {
                securityLogger.warn(logMessage)
            }

            else -> {
                logger.info(logMessage)
            }
        }
    }

    private fun getCurrentHttpRequest(): HttpServletRequest? =
        try {
            val requestAttributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
            requestAttributes.request
        } catch (e: Exception) {
            null
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

    private fun sanitizeHeader(header: String?): String? = header?.take(200)?.replace(Regex("[\\r\\n\\t]"), " ")

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
        private val securityLogger = LoggerFactory.getLogger("SECURITY.${BaseController::class.java.simpleName}")
    }
}
