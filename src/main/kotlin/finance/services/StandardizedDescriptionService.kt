package finance.services

import finance.domain.Description
import finance.domain.ServiceResult
import finance.repositories.DescriptionRepository
import finance.repositories.TransactionRepository
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*

/**
 * Standardized Description Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@Primary
class StandardizedDescriptionService(
    private val descriptionRepository: DescriptionRepository,
    private val transactionRepository: TransactionRepository
) : StandardizedBaseService<Description, Long>(), IDescriptionService {

    override fun getEntityName(): String = "Description"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Description>> {
        return handleServiceOperation("findAllActive", null) {
            val descriptions = descriptionRepository.findByActiveStatusOrderByDescriptionName(true)
            descriptions.map { description ->
                val count = transactionRepository.countByDescriptionName(description.descriptionName)
                description.descriptionCount = count
                description
            }
        }
    }

    override fun findById(id: Long): ServiceResult<Description> {
        return handleServiceOperation("findById", id) {
            val optionalDescription = descriptionRepository.findByDescriptionId(id)
            if (optionalDescription.isPresent) {
                optionalDescription.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Description not found: $id")
            }
        }
    }

    override fun save(entity: Description): ServiceResult<Description> {
        return handleServiceOperation("save", entity.descriptionId) {
            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }

            // Set timestamps
            val timestamp = Timestamp(System.currentTimeMillis())
            entity.dateAdded = timestamp
            entity.dateUpdated = timestamp

            descriptionRepository.saveAndFlush(entity)
        }
    }

    override fun update(entity: Description): ServiceResult<Description> {
        return handleServiceOperation("update", entity.descriptionId) {
            val existingDescription = descriptionRepository.findByDescriptionId(entity.descriptionId!!)
            if (existingDescription.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Description not found: ${entity.descriptionId}")
            }

            // Update fields from the provided entity
            val descriptionToUpdate = existingDescription.get()
            descriptionToUpdate.descriptionName = entity.descriptionName
            descriptionToUpdate.activeStatus = entity.activeStatus
            descriptionToUpdate.dateUpdated = Timestamp(System.currentTimeMillis())

            descriptionRepository.saveAndFlush(descriptionToUpdate)
        }
    }

    override fun deleteById(id: Long): ServiceResult<Boolean> {
        return handleServiceOperation("deleteById", id) {
            val optionalDescription = descriptionRepository.findByDescriptionId(id)
            if (optionalDescription.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Description not found: $id")
            }
            descriptionRepository.delete(optionalDescription.get())
            true
        }
    }

    // ===== ServiceResult Business Methods for Controller =====

    fun findByDescriptionNameStandardized(descriptionName: String): ServiceResult<Description> {
        return handleServiceOperation("findByDescriptionName", null) {
            val optionalDescription = descriptionRepository.findByDescriptionName(descriptionName)
            if (optionalDescription.isPresent) {
                val description = optionalDescription.get()
                val count = transactionRepository.countByDescriptionName(description.descriptionName)
                description.descriptionCount = count
                description
            } else {
                throw jakarta.persistence.EntityNotFoundException("Description not found: $descriptionName")
            }
        }
    }

    fun deleteByDescriptionNameStandardized(descriptionName: String): ServiceResult<Boolean> {
        return handleServiceOperation("deleteByDescriptionName", null) {
            val optionalDescription = descriptionRepository.findByDescriptionName(descriptionName)
            if (optionalDescription.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Description not found: $descriptionName")
            }
            descriptionRepository.delete(optionalDescription.get())
            true
        }
    }

    // ===== Legacy Method Compatibility =====

    override fun fetchAllDescriptions(): List<Description> {
        val result = findAllActive()
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    override fun insertDescription(description: Description): Description {
        val result = save(description)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.ValidationError -> {
                val violations = result.errors.map { (field, message) ->
                    object : jakarta.validation.ConstraintViolation<Description> {
                        override fun getMessage(): String = message
                        override fun getMessageTemplate(): String = message
                        override fun getRootBean(): Description = description
                        override fun getRootBeanClass(): Class<Description> = Description::class.java
                        override fun getLeafBean(): Any = description
                        override fun getExecutableParameters(): Array<Any> = emptyArray()
                        override fun getExecutableReturnValue(): Any? = null
                        override fun getPropertyPath(): jakarta.validation.Path {
                            return object : jakarta.validation.Path {
                                override fun toString(): String = field
                                override fun iterator(): MutableIterator<jakarta.validation.Path.Node> = mutableListOf<jakarta.validation.Path.Node>().iterator()
                            }
                        }
                        override fun getInvalidValue(): Any? = null
                        override fun getConstraintDescriptor(): jakarta.validation.metadata.ConstraintDescriptor<*>? = null
                        override fun <U : Any?> unwrap(type: Class<U>?): U = throw UnsupportedOperationException()
                    }
                }.toSet()
                throw ValidationException(jakarta.validation.ConstraintViolationException("Validation failed", violations))
            }
            is ServiceResult.BusinessError -> {
                if (result.errorCode == "DATA_INTEGRITY_VIOLATION") {
                    throw org.springframework.dao.DataIntegrityViolationException(result.message)
                } else {
                    throw RuntimeException("Business error: ${result.message}")
                }
            }
            else -> throw RuntimeException("Failed to insert description: ${result}")
        }
    }


    override fun findByDescriptionName(descriptionName: String): Optional<Description> {
        return descriptionRepository.findByDescriptionName(descriptionName)
    }

    override fun description(descriptionName: String): Optional<Description> {
        return findByDescriptionName(descriptionName)
    }


    override fun mergeDescriptions(targetName: String, sourceNames: List<String>): Description {
        // Normalize target name (trim whitespace and convert to lowercase)
        val normalizedTargetName = targetName.trim().lowercase()

        // Find target description by normalized name
        val targetDescription = descriptionRepository.findByDescriptionName(normalizedTargetName).orElseThrow {
            RuntimeException("Target description $normalizedTargetName not found")
        }

        logger.info("Merging descriptions: ${sourceNames.joinToString(", ")} into $normalizedTargetName")

        var totalMergedCount = targetDescription.descriptionCount

        // Process each source description, with normalization for self-merge detection
        sourceNames.forEach { sourceName ->
            val normalizedSourceName = sourceName.trim().lowercase()

            // Skip self-merge when normalized source equals normalized target
            if (normalizedSourceName == normalizedTargetName) {
                logger.info("Skipping self-merge: $sourceName normalizes to same as target $normalizedTargetName")
                return@forEach
            }

            val sourceDescription = descriptionRepository.findByDescriptionName(sourceName).orElseThrow {
                RuntimeException("Source description $sourceName not found")
            }

            // Reassign transactions from source to target
            val transactionsToUpdate = transactionRepository.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(sourceName, true)
            logger.info("Found ${transactionsToUpdate.size} transactions to reassign from $sourceName to $normalizedTargetName")

            transactionsToUpdate.forEach { transaction ->
                transaction.description = normalizedTargetName
                transactionRepository.saveAndFlush(transaction)
            }

            // Merge description counts
            totalMergedCount += sourceDescription.descriptionCount

            // Mark source description as inactive
            sourceDescription.activeStatus = false
            descriptionRepository.saveAndFlush(sourceDescription)
        }

        // Update target description with merged count
        targetDescription.descriptionCount = totalMergedCount

        // Save the updated target description
        val mergedDescription = descriptionRepository.saveAndFlush(targetDescription)
        logger.info("Successfully merged descriptions ${sourceNames.joinToString(", ")} into $normalizedTargetName")

        return mergedDescription
    }
}