package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Payment
import finance.services.PaymentService
import org.apache.logging.log4j.LogManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import java.util.*
import javax.validation.ConstraintViolationException
import javax.validation.ValidationException

@CrossOrigin
@RestController
@RequestMapping("/payment")
class PaymentController(private var paymentService: PaymentService) {

    @GetMapping(path = ["/select"], produces = ["application/json"])
    fun selectAllPayments(): ResponseEntity<List<Payment>> {
        val payments = paymentService.findAllPayments()

        return ResponseEntity.ok(payments)
    }

    @PostMapping(path = ["/insert"], produces = ["application/json"])
    fun insertPayment(@RequestBody payment: Payment): ResponseEntity<String> {
        paymentService.insertPayment(payment)
        return ResponseEntity.ok("payment inserted")
    }

    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8080/payment/delete/1001
    @DeleteMapping(path = ["/delete/{paymentId}"], produces = ["application/json"])
    fun deleteByPaymentId(@PathVariable paymentId: Long): ResponseEntity<String> {
        val paymentOptional: Optional<Payment> = paymentService.findByPaymentId(paymentId)

        logger.info("deleteByPaymentId controller - $paymentId")
        if (paymentOptional.isPresent) {
            paymentService.deleteByPaymentId(paymentId)
            return ResponseEntity.ok("payment deleted")
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not deleted: $paymentId")
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST) //400
    @ExceptionHandler(
        value = [ConstraintViolationException::class, NumberFormatException::class, EmptyResultDataAccessException::class,
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
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}