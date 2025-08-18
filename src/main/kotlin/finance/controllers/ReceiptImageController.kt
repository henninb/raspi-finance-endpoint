package finance.controllers

import finance.domain.ReceiptImage
import finance.services.ReceiptImageService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.validation.BindingResult

@CrossOrigin
@RestController
@RequestMapping("/receipt/image", "/api/receipt/image")
class ReceiptImageController(private var receiptImageService: ReceiptImageService) : BaseController() {

    // curl -k --header "Content-Type: application/json" --request POST --data '{"transactionId": 1, "image": "base64encodedimage"}' https://localhost:8443/receipt/image/insert
    @PostMapping("/insert", produces = ["application/json"])
    fun insertReceiptImage(
        @Valid @RequestBody receiptImage: ReceiptImage,
        bindingResult: BindingResult
    ): ResponseEntity<Map<String, String>> {
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            val errors = bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
            logger.warn("Receipt image validation failed: $errors")
            return ResponseEntity.badRequest().body(mapOf("errors" to errors.toString()))
        }

        try {
            val receiptImageId = receiptImageService.insertReceiptImage(receiptImage)
            return ResponseEntity.ok(mapOf("message" to "Receipt image inserted", "id" to receiptImageId.toString()))
        } catch (e: Exception) {
            logger.error("Failed to insert receipt image")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Failed to insert receipt image"))
        }
    }

    // curl -k https://localhost:8443/receipt/image/select/1
    @GetMapping("/select/{receipt_image_id}")
    fun selectReceiptImage(
        @PathVariable("receipt_image_id") @Positive(message = "Receipt image ID must be positive") receiptImageId: Long
    ): ResponseEntity<Map<String, Any>> {
        try {
            val receiptImageOptional = receiptImageService.findByReceiptImageId(receiptImageId)
            if (receiptImageOptional.isPresent) {
                return ResponseEntity.ok(mapOf(
                    "receiptImage" to receiptImageOptional.get(),
                    "message" to "Receipt image found"
                ))
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Receipt image not found"))
        } catch (e: Exception) {
            logger.error("Error retrieving receipt image with ID: $receiptImageId")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Error retrieving receipt image"))
        }
    }
}