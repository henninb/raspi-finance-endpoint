package finance.services

import finance.domain.Description
import finance.repositories.DescriptionRepository
import io.micrometer.core.annotation.Timed
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*
import jakarta.validation.ConstraintViolation

@Service
open class DescriptionService(
    private var descriptionRepository: DescriptionRepository
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

//    @Timed
//    override fun descriptions(): List<Description> {
//        return descriptionRepository.findByActiveStatusOrderByDescriptionName(true)
//    }

    @Timed
    override fun fetchAllDescriptions(): List<Description> {
        return descriptionRepository.findByActiveStatusOrderByDescriptionName(true)
    }

    @Timed
    override fun findByDescriptionName(descriptionName: String): Optional<Description> {
        return descriptionRepository.findByDescriptionName(descriptionName)
    }
}
