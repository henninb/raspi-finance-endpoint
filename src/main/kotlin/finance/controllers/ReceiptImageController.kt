package finance.controllers

import finance.domain.Payment
import finance.domain.ReceiptImage
import finance.services.MeterService
import finance.services.ReceiptImageService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@CrossOrigin
@RestController
@RequestMapping("/receipt/image")
class ReceiptImageController(private var receiptImageService: ReceiptImageService): BaseController() {

    @PostMapping(path = ["/insert"], produces = ["application/json"])
    fun insertReceiptImage(@RequestBody receiptImage: ReceiptImage): ResponseEntity<String> {
        val receiptImageId = receiptImageService.insertReceiptImage(receiptImage)
        return ResponseEntity.ok("receiptImage inserted $receiptImageId")
    }

    // curl -k 'https://localhost:8080/receipt/image/select/1'
    @GetMapping(path = ["/select/{receipt_image_id}"])
    fun selectReceiptImage(@PathVariable("receipt_image_id") receiptImageId: Long): ResponseEntity<String> {
        val receiptImageOptional = receiptImageService.findByReceiptImageId(receiptImageId)
        if (receiptImageOptional.isPresent) {
            return ResponseEntity.ok(receiptImageOptional.get().jpgImage.toString(Charsets.UTF_8))
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "receipt image not found for: $receiptImageId")
    }
}