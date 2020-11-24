package finance.controllers

import finance.services.ReceiptImageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/receipt/image")
class ReceiptImageController(private var receiptImageService: ReceiptImageService) {

    //http://localhost:8080/account/select
    @GetMapping(path = ["/select/{receipt_image_id}"])
    fun selectReceiptImage(@PathVariable("receipt_image_id") receiptImageId: Long): ResponseEntity<String> {
        val receiptImage = receiptImageService.findByReceiptImageId(receiptImageId)
        return ResponseEntity.ok(receiptImage.jpgImage.toString(Charsets.UTF_8))
    }
}