package finance.services

import finance.domain.ReceiptImage
import finance.domain.ServiceResult
import finance.repositories.ReceiptImageRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp

/**
 * Standardized Receipt Image Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@org.springframework.context.annotation.Primary
open class ReceiptImageService(
    private val receiptImageRepository: ReceiptImageRepository,
) : CrudBaseService<ReceiptImage, Long>() {
    override fun getEntityName(): String = "ReceiptImage"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<ReceiptImage>> =
        handleServiceOperation("findAllActive", null) {
            receiptImageRepository.findAll()
        }

    override fun findById(id: Long): ServiceResult<ReceiptImage> =
        handleServiceOperation("findById", id) {
            val optionalReceiptImage = receiptImageRepository.findById(id)
            if (optionalReceiptImage.isPresent) {
                optionalReceiptImage.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("ReceiptImage not found: $id")
            }
        }

    override fun save(entity: ReceiptImage): ServiceResult<ReceiptImage> =
        handleServiceOperation("save", entity.receiptImageId) {
            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }

            // Set timestamps
            val timestamp = Timestamp(System.currentTimeMillis())
            entity.dateAdded = timestamp
            entity.dateUpdated = timestamp

            receiptImageRepository.saveAndFlush(entity)
        }

    override fun update(entity: ReceiptImage): ServiceResult<ReceiptImage> =
        handleServiceOperation("update", entity.receiptImageId) {
            val existingReceiptImage = receiptImageRepository.findById(entity.receiptImageId)
            if (existingReceiptImage.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("ReceiptImage not found: ${entity.receiptImageId}")
            }

            // Update timestamp
            entity.dateUpdated = Timestamp(System.currentTimeMillis())

            receiptImageRepository.saveAndFlush(entity)
        }

    override fun deleteById(id: Long): ServiceResult<Boolean> =
        handleServiceOperation("deleteById", id) {
            val optionalReceiptImage = receiptImageRepository.findById(id)
            if (optionalReceiptImage.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("ReceiptImage not found: $id")
            }
            receiptImageRepository.deleteById(id)
            true
        }

    // ===== Additional ServiceResult Methods =====

    fun findByTransactionId(transactionId: Long): ServiceResult<ReceiptImage> =
        handleServiceOperation("findByTransactionId", transactionId) {
            val optionalReceiptImage = receiptImageRepository.findByTransactionId(transactionId)
            if (optionalReceiptImage.isPresent) {
                optionalReceiptImage.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("ReceiptImage not found for transaction: $transactionId")
            }
        }
}
