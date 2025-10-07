package finance.controllers

import finance.domain.Payment
import finance.domain.ServiceResult
import finance.services.StandardizedPaymentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@CrossOrigin
@RestController
@RequestMapping("/api/payment")
class PaymentController(
    private val standardizedPaymentService: StandardizedPaymentService,
) : StandardizedBaseController(),
    StandardRestController<Payment, Long> {
    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    // curl -k https://localhost:8443/payment/select

    /**
     * Legacy endpoint - GET /api/payment/select
     * Maintains original behavior
     */
    @GetMapping("/select", produces = ["application/json"])
    fun selectAllPayments(): ResponseEntity<List<Payment>> =
        try {
            logger.debug("Retrieving all payments")
            val payments = standardizedPaymentService.findAllPayments()
            logger.info("Retrieved ${payments.size} payments")
            ResponseEntity.ok(payments)
        } catch (ex: Exception) {
            logger.error("Failed to retrieve payments: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve payments: ${ex.message}", ex)
        }

    // curl -k --header "Content-Type: application/json" --request PUT --data '{"transactionDate":"2025-08-15","amount": 123.45}' https://localhost:8443/payment/update/1001

    /**
     * Legacy endpoint - PUT /api/payment/update/{paymentId}
     * Maintains original behavior
     */
    @PutMapping("/update/{paymentId}", consumes = ["application/json"], produces = ["application/json"])
    fun updatePayment(
        @PathVariable paymentId: Long,
        @RequestBody patch: Payment,
    ): ResponseEntity<Payment> =
        try {
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

    // curl -k --header "Content-Type: application/json" --request POST --data '{"sourceAccount":"checking_brian", "destinationAccount":"visa_brian", "amount": 100.00, "activeStatus": true}' https://localhost:8443/payment/insert

    /**
     * Legacy endpoint - POST /api/payment/insert
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertPayment(
        @RequestBody payment: Payment,
    ): ResponseEntity<Payment> =
        try {
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

    // curl -k --header "Content-Type: application/json" --request DELETE https://localhost:8443/payment/delete/1001

    /**
     * Legacy endpoint - DELETE /api/payment/delete/{paymentId}
     */
    @DeleteMapping("/delete/{paymentId}", produces = ["application/json"])
    fun deleteByPaymentId(
        @PathVariable paymentId: Long,
    ): ResponseEntity<Payment> =
        try {
            logger.info("Attempting to delete payment: $paymentId")
            val payment =
                standardizedPaymentService
                    .findByPaymentId(paymentId)
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

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/payment/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Payment>> =
        when (val result = standardizedPaymentService.findAllActive()) {
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

    /**
     * Standardized single entity retrieval - GET /api/payment/{paymentId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{paymentId}", produces = ["application/json"])
    override fun findById(
        @PathVariable("paymentId") id: Long,
    ): ResponseEntity<Payment> =
        when (val result = standardizedPaymentService.findById(id)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved payment: $id (standardized)")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Payment not found: $id (standardized)")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving payment $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized entity creation - POST /api/payment
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: Payment,
    ): ResponseEntity<Payment> =
        when (val result = standardizedPaymentService.save(entity)) {
            is ServiceResult.Success -> {
                logger.info("Payment created successfully: ${entity.sourceAccount} -> ${entity.destinationAccount} (standardized)")
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

    /**
     * Standardized entity update - PUT /api/payment/{paymentId}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{paymentId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("paymentId") id: Long,
        @Valid @RequestBody entity: Payment,
    ): ResponseEntity<Payment> {
        @Suppress("REDUNDANT_ELSE_IN_WHEN") // Defensive programming: handle unexpected ServiceResult types
        return when (val result = standardizedPaymentService.update(entity)) {
            is ServiceResult.Success -> {
                logger.info("Payment updated successfully: $id (standardized)")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Payment not found for update: $id (standardized)")
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
    override fun deleteById(
        @PathVariable("paymentId") id: Long,
    ): ResponseEntity<Payment> {
        // First get the payment to return it
        val paymentResult = standardizedPaymentService.findById(id)
        if (paymentResult !is ServiceResult.Success) {
            logger.warn("Payment not found for deletion: $id")
            return ResponseEntity.notFound().build()
        }

        return when (val result = standardizedPaymentService.deleteById(id)) {
            is ServiceResult.Success -> {
                logger.info("Payment deleted successfully: $id")
                ResponseEntity.ok(paymentResult.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Payment not found for deletion: $id")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error deleting payment $id: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
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
