package finance.services

import finance.domain.ReceiptImage
import finance.repositories.ReceiptImageRepository
import io.micrometer.core.annotation.Timed
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*
import jakarta.validation.ConstraintViolation

@Service
open class ReceiptImageService(
    private var receiptImageRepository: ReceiptImageRepository
) : IReceiptImageService, BaseService() {

    @Timed
    override fun insertReceiptImage(receiptImage: ReceiptImage): ReceiptImage {
        logger.info("Inserting receipt image for transaction ID: ${receiptImage.transactionId}")
        val constraintViolations: Set<ConstraintViolation<ReceiptImage>> = validator.validate(receiptImage)
        handleConstraintViolations(constraintViolations, meterService)

        val timestamp = Timestamp(System.currentTimeMillis())
        receiptImage.dateAdded = timestamp
        receiptImage.dateUpdated = timestamp

        val savedReceiptImage = receiptImageRepository.saveAndFlush(receiptImage)
        logger.info("Successfully inserted receipt image with ID: ${savedReceiptImage.receiptImageId}")
        return savedReceiptImage
    }

    @Timed
    override fun findByReceiptImageId(receiptImageId: Long): Optional<ReceiptImage> {
        logger.info("Finding receipt image by ID: $receiptImageId")
        val receiptImage = receiptImageRepository.findById(receiptImageId)
        if (receiptImage.isPresent) {
            logger.info("Found receipt image with ID: $receiptImageId")
        } else {
            logger.warn("Receipt image not found with ID: $receiptImageId")
        }
        return receiptImage
    }

    @Timed
    override fun deleteReceiptImage(receiptImage: ReceiptImage): Boolean {
        logger.info("Deleting receipt image with ID: ${receiptImage.receiptImageId}")
        receiptImageRepository.deleteById(receiptImage.receiptImageId)
        logger.info("Successfully deleted receipt image with ID: ${receiptImage.receiptImageId}")
        return true
    }
}