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
class PaymentController(private val paymentService: PaymentService) : BaseController() {

    // curl -k https://localhost:8443/payment/select
    @GetMapping("/select", produces = ["application/json"])
    fun selectAllPayments(): ResponseEntity<List<Payment>> {
        return try {
            logger.debug("Retrieving all payments")
            val payments = paymentService.findAllPayments()
            logger.info("Retrieved ${payments.size} payments")
            ResponseEntity.ok(payments)
        } catch (ex: Exception) {
            logger.error("Failed to retrieve payments: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve payments: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT --data '{"transactionDate":"2025-08-15","amount": 123.45}' https://localhost:8443/payment/update/1001
    @PutMapping("/update/{paymentId}", consumes = ["application/json"], produces = ["application/json"])
    fun updatePayment(
        @PathVariable paymentId: Long,
        @RequestBody patch: Payment
    ): ResponseEntity<Payment> {
        return try {
            logger.info("Updating payment: $paymentId")
            val response = paymentService.updatePayment(paymentId, patch)
            logger.info("Payment updated successfully: $paymentId")
            ResponseEntity.ok(response)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to update payment due to data integrity violation for paymentId $paymentId: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate payment found.")
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error updating payment $paymentId: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}", ex)
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid input updating payment $paymentId: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error updating payment $paymentId: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request POST --data '{"accountNameOwner":"test_brian", "amount": 100.00, "activeStatus": true}' https://localhost:8443/payment/insert
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertPayment(@RequestBody payment: Payment): ResponseEntity<Payment> {
        return try {
            logger.info("Inserting payment for account: ${payment.accountNameOwner}")
            val paymentResponse = paymentService.insertPaymentNew(payment)
            logger.info("Payment inserted successfully: ${paymentResponse.paymentId}")
            ResponseEntity.ok(paymentResponse)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to insert payment due to data integrity violation for account ${payment.accountNameOwner}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate payment found.")
        } catch (ex: ResponseStatusException) {
            logger.error("Failed to insert payment for account ${payment.accountNameOwner}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert payment: ${ex.message}", ex)
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error inserting payment for account ${payment.accountNameOwner}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}", ex)
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid input inserting payment for account ${payment.accountNameOwner}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error inserting payment for account ${payment.accountNameOwner}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request DELETE https://localhost:8443/payment/delete/1001
    @DeleteMapping("/delete/{paymentId}", produces = ["application/json"])
    fun deleteByPaymentId(@PathVariable paymentId: Long): ResponseEntity<Payment> {
        return try {
            logger.info("Attempting to delete payment: $paymentId")
            val payment = paymentService.findByPaymentId(paymentId)
                .orElseThrow {
                    logger.warn("Payment not found for deletion: $paymentId")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found: $paymentId")
                }
            
            paymentService.deleteByPaymentId(paymentId)
            logger.info("Payment deleted successfully: $paymentId")
            ResponseEntity.ok(payment)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to delete payment $paymentId: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete payment: ${ex.message}", ex)
        }
    }
}
