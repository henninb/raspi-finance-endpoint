package finance.services

import finance.domain.ReceiptImage
import java.util.*

interface IReceiptImageService {
    fun insertReceiptImage(receiptImage: ReceiptImage): ReceiptImage
    fun findByReceiptImageId(receiptImageId: Long): Optional<ReceiptImage>
    fun deleteReceiptImage(receiptImage: ReceiptImage): Boolean
}