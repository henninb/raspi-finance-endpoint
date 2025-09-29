package finance.controllers

import finance.domain.ReceiptImage
import finance.domain.ServiceResult
import finance.services.StandardizedReceiptImageService
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
@RequestMapping("/api/receipt/image")
class ReceiptImageController(private var standardizedReceiptImageService: StandardizedReceiptImageService) : BaseController() {
    // curl -k --header "Content-Type: application/json" --request POST --data '{"transactionId": 1, "image": "base64encodedimage"}' https://localhost:8443/receipt/image/insert
    @PostMapping("/insert", produces = ["application/json"])
    fun insertReceiptImage(
        @Valid @RequestBody receiptImage: ReceiptImage,
        bindingResult: BindingResult,
    ): ResponseEntity<Map<String, String>> {
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            val errors = bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
            logger.warn("Receipt image validation failed: $errors")
            return ResponseEntity.badRequest().body(mapOf("errors" to errors.toString()))
        }

        return when (val result = standardizedReceiptImageService.save(receiptImage)) {
            is ServiceResult.Success -> {
                logger.info("Receipt image created successfully: ${result.data.receiptImageId}")
                ResponseEntity.ok(mapOf("message" to "Receipt image inserted", "id" to result.data.receiptImageId.toString()))
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating receipt image: ${result.errors}")
                ResponseEntity.badRequest().body(mapOf("errors" to result.errors.toString()))
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating receipt image: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to result.message))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error creating receipt image: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Failed to insert receipt image"))
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Failed to insert receipt image"))
            }
        }
    }

    // curl -k https://localhost:8443/receipt/image/select/1
    @GetMapping("/select/{receipt_image_id}")
    fun selectReceiptImage(
        @PathVariable("receipt_image_id") @Positive(message = "Receipt image ID must be positive") receiptImageId: Long,
    ): ResponseEntity<Map<String, Any>> {
        return when (val result = standardizedReceiptImageService.findById(receiptImageId)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved receipt image: $receiptImageId")
                ResponseEntity.ok(
                    mapOf(
                        "receiptImage" to result.data,
                        "message" to "Receipt image found",
                    ),
                )
            }
            is ServiceResult.NotFound -> {
                logger.warn("Receipt image not found: $receiptImageId")
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to result.message))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving receipt image $receiptImageId: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Error retrieving receipt image"))
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Error retrieving receipt image"))
            }
        }
    }
}
