package finance.controllers

import finance.domain.Payment
import finance.domain.ServiceResult
import finance.services.StandardizedPaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Payment Management", description = "Operations for managing payments")
@RestController
@RequestMapping("/api/payment")
class PaymentController(
    private val standardizedPaymentService: StandardizedPaymentService,
) : StandardizedBaseController(),
    StandardRestController<Payment, Long> {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/payment/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @Operation(summary = "Get all active payments")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active payments retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
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
    @Operation(summary = "Get payment by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Payment retrieved"),
            ApiResponse(responseCode = "404", description = "Payment not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
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
    @Operation(summary = "Create payment")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Payment created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
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
    @Operation(summary = "Update payment by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Payment updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Payment not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
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
    @Operation(summary = "Delete payment by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Payment deleted"),
            ApiResponse(responseCode = "404", description = "Payment not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
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
