package finance.controllers

import finance.domain.Payment
import finance.services.StandardizedPaymentService
import finance.domain.ServiceResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/api/payment")
class PaymentController(private val standardizedPaymentService: StandardizedPaymentService) :
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
            val payments = standardizedPaymentService.findAllPayments()
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
            val response = standardizedPaymentService.updatePayment(paymentId, patch)
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
            val paymentResponse = standardizedPaymentService.insertPayment(payment)
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
            val payment = standardizedPaymentService.findByPaymentId(paymentId)
                .orElseThrow {
                    logger.warn("Payment not found for deletion: $paymentId")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found: $paymentId")
                }

            standardizedPaymentService.deleteByPaymentId(paymentId)
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
        return when (val result = standardizedPaymentService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} active payments (standardized)")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.info("No active payments found (standardized)")
                ResponseEntity.ok(emptyList())
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving active payments: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/payment/{paymentId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{paymentId}", produces = ["application/json"])
    override fun findById(@PathVariable paymentId: Long): ResponseEntity<Payment> {
        return when (val result = standardizedPaymentService.findById(paymentId)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved payment: $paymentId (standardized)")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Payment not found: $paymentId (standardized)")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving payment $paymentId: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity creation - POST /api/payment
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(@Valid @RequestBody payment: Payment): ResponseEntity<Payment> {
        return when (val result = standardizedPaymentService.save(payment)) {
            is ServiceResult.Success -> {
                logger.info("Payment created successfully: ${payment.sourceAccount} -> ${payment.destinationAccount} (standardized)")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating payment: ${result.errors}")
                ResponseEntity.badRequest().build<Payment>()
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating payment: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<Payment>()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error creating payment: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Payment>()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Payment>()
            }
        }
    }

    /**
     * Standardized entity update - PUT /api/payment/{paymentId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{paymentId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(@PathVariable paymentId: Long, @Valid @RequestBody payment: Payment): ResponseEntity<Payment> {
        return when (val result = standardizedPaymentService.update(payment)) {
            is ServiceResult.Success -> {
                logger.info("Payment updated successfully: $paymentId (standardized)")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Payment not found for update: $paymentId (standardized)")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating payment: ${result.errors}")
                ResponseEntity.badRequest().build<Payment>()
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating payment: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<Payment>()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error updating payment: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Payment>()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Payment>()
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/payment/{paymentId}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{paymentId}", produces = ["application/json"])
    override fun deleteById(@PathVariable paymentId: Long): ResponseEntity<Payment> {
        // First get the payment to return it
        val paymentResult = standardizedPaymentService.findById(paymentId)
        if (paymentResult !is ServiceResult.Success) {
            logger.warn("Payment not found for deletion: $paymentId")
            return ResponseEntity.notFound().build()
        }

        return when (val result = standardizedPaymentService.deleteById(paymentId)) {
            is ServiceResult.Success -> {
                logger.info("Payment deleted successfully: $paymentId")
                ResponseEntity.ok(paymentResult.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Payment not found for deletion: $paymentId")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error deleting payment: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Payment>()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Payment>()
            }
        }
    }
}
