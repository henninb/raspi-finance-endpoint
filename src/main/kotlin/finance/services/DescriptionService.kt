package finance.services

import finance.domain.Description
import finance.repositories.DescriptionRepository
import finance.repositories.TransactionRepository
import io.micrometer.core.annotation.Timed
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*
import jakarta.validation.ConstraintViolation

@Service
open class DescriptionService(
    private var descriptionRepository: DescriptionRepository,
    private var transactionRepository: TransactionRepository
) : IDescriptionService, BaseService() {

    @Timed
    override fun insertDescription(description: Description): Description {
        logger.info("Inserting description: ${description.descriptionName}")
        val constraintViolations: Set<ConstraintViolation<Description>> = validator.validate(description)
        handleConstraintViolations(constraintViolations, meterService)
        val timestamp = Timestamp(System.currentTimeMillis())
        description.dateAdded = timestamp
        description.dateUpdated = timestamp
        val savedDescription = descriptionRepository.saveAndFlush(description)
        logger.info("Successfully inserted description: ${savedDescription.descriptionName} with ID: ${savedDescription.descriptionId}")
        return savedDescription
    }

    @Timed
    override fun deleteByDescriptionName(descriptionName: String): Boolean {
        logger.info("Deleting description: $descriptionName")
        val descriptionOptional = descriptionRepository.findByDescriptionName(descriptionName)
        if (descriptionOptional.isPresent) {
            descriptionRepository.delete(descriptionOptional.get())
            logger.info("Successfully deleted description: $descriptionName")
            return true
        }
        logger.warn("Description not found for deletion: $descriptionName")
        return false
    }

    override fun description(descriptionName: String): Optional<Description> {
        logger.info("Finding description: $descriptionName")
        val descriptionOptional: Optional<Description> = descriptionRepository.findByDescriptionName(descriptionName)
        if (descriptionOptional.isPresent) {
            logger.info("Found description: $descriptionName")
            return descriptionOptional
        }
        logger.warn("Description not found: $descriptionName")
        return Optional.empty()
    }


    @Transactional
    @Timed
    override fun updateDescription(description: Description): Description {
        val optionalDescription = descriptionRepository.findByDescriptionId(description.descriptionId)

        if (optionalDescription.isPresent) {
            val descriptionToUpdate = optionalDescription.get()

            // Updating fields
            descriptionToUpdate.descriptionName = description.descriptionName
            descriptionToUpdate.activeStatus = description.activeStatus
            descriptionToUpdate.dateUpdated = Timestamp(System.currentTimeMillis())
            logger.info("Updating description: ${descriptionToUpdate.descriptionName}")
            return descriptionRepository.saveAndFlush(descriptionToUpdate)
        }

        throw RuntimeException("Description not updated as the description does not exist: ${description.descriptionId}.")
    }

    @Timed
    override fun fetchAllDescriptions(): List<Description> {
        logger.info("Fetching all active descriptions")
        val descriptions = descriptionRepository.findByActiveStatusOrderByDescriptionName(true)
        logger.info("Found ${descriptions.size} active descriptions")
        return descriptions
    }

    @Timed
    override fun findByDescriptionName(descriptionName: String): Optional<Description> {
        logger.info("Finding description by name: $descriptionName")
        val description = descriptionRepository.findByDescriptionName(descriptionName)
        if (description.isPresent) {
            logger.info("Found description: $descriptionName")
        } else {
            logger.warn("Description not found: $descriptionName")
        }
        return description
    }
}
