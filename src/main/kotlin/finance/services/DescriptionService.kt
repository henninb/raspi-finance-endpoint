package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.Description
import finance.domain.ServiceResult
import finance.repositories.DescriptionRepository
import finance.repositories.TransactionRepository
import finance.utils.TenantContext
import finance.utils.orThrowNotFound
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
class DescriptionService
    constructor(
        private val descriptionRepository: DescriptionRepository,
        private val transactionRepository: TransactionRepository,
        meterService: MeterService,
        validator: Validator,
        resilienceComponents: ResilienceComponents,
    ) : CrudBaseService<Description, Long>(meterService, validator, resilienceComponents) {
        override fun getEntityName(): String = "Description"

        // ===== New Standardized ServiceResult Methods =====

        override fun findAllActive(): ServiceResult<List<Description>> =
            handleServiceOperation("findAllActive", null) {
                val owner = TenantContext.getCurrentOwner()
                val descriptions = descriptionRepository.findByOwnerAndActiveStatusOrderByDescriptionName(owner, true)

                // Batch query to get all counts at once (prevents N+1 query problem)
                val descriptionNames = descriptions.map { it.descriptionName }
                val countMap =
                    if (descriptionNames.isNotEmpty()) {
                        transactionRepository
                            .countByOwnerAndDescriptionNameIn(owner, descriptionNames)
                            .associate { row -> row[0] as String to row[1] as Long }
                    } else {
                        emptyMap()
                    }

                // Apply counts to descriptions
                descriptions.forEach { description ->
                    description.descriptionCount = countMap[description.descriptionName] ?: 0L
                }

                descriptions
            }

        override fun findById(id: Long): ServiceResult<Description> =
            handleServiceOperation("findById", id) {
                val owner = TenantContext.getCurrentOwner()
                descriptionRepository.findByOwnerAndDescriptionId(owner, id).orThrowNotFound("Description", id)
            }

        override fun save(entity: Description): ServiceResult<Description> =
            handleServiceOperation("save", entity.descriptionId) {
                val owner = TenantContext.getCurrentOwner()
                entity.owner = owner
                validateOrThrow(entity)
                val timestamp = nowTimestamp()
                entity.dateAdded = timestamp
                entity.dateUpdated = timestamp
                descriptionRepository.saveAndFlush(entity)
            }

        override fun update(entity: Description): ServiceResult<Description> =
            handleServiceOperation("update", entity.descriptionId) {
                val owner = TenantContext.getCurrentOwner()
                val descriptionToUpdate =
                    descriptionRepository
                        .findByOwnerAndDescriptionId(owner, entity.descriptionId)
                        .orThrowNotFound("Description", entity.descriptionId)
                descriptionToUpdate.descriptionName = entity.descriptionName
                descriptionToUpdate.activeStatus = entity.activeStatus
                descriptionToUpdate.dateUpdated = nowTimestamp()
                descriptionRepository.saveAndFlush(descriptionToUpdate)
            }

        override fun deleteById(id: Long): ServiceResult<Description> =
            handleServiceOperation("deleteById", id) {
                val owner = TenantContext.getCurrentOwner()
                val description = descriptionRepository.findByOwnerAndDescriptionId(owner, id).orThrowNotFound("Description", id)
                descriptionRepository.delete(description)
                description
            }

        // ===== Paginated ServiceResult Methods =====

        /**
         * Find all active descriptions with pagination.
         * Sorted by descriptionName ascending. Preserves transaction count batch loading.
         */
        fun findAllActive(pageable: Pageable): ServiceResult<Page<Description>> =
            handleServiceOperation("findAllActive-paginated", null) {
                val owner = TenantContext.getCurrentOwner()
                val page = descriptionRepository.findAllByOwnerAndActiveStatusOrderByDescriptionName(owner, true, pageable)

                // Batch query to get all counts at once (prevents N+1 query problem)
                val descriptionNames = page.content.map { it.descriptionName }
                val countMap =
                    if (descriptionNames.isNotEmpty()) {
                        transactionRepository
                            .countByOwnerAndDescriptionNameIn(owner, descriptionNames)
                            .associate { row -> row[0] as String to row[1] as Long }
                    } else {
                        emptyMap()
                    }

                // Apply counts to descriptions
                page.content.forEach { description ->
                    description.descriptionCount = countMap[description.descriptionName] ?: 0L
                }

                page
            }

        // ===== ServiceResult Business Methods for Controller =====

        fun findByDescriptionNameStandardized(descriptionName: String): ServiceResult<Description> =
            handleServiceOperation("findByDescriptionName", null) {
                val owner = TenantContext.getCurrentOwner()
                val description =
                    descriptionRepository
                        .findByOwnerAndDescriptionName(owner, descriptionName)
                        .orThrowNotFound("Description", descriptionName)
                description.descriptionCount = transactionRepository.countByOwnerAndDescriptionName(owner, description.descriptionName)
                description
            }

        fun deleteByDescriptionNameStandardized(descriptionName: String): ServiceResult<Description> =
            handleServiceOperation("deleteByDescriptionName", null) {
                val owner = TenantContext.getCurrentOwner()
                val description =
                    descriptionRepository
                        .findByOwnerAndDescriptionName(owner, descriptionName)
                        .orThrowNotFound("Description", descriptionName)
                descriptionRepository.delete(description)
                description
            }

        // ===== Legacy Method Compatibility =====

        fun fetchAllDescriptions(): List<Description> {
            val result = findAllActive()
            return when (result) {
                is ServiceResult.Success -> result.data
                else -> emptyList()
            }
        }

        fun insertDescription(description: Description): Description =
            when (val result = save(description)) {
                is ServiceResult.Success -> {
                    result.data
                }

                is ServiceResult.ValidationError -> {
                    throw ValidationException("Validation failed: ${result.errors}")
                }

                is ServiceResult.BusinessError -> {
                    if (result.errorCode == "DATA_INTEGRITY_VIOLATION") {
                        throw org.springframework.dao.DataIntegrityViolationException(result.message)
                    } else {
                        throw RuntimeException("Business error: ${result.message}")
                    }
                }

                is ServiceResult.NotFound -> {
                    throw jakarta.persistence.EntityNotFoundException("Description not found")
                }

                is ServiceResult.SystemError -> {
                    throw RuntimeException("Failed to insert description", result.exception)
                }
            }

        fun findByDescriptionName(descriptionName: String): Optional<Description> {
            val owner = TenantContext.getCurrentOwner()
            return descriptionRepository.findByOwnerAndDescriptionName(owner, descriptionName)
        }

        fun description(descriptionName: String): Optional<Description> = findByDescriptionName(descriptionName)

        @Transactional
        fun mergeDescriptions(
            targetName: String,
            sourceNames: List<String>,
        ): Description {
            val owner = TenantContext.getCurrentOwner()
            // Normalize target name (trim whitespace and convert to lowercase)
            val normalizedTargetName = targetName.trim().lowercase()

            // Find target description by normalized name
            val targetDescription =
                descriptionRepository.findByOwnerAndDescriptionName(owner, normalizedTargetName).orElseThrow {
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

                val sourceDescription =
                    descriptionRepository.findByOwnerAndDescriptionName(owner, sourceName).orElseThrow {
                        RuntimeException("Source description $sourceName not found")
                    }

                // Reassign transactions from source to target via single bulk UPDATE
                val updatedCount = transactionRepository.bulkUpdateDescriptionByOwner(owner, sourceName, normalizedTargetName)
                logger.info("Bulk updated $updatedCount transactions from $sourceName to $normalizedTargetName")

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
