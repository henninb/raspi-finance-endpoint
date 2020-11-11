package finance.services

import finance.domain.Description
import finance.repositories.DescriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Service
open class DescriptionService(private var descriptionRepository: DescriptionRepository, private val validator: Validator) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun insertDescription(description: Description): Boolean {
        val constraintViolations: Set<ConstraintViolation<Description>> = validator.validate(description)
        if (constraintViolations.isNotEmpty()) {
            logger.error("Cannot insert description as there is a constraint violation on the data.")
            throw ValidationException("Cannot insert description as there is a constraint violation on the data.")
        }
        descriptionRepository.saveAndFlush(description)
        return true
    }

    fun deleteByDescription(description: String): Boolean {
        logger.info("deleteByCategory")

        descriptionRepository.deleteByDescription(description)
        return true
    }

    fun fetchAllDescriptions(): List<Description> {
        return descriptionRepository.findByActiveStatusOrderByDescription(true)
    }

//    fun findByDescription(description: String): Optional<Description> {
//        val descriptionOptional: Optional<Description> = descriptionRepository.findByDescription(description)
//        if (descriptionOptional.isPresent) {
//            return descriptionOptional
//        }
//        return Optional.empty()
//    }
}