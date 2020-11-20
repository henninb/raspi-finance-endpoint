package finance.services

import finance.repositories.ReceiptImageRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ReceiptImageService @Autowired constructor(private var receiptImageRepository: ReceiptImageRepository) {

    fun insertReceiptImageByTransactionId(transactionId: Long, receiptImage: ByteArray): Boolean {
        receiptImageRepository.insertReceiptImageByTransactionId(transactionId, receiptImage)
        return true
    }

    fun updateReceiptImageByTransactionId(transactionId: Long, receiptImage: ByteArray): Boolean {
        receiptImageRepository.updateReceiptImageByTransactionId(transactionId, receiptImage)
        return true
    }

    fun findByTransactionId(transactionId: Long): Long {
        val receiptImage = receiptImageRepository.findByTransactionId(transactionId)
        return receiptImage.receipt_image_id
    }
}