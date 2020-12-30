package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.ReceiptImage
import finance.repositories.ReceiptImageRepository
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.util.*

//TODO: add meter service
@Service
open class ReceiptImageService @Autowired constructor(private var receiptImageRepository: ReceiptImageRepository) {

    @Transactional
    open fun insertReceiptImage(receiptImage: ReceiptImage): Long {
        receiptImage.dateAdded = Timestamp(Calendar.getInstance().time.time)
        receiptImage.dateUpdated = Timestamp(Calendar.getInstance().time.time)

        val response = receiptImageRepository.saveAndFlush(receiptImage)
        return response.receiptImageId
    }

    @Transactional
    open fun findByReceiptImageId(receiptImageId: Long): Optional<ReceiptImage> {
        return receiptImageRepository.findById(receiptImageId)
    }

    @Transactional
    open fun deleteReceiptImage(receiptImage: ReceiptImage): Boolean {
        receiptImageRepository.deleteById(receiptImage.receiptImageId)
        return true
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}