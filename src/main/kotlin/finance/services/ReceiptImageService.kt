package finance.services

import finance.domain.ReceiptImage
import finance.repositories.ReceiptImageRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
open class ReceiptImageService @Autowired constructor(private var receiptImageRepository: ReceiptImageRepository) {

    @Transactional
    open fun insertReceiptImage(receiptImage: ReceiptImage): Long {
        val response = receiptImageRepository.saveAndFlush(receiptImage)
        return response.receiptImageId
    }

    @Transactional
    open fun findByReceiptImageId(receiptImageId: Long) : Optional<ReceiptImage> {
        return receiptImageRepository.findById(receiptImageId)
    }

    @Transactional
    open fun deleteReceiptImage(receiptImage: ReceiptImage) :  Boolean {
        receiptImageRepository.deleteById(receiptImage.receiptImageId)
        return true
    }
}