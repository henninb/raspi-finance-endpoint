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

        override fun findAllActive(): ServiceResult<List<Description>> =
            handleServiceOperation("findAllActive", null) {
                val owner = TenantContext.getCurrentOwner()
                val descriptions = descriptionRepository.findByOwnerAndActiveStatusOrderByDescriptionName(owner, true)
                applyDescriptionCounts(owner, descriptions)
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

        fun findAllActive(pageable: Pageable): ServiceResult<Page<Description>> =
            handleServiceOperation("findAllActive-paginated", null) {
                val owner = TenantContext.getCurrentOwner()
                val page = descriptionRepository.findAllByOwnerAndActiveStatusOrderByDescriptionName(owner, true, pageable)
                applyDescriptionCounts(owner, page.content)
                page
            }

        private fun applyDescriptionCounts(
            owner: String,
            descriptions: List<Description>,
        ) {
            if (descriptions.isEmpty()) return
            val countMap =
                transactionRepository
                    .countByOwnerAndDescriptionNameIn(owner, descriptions.map { it.descriptionName })
                    .associate { row -> row[0] as String to row[1] as Long }
            descriptions.forEach { it.descriptionCount = countMap[it.descriptionName] ?: 0L }
        }

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
            val normalizedTargetName = targetName.trim().lowercase()

            val targetDescription =
                descriptionRepository.findByOwnerAndDescriptionName(owner, normalizedTargetName).orElseThrow {
                    RuntimeException("Target description $normalizedTargetName not found")
                }

            logger.info("Merging descriptions: ${sourceNames.joinToString(", ")} into $normalizedTargetName")

            var totalMergedCount = targetDescription.descriptionCount

            sourceNames.forEach { sourceName ->
                val normalizedSourceName = sourceName.trim().lowercase()

                if (normalizedSourceName == normalizedTargetName) {
                    logger.info("Skipping self-merge: $sourceName normalizes to same as target $normalizedTargetName")
                    return@forEach
                }

                val sourceDescription =
                    descriptionRepository.findByOwnerAndDescriptionName(owner, sourceName).orElseThrow {
                        RuntimeException("Source description $sourceName not found")
                    }

                val updatedCount = transactionRepository.bulkUpdateDescriptionByOwner(owner, sourceName, normalizedTargetName)
                logger.info("Bulk updated $updatedCount transactions from $sourceName to $normalizedTargetName")

                totalMergedCount += sourceDescription.descriptionCount
                sourceDescription.activeStatus = false
                descriptionRepository.saveAndFlush(sourceDescription)
            }

            targetDescription.descriptionCount = totalMergedCount

            val mergedDescription = descriptionRepository.saveAndFlush(targetDescription)
            logger.info("Successfully merged descriptions ${sourceNames.joinToString(", ")} into $normalizedTargetName")

            return mergedDescription
        }
    }
