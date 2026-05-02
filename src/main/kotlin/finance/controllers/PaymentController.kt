package finance.controllers

import finance.domain.Payment
import finance.domain.toCreatedResponse
import finance.domain.toListOkResponse
import finance.domain.toOkResponse
import finance.domain.toPagedOkResponse
import finance.services.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

@Tag(name = "Payment Management", description = "Operations for managing payments")
@RestController
@RequestMapping("/api/payment")
@PreAuthorize("hasAuthority('USER')")
class PaymentController(
    private val paymentService: PaymentService,
) : StandardizedBaseController(),
    StandardRestController<Payment, Long> {
    @Operation(summary = "Get all active payments")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active payments retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Payment>> = paymentService.findAllActive().toListOkResponse()

    @Operation(summary = "Get all active payments (paginated)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of payments returned"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active/paged", produces = ["application/json"])
    override fun findAllActivePaged(pageable: Pageable): ResponseEntity<Page<Payment>> = paymentService.findAllActive(pageable).toPagedOkResponse(pageable)

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
    ): ResponseEntity<Payment> = paymentService.findById(id).toOkResponse()

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
    ): ResponseEntity<Payment> = paymentService.save(entity).toCreatedResponse()

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
        entity.paymentId = id
        return paymentService.update(entity).toOkResponse()
    }

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
    ): ResponseEntity<Payment> = paymentService.deleteById(id).toOkResponse()
}
