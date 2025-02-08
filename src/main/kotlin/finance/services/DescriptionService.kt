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
        val constraintViolations: Set<ConstraintViolation<Description>> = validator.validate(description)
        handleConstraintViolations(constraintViolations, meterService)
        description.dateAdded = Timestamp(Calendar.getInstance().time.time)
        description.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        return descriptionRepository.saveAndFlush(description)
    }

    @Timed
    override fun deleteByDescriptionName(descriptionName: String): Boolean {
        val description = descriptionRepository.findByDescriptionName(descriptionName).get()
        descriptionRepository.delete(description)
        return true
    }

    override fun description(descriptionName: String): Optional<Description> {
        val descriptionOptional: Optional<Description> = descriptionRepository.findByDescriptionName(descriptionName)
        if (descriptionOptional.isPresent) {
            return descriptionOptional
        }
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
            descriptionToUpdate.dateUpdated = Timestamp(Calendar.getInstance().time.time)
            logger.info("description update")
            return descriptionRepository.saveAndFlush(descriptionToUpdate)
        }

        throw RuntimeException("Description not updated as the description does not exist: ${description.descriptionId}.")
    }

    @Timed
    override fun fetchAllDescriptions(): List<Description> {
        val descriptions = descriptionRepository.findByActiveStatusOrderByDescriptionName(true)
        return descriptions.map { description ->
            val count = transactionRepository.countByDescriptionName(description.descriptionName)
            description.descriptionCount = count
            description
        }
    }

    @Timed
    override fun findByDescriptionName(descriptionName: String): Optional<Description> {
        return descriptionRepository.findByDescriptionName(descriptionName)
    }
}
