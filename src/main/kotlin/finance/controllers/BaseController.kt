package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.catalina.connector.ClientAbortException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.tomcat.websocket.AuthenticationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import javax.validation.ConstraintViolationException
import javax.validation.ValidationException

open class BaseController {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(
        value = [ConstraintViolationException::class, NumberFormatException::class, EmptyResultDataAccessException::class, RuntimeException::class,
            MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class, HttpMediaTypeNotSupportedException::class,
            IllegalArgumentException::class, DataIntegrityViolationException::class, ValidationException::class]
    )
    fun handleBadHttpRequests(throwable: Throwable): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        logger.info("Bad Request: ", throwable)
        response["response"] = "BAD_REQUEST: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        logger.info(response.toString())
        return response
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = [ResponseStatusException::class])
    fun handleHttpNotFound(throwable: Throwable): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        logger.error("not found: ", throwable)
        response["response"] = "NOT_FOUND: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        return response
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(value = [AuthenticationException::class])
    fun handleUnauthorized(throwable: Throwable): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        logger.error("not authorized: ", throwable)
        return response
    }

    @ResponseStatus(HttpStatus.NOT_MODIFIED)
    //@ExceptionHandler(value = [EmptyTransactionException::class])
    fun handleHttpNotModified(throwable: Throwable): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        logger.error("not modified: ", throwable)
        response["response"] = "NOT_MODIFIED: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        return response
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(value = [ClientAbortException::class])
    fun handleServiceUnavailable(throwable: Throwable) {
        logger.error("client connection aborted")
        logger.error(throwable.message)
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = [Exception::class])
    fun handleHttpInternalError(throwable: Throwable): Map<String, String> {
        val response: MutableMap<String, String> = HashMap()
        logger.error("internal server error: ", throwable)
        response["response"] =
            "INTERNAL_SERVER_ERROR: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        logger.info("response: $response")
        return response
    }

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
    }
}