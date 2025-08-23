package finance.controllers

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import jakarta.validation.ValidationException

@ControllerAdvice(assignableTypes = [LoginController::class])
@Order(1)
class LoginControllerAdvice {

    companion object {
        private val logger: Logger = LogManager.getLogger()
    }

    @ExceptionHandler(value = [HttpMessageNotReadableException::class])
    fun handleMessageNotReadableException(request: HttpServletRequest, throwable: HttpMessageNotReadableException): ResponseEntity<Map<String, String>> {
        logger.warn("LOGIN_400_NOT_READABLE method=${request.method} uri=${request.requestURI} msg=${throwable.mostSpecificCause?.message ?: throwable.message}")
        val response = mapOf("error" to "Invalid request data: ${throwable.mostSpecificCause?.message ?: throwable.message}")
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [ValidationException::class])
    fun handleValidationException(request: HttpServletRequest, throwable: ValidationException): ResponseEntity<Map<String, String>> {
        logger.warn("LOGIN_400_VALIDATION_EXCEPTION method=${request.method} uri=${request.requestURI} msg=${throwable.message}")
        val response = mapOf("error" to "Validation error: ${throwable.message}")
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }
}
