package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.catalina.connector.ClientAbortException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
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
import javax.validation.ConstraintViolationException
import javax.validation.ValidationException

open class BaseController {

    @ExceptionHandler(
        value = [ConstraintViolationException::class, NumberFormatException::class, EmptyResultDataAccessException::class,
            MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class, HttpMediaTypeNotSupportedException::class,
            IllegalArgumentException::class, DataIntegrityViolationException::class, ValidationException::class]
    )
    fun handleBadHttpRequests(throwable: Throwable): ResponseEntity<Map<String, String>> {
        logger.info("BAD_REQUEST: ", throwable)
        val response = mapOf("response" to "BAD_REQUEST: ${throwable.javaClass.simpleName}, message: ${throwable.message}")
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(value = [ResponseStatusException::class])
    fun handleResponseStatusException(throwable: ResponseStatusException): ResponseEntity<Map<String, String>> {
        logger.error("${throwable.statusCode}: ", throwable)
        val response = mapOf("response" to "${throwable.statusCode}: ${throwable.javaClass.simpleName}, message: ${throwable.reason}")
        return ResponseEntity(response, throwable.statusCode)
    }

    @ExceptionHandler(value = [AuthenticationException::class])
    fun handleUnauthorized(throwable: Throwable): ResponseEntity<Map<String, String>> {
        logger.error("UNAUTHORIZED: ", throwable)
        val response = mapOf("response" to "UNAUTHORIZED: ${throwable.javaClass.simpleName}, message: ${throwable.message}")
        return ResponseEntity(response, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(value = [ClientAbortException::class])
    fun handleServiceUnavailable(throwable: Throwable): ResponseEntity<Map<String, String>> {
        logger.error("SERVICE_UNAVAILABLE: ", throwable)
        val response = mapOf("response" to "SERVICE_UNAVAILABLE: ${throwable.javaClass.simpleName}, message: ${throwable.message}")
        return ResponseEntity(response, HttpStatus.SERVICE_UNAVAILABLE)
    }

    @ExceptionHandler(value = [Exception::class])
    fun handleHttpInternalError(throwable: Throwable): ResponseEntity<Map<String, String>> {
        logger.error("INTERNAL_SERVER_ERROR: ", throwable)
        val response = mapOf("response" to "INTERNAL_SERVER_ERROR: ${throwable.javaClass.simpleName}, message: ${throwable.message}")
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
    }
}