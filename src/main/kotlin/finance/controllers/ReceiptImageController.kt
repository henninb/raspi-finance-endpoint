package finance.controllers

import finance.domain.Payment
import finance.services.ReceiptImageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/receipt/image")
class ReceiptImageController(private var receiptImageService: ReceiptImageService) {

    @PutMapping(path = ["/update/{transactionId}"], produces = ["application/json"])
    fun updateReceiptImageByTransactionId(@PathVariable("transactionId") transactionId: Long, @RequestBody payload: String): ResponseEntity<String> {
        receiptImageService.updateReceiptImageByTransactionId(transactionId, payload.toByteArray())
        return ResponseEntity.ok("update transaction receipt image")
    }

    // curl https://hornsup:3000/receipt/image/insert/1
    @PostMapping(path = ["/insert/{transactionId}"], produces = ["application/json"])
    fun insertReceiptImageByTransactionId(@PathVariable("transactionId") transactionId: Long, @RequestBody payload: String): ResponseEntity<String> {
        receiptImageService.insertReceiptImageByTransactionId(transactionId, payload.toByteArray())
        return ResponseEntity.ok("insert transaction receipt image")
    }
}