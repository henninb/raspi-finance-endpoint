package finance.controllers

import finance.domain.ReceiptImage
import finance.domain.ServiceResult
import finance.services.ReceiptImageService
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

@CrossOrigin
@Tag(name = "Receipt Image Management", description = "Operations for managing receipt images")
@RestController
@RequestMapping("/api/receipt/image")
class ReceiptImageController(
    private val receiptImageService: ReceiptImageService,
) : StandardizedBaseController(),
    StandardRestController<ReceiptImage, Long> {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/receipt/image/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @Operation(summary = "Get all active receipt images")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active receipt images retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<ReceiptImage>> =
        when (val result = receiptImageService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} active receipt images (standardized)")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.info("No active receipt images found (standardized)")
                ResponseEntity.ok(emptyList())
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving active receipt images: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized single entity retrieval - GET /api/receipt/image/{receiptImageId}
     * Uses camelCase parameter with @PathVariable annotation
     */
    @Operation(summary = "Get receipt image by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Receipt image retrieved"),
            ApiResponse(responseCode = "404", description = "Receipt image not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/{receiptImageId}", produces = ["application/json"])
    override fun findById(
        @PathVariable("receiptImageId") id: Long,
    ): ResponseEntity<ReceiptImage> =
        when (val result = receiptImageService.findById(id)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved receipt image: $id (standardized)")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Receipt image not found: $id (standardized)")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error retrieving receipt image $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized entity creation - POST /api/receipt/image
     * Returns 201 CREATED
     */
    @Operation(summary = "Create receipt image")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Receipt image created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: ReceiptImage,
    ): ResponseEntity<ReceiptImage> =
        when (val result = receiptImageService.save(entity)) {
            is ServiceResult.Success -> {
                logger.info("Receipt image created successfully: ${result.data.receiptImageId} (standardized)")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating receipt image: ${result.errors}")
                ResponseEntity.badRequest().build()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating receipt image: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error creating receipt image: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized entity update - PUT /api/receipt/image/{receiptImageId}
     * Uses camelCase parameter with @PathVariable annotation
     */
    @Operation(summary = "Update receipt image by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Receipt image updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Receipt image not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PutMapping("/{receiptImageId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("receiptImageId") id: Long,
        @Valid @RequestBody entity: ReceiptImage,
    ): ResponseEntity<ReceiptImage> {
        @Suppress("REDUNDANT_ELSE_IN_WHEN") // Defensive programming: handle unexpected ServiceResult types
        return when (val result = receiptImageService.update(entity)) {
            is ServiceResult.Success -> {
                logger.info("Receipt image updated successfully: $id (standardized)")
                ResponseEntity.ok(result.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Receipt image not found for update: $id (standardized)")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating receipt image: ${result.errors}")
                ResponseEntity.badRequest().build()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating receipt image: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error updating receipt image: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/receipt/image/{receiptImageId}
     * Returns 200 OK with deleted entity
     */
    @Operation(summary = "Delete receipt image by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Receipt image deleted"),
            ApiResponse(responseCode = "404", description = "Receipt image not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @DeleteMapping("/{receiptImageId}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("receiptImageId") id: Long,
    ): ResponseEntity<ReceiptImage> {
        // First get the receipt image to return it
        val receiptImageResult = receiptImageService.findById(id)
        if (receiptImageResult !is ServiceResult.Success) {
            logger.warn("Receipt image not found for deletion: $id")
            return ResponseEntity.notFound().build()
        }

        return when (val result = receiptImageService.deleteById(id)) {
            is ServiceResult.Success -> {
                logger.info("Receipt image deleted successfully: $id")
                ResponseEntity.ok(receiptImageResult.data)
            }

            is ServiceResult.NotFound -> {
                logger.warn("Receipt image not found for deletion: $id")
                ResponseEntity.notFound().build()
            }

            is ServiceResult.BusinessError -> {
                logger.warn("Business error deleting receipt image $id: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }

            is ServiceResult.SystemError -> {
                logger.error("System error deleting receipt image: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }

            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }
}
