package finance.controllers

import finance.domain.ReceiptImage
import finance.domain.ServiceResult
import finance.services.StandardizedReceiptImageService
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
@RestController
@RequestMapping("/api/receipt/image")
class ReceiptImageController(
    private val standardizedReceiptImageService: StandardizedReceiptImageService,
) : StandardizedBaseController(),
    StandardRestController<ReceiptImage, Long> {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/receipt/image/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<ReceiptImage>> =
        when (val result = standardizedReceiptImageService.findAllActive()) {
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
    @GetMapping("/{receiptImageId}", produces = ["application/json"])
    override fun findById(
        @PathVariable("receiptImageId") id: Long,
    ): ResponseEntity<ReceiptImage> =
        when (val result = standardizedReceiptImageService.findById(id)) {
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
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: ReceiptImage,
    ): ResponseEntity<ReceiptImage> =
        when (val result = standardizedReceiptImageService.save(entity)) {
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
    @PutMapping("/{receiptImageId}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("receiptImageId") id: Long,
        @Valid @RequestBody entity: ReceiptImage,
    ): ResponseEntity<ReceiptImage> {
        @Suppress("REDUNDANT_ELSE_IN_WHEN") // Defensive programming: handle unexpected ServiceResult types
        return when (val result = standardizedReceiptImageService.update(entity)) {
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
    @DeleteMapping("/{receiptImageId}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("receiptImageId") id: Long,
    ): ResponseEntity<ReceiptImage> {
        // First get the receipt image to return it
        val receiptImageResult = standardizedReceiptImageService.findById(id)
        if (receiptImageResult !is ServiceResult.Success) {
            logger.warn("Receipt image not found for deletion: $id")
            return ResponseEntity.notFound().build()
        }

        return when (val result = standardizedReceiptImageService.deleteById(id)) {
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
