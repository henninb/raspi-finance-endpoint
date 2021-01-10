package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Payment
import finance.domain.ReceiptImage
import finance.repositories.ReceiptImageRepository
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Service
open class ReceiptImageService @Autowired constructor(private var receiptImageRepository: ReceiptImageRepository,
                                                      private val validator: Validator,
                                                      private var meterService: MeterService) {

    @Transactional
    open fun insertReceiptImage(receiptImage: ReceiptImage): ReceiptImage {

        val constraintViolations: Set<ConstraintViolation<ReceiptImage>> = validator.validate(receiptImage)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
            logger.error("Cannot insert receiptImage as there is a constraint violation on the data.")
            throw ValidationException("Cannot insert receiptImage as there is a constraint violation on the data.")
        }

        receiptImage.dateAdded = Timestamp(Calendar.getInstance().time.time)
        receiptImage.dateUpdated = Timestamp(Calendar.getInstance().time.time)

        return receiptImageRepository.saveAndFlush(receiptImage)
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