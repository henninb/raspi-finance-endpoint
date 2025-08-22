package finance.controllers

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import jakarta.validation.ValidationException

@ControllerAdvice(assignableTypes = [AccountController::class])
@Order(1)
class AccountControllerAdvice {

    companion object {
        private val logger: Logger = LogManager.getLogger()
    }

    @ExceptionHandler(value = [HttpMessageNotReadableException::class])
    fun handleMessageNotReadableException(throwable: HttpMessageNotReadableException): ResponseEntity<Map<String, String>> {
        logger.error("Account - Message not readable: ${throwable.message}", throwable)
        val response = mapOf("error" to "Invalid request data: ${throwable.mostSpecificCause?.message ?: throwable.message}")
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [ValidationException::class])
    fun handleValidationException(throwable: ValidationException): ResponseEntity<Map<String, String>> {
        logger.error("Account - Validation error: ${throwable.message}", throwable)
        val response = mapOf("error" to "Validation error: ${throwable.message}")
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }
}