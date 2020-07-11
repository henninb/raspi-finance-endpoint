package finance.controllers

import finance.domain.Payment
import finance.exceptions.EmptyAccountException
import finance.services.PaymentService
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.*
import javax.validation.ConstraintViolationException

@CrossOrigin
@RestController
@RequestMapping("/payment")
class PaymentController(private var paymentService: PaymentService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping(path = ["/select"])
    fun selectAllPayments(): ResponseEntity<List<Payment>> {
        val payments = paymentService.findAllPayments()

        return ResponseEntity.ok(payments)
    }

    @PostMapping(path = ["/insert"])
    fun insertPayment(@RequestBody payment: Payment): ResponseEntity<String> {
        paymentService.insertPayment(payment)
        return ResponseEntity.ok("payment inserted")
    }

    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8080/payment/delete/1001
    @DeleteMapping(path = ["/delete/{paymentId}"])
    fun deleteByPaymentId(@PathVariable paymentId: Long): ResponseEntity<String> {
        val paymentOptional: Optional<Payment> = paymentService.findByPaymentId(paymentId)

        logger.info("deleteByPaymentId controller - $paymentId")
        if (paymentOptional.isPresent) {
            paymentService.deleteByPaymentId(paymentId)
            return ResponseEntity.ok("payment deleted")
        }
        throw EmptyAccountException("payment not deleted.")
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST) //400
    @ExceptionHandler(value = [ConstraintViolationException::class, NumberFormatException::class, MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class, DataIntegrityViolationException::class])
    fun handleBadHttpRequests(throwable: Throwable): Map<String, String>? {
        val response: MutableMap<String, String> = HashMap()
        logger.error("Bad Request", throwable)
        response["response"] = "BAD_REQUEST: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        return response
    }
}