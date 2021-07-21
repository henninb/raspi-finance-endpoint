package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.ReceiptImage
import finance.repositories.ReceiptImageRepository
import io.micrometer.core.annotation.Timed
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Service
open class ReceiptImageService(
    private var receiptImageRepository: ReceiptImageRepository
) : IReceiptImageService, BaseService() {

    @Timed
    override fun insertReceiptImage(receiptImage: ReceiptImage): ReceiptImage {
        val constraintViolations: Set<ConstraintViolation<ReceiptImage>> = validator.validate(receiptImage)
        handleConstraintViolations(constraintViolations, meterService)

        receiptImage.dateAdded = Timestamp(Calendar.getInstance().time.time)
        receiptImage.dateUpdated = Timestamp(Calendar.getInstance().time.time)

        return receiptImageRepository.saveAndFlush(receiptImage)
    }

    @Timed
    override fun findByReceiptImageId(receiptImageId: Long): Optional<ReceiptImage> {
        return receiptImageRepository.findById(receiptImageId)
    }

    @Timed
    override fun deleteReceiptImage(receiptImage: ReceiptImage): Boolean {
        receiptImageRepository.deleteById(receiptImage.receiptImageId)
        return true
    }
}