package finance.controllers

import finance.domain.Payment
import finance.services.PaymentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/api/payment")
class PaymentController(private val paymentService: PaymentService) :
    StandardizedBaseController(), StandardRestController<Payment, Long> {

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/payment/select
     * Maintains original behavior
     */
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

    /**
     * Legacy endpoint - PUT /api/payment/update/{paymentId}
     * Maintains original behavior
     */
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
            throw ResponseStatusException(HttpStatus.CONFLICT, ex.message ?: "Duplicate payment found.")
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

    /**
     * Legacy endpoint - POST /api/payment/insert
     */
    // curl -k --header "Content-Type: application/json" --request POST --data '{"sourceAccount":"checking_brian", "destinationAccount":"visa_brian", "amount": 100.00, "activeStatus": true}' https://localhost:8443/payment/insert
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertPayment(@RequestBody payment: Payment): ResponseEntity<Payment> {
        return try {
            logger.info("Inserting payment: ${payment.sourceAccount} -> ${payment.destinationAccount}")
            val paymentResponse = paymentService.insertPaymentNew(payment)
            logger.info("Payment inserted successfully: ${paymentResponse.paymentId}")
            ResponseEntity.status(HttpStatus.CREATED).body(paymentResponse)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to insert payment due to data integrity violation for ${payment.sourceAccount} -> ${payment.destinationAccount}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, ex.message ?: "Duplicate payment found.")
        } catch (ex: ResponseStatusException) {
            logger.error("Failed to insert payment ${payment.sourceAccount} -> ${payment.destinationAccount}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert payment: ${ex.message}", ex)
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error inserting payment ${payment.sourceAccount} -> ${payment.destinationAccount}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}", ex)
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid input inserting payment ${payment.sourceAccount} -> ${payment.destinationAccount}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error inserting payment ${payment.sourceAccount} -> ${payment.destinationAccount}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    /**
     * Legacy endpoint - DELETE /api/payment/delete/{paymentId}
     */
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

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/payment/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Payment>> {
        return handleCrudOperation("Find all active payments", null) {
            logger.debug("Retrieving all active payments")
            val payments: List<Payment> = paymentService.findAllPayments()
            logger.info("Retrieved ${payments.size} active payments")
            payments
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/payment/{paymentId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{paymentId}", produces = ["application/json"])
    override fun findById(@PathVariable paymentId: Long): ResponseEntity<Payment> {
        return handleCrudOperation("Find payment by ID", paymentId) {
            logger.debug("Retrieving payment: $paymentId")
            val payment = paymentService.findByPaymentId(paymentId)
                .orElseThrow {
                    logger.warn("Payment not found: $paymentId")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found: $paymentId")
                }
            logger.info("Retrieved payment: $paymentId")
            payment
        }
    }

    /**
     * Standardized entity creation - POST /api/payment
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(@Valid @RequestBody payment: Payment): ResponseEntity<Payment> {
        return handleCreateOperation("Payment", "${payment.sourceAccount} -> ${payment.destinationAccount}") {
            logger.info("Creating payment: ${payment.sourceAccount} -> ${payment.destinationAccount}")
            val result = paymentService.insertPaymentNew(payment)
            logger.info("Payment created successfully: ${result.paymentId}")
            result
        }
    }

    /**
     * Standardized entity update - PUT /api/payment/{paymentId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{paymentId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(@PathVariable paymentId: Long, @Valid @RequestBody payment: Payment): ResponseEntity<Payment> {
        return handleCrudOperation("Update payment", paymentId) {
            logger.info("Updating payment: $paymentId")
            // Validate payment exists first
            paymentService.findByPaymentId(paymentId)
                .orElseThrow {
                    logger.warn("Payment not found for update: $paymentId")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found: $paymentId")
                }
            val result = paymentService.updatePayment(paymentId, payment)
            logger.info("Payment updated successfully: $paymentId")
            result
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/payment/{paymentId}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{paymentId}", produces = ["application/json"])
    override fun deleteById(@PathVariable paymentId: Long): ResponseEntity<Payment> {
        return handleDeleteOperation(
            "Payment",
            paymentId,
            { paymentService.findByPaymentId(paymentId) },
            { paymentService.deleteByPaymentId(paymentId) }
        )
    }
}
