package finance.controllers

import finance.domain.Payment
import finance.services.PaymentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/payment", "/api/payment")
class PaymentController(private var paymentService: PaymentService) : BaseController() {

    @GetMapping("/select", produces = ["application/json"])
    fun selectAllPayments(): ResponseEntity<List<Payment>> {
        val payments = paymentService.findAllPayments()

        return ResponseEntity.ok(payments)
    }

    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertPayment(@RequestBody payment: Payment): ResponseEntity<Payment> {
        return try {
            val paymentResponse = paymentService.insertPayment(payment)
            ResponseEntity.ok(paymentResponse)
        } catch (ex: ResponseStatusException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert payment: ${ex.message}", ex)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8443/payment/delete/1001
    @DeleteMapping("/delete/{paymentId}", produces = ["application/json"])
    fun deleteByPaymentId(@PathVariable paymentId: Long): ResponseEntity<Payment> {
        val paymentOptional: Optional<Payment> = paymentService.findByPaymentId(paymentId)

        if (paymentOptional.isPresent) {
            paymentService.deleteByPaymentId(paymentId)
            val payment = paymentOptional.get()
            logger.info("payment deleted: ${payment.paymentId}")
            return ResponseEntity.ok(payment)
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not deleted: $paymentId")
    }
}