package finance.controllers

import finance.services.ReceiptImageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.lang.RuntimeException

@CrossOrigin
@RestController
@RequestMapping("/receipt/image")
class ReceiptImageController(private var receiptImageService: ReceiptImageService) {

    //http://localhost:8080/account/select
    @GetMapping(path = ["/select/{receipt_image_id}"])
    fun selectReceiptImage(@PathVariable("receipt_image_id") receiptImageId: Long): ResponseEntity<String> {
        val receiptImageOptional = receiptImageService.findByReceiptImageId(receiptImageId)
        if (receiptImageOptional.isPresent) {
            return ResponseEntity.ok(receiptImageOptional.get().jpgImage.toString(Charsets.UTF_8))
        }
        throw RuntimeException("failed to find a receipt image for $receiptImageId")
    }
}