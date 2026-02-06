package finance.controllers

import finance.domain.Payment
import finance.domain.ServiceResult
import finance.services.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Payment Management", description = "Operations for managing payments")
@RestController
@RequestMapping("/api/payment")
@PreAuthorize("hasAuthority('USER')")
class PaymentController(
    private val paymentService: PaymentService,
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
        when (val result = paymentService.findAllActive()) {
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
     * Paginated collection retrieval - GET /api/payment/active/paged?page=0&size=50
     * Returns Page<Payment> with metadata
     */
    @Operation(summary = "Get all active payments (paginated)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of payments returned"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active/paged", produces = ["application/json"])
    override fun findAllActivePaged(
        pageable: Pageable,
    ): ResponseEntity<Page<Payment>> {
        logger.debug("Retrieving all active payments (paginated) - page: ${pageable.pageNumber}, size: ${pageable.pageSize}")
        return when (val result = paymentService.findAllActive(pageable)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved page ${pageable.pageNumber} with ${result.data.numberOfElements} payments")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("No payments found")
                ResponseEntity.ok(Page.empty(pageable))
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving payments: ${result.exception.message}", result.exception)
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
        when (val result = paymentService.findById(id)) {
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
        when (val result = paymentService.save(entity)) {
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
        logger.debug("Updating payment with ID: $id")

        // Ensure the entity has the correct ID from the path
        entity.paymentId = id

        @Suppress("REDUNDANT_ELSE_IN_WHEN") // Defensive programming: handle unexpected ServiceResult types
        return when (val result = paymentService.update(entity)) {
            is ServiceResult.Success -> {
                logger.info("Payment updated successfully: $id (standardized)")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Payment not found for update: $id (standardized)")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating payment $id: ${result.errors}")
                ResponseEntity.badRequest().build<Payment>()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating payment $id: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<Payment>()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error updating payment $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Payment>()
            }

            else -> {
                logger.error("Unexpected result type for payment update $id: $result")
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
        val paymentResult = paymentService.findById(id)
        if (paymentResult !is ServiceResult.Success) {
            logger.warn("Payment not found for deletion: $id")
            return ResponseEntity.notFound().build()
        }

        return when (val result = paymentService.deleteById(id)) {
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
