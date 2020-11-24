package finance.services

import finance.domain.ReceiptImage
import finance.repositories.ReceiptImageRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
open class ReceiptImageService @Autowired constructor(private var receiptImageRepository: ReceiptImageRepository) {

    fun findByReceiptImageId(receiptImageId: Long): ReceiptImage {
        val optionalReceiptImage = receiptImageRepository.findById(receiptImageId)
        if( optionalReceiptImage.isPresent ) {
            return receiptImageRepository.findById(receiptImageId).get()
        }
        throw RuntimeException("cannot find the receipt image.")
    }

    @Transactional
    open fun insertReceiptImage(receiptImage: ReceiptImage): Long {
        val response = receiptImageRepository.saveAndFlush(receiptImage)
        return response.receiptImageId
    }

    @Transactional
    open fun findByReceiptId(receiptImageId: Long) : Optional<ReceiptImage> {
        return receiptImageRepository.findById(receiptImageId)
    }
}