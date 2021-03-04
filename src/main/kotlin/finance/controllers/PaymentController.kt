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
@RequestMapping("/payment")
class PaymentController(private var paymentService: PaymentService) : BaseController() {

    @GetMapping("/select", produces = ["application/json"])
    fun selectAllPayments(): ResponseEntity<List<Payment>> {
        val payments = paymentService.findAllPayments()

        return ResponseEntity.ok(payments)
    }

    @PostMapping("/insert", produces = ["application/json"])
    fun insertPayment(@RequestBody payment: Payment): ResponseEntity<String> {
        paymentService.insertPayment(payment)
        return ResponseEntity.ok("payment inserted")
    }

    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8080/payment/delete/1001
    @DeleteMapping("/delete/{paymentId}", produces = ["application/json"])
    fun deleteByPaymentId(@PathVariable paymentId: Long): ResponseEntity<String> {
        val paymentOptional: Optional<Payment> = paymentService.findByPaymentId(paymentId)

        logger.info("deleteByPaymentId controller - $paymentId")
        if (paymentOptional.isPresent) {
            paymentService.deleteByPaymentId(paymentId)
            return ResponseEntity.ok("payment deleted")
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not deleted: $paymentId")
    }
}