package finance.services

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.domain.ServiceResult
import finance.repositories.FamilyMemberRepository
import jakarta.validation.ValidationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.sql.Timestamp

/**
 * Standardized FamilyMember Service with ServiceResult patterns
 * Provides enhanced error handling and standardized CRUD operations
 */
@Service
@org.springframework.context.annotation.Primary
class FamilyMemberService(
    private val familyMemberRepository: FamilyMemberRepository,
) : BaseService() {
    // ===== ServiceResult Methods =====

    fun findAllActive(): ServiceResult<List<FamilyMember>> =
        try {
            ServiceResult.Success(familyMemberRepository.findByActiveStatusTrue())
        } catch (e: Exception) {
            logger.error("Error retrieving all family members", e)
            ServiceResult.SystemError(e)
        }

    fun findByIdServiceResult(id: Long): ServiceResult<FamilyMember> =
        try {
            val fm = familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(id)
            if (fm != null) ServiceResult.Success(fm) else ServiceResult.NotFound("FamilyMember not found: $id")
        } catch (e: Exception) {
            logger.error("Error retrieving family member by ID: $id", e)
            ServiceResult.SystemError(e)
        }

    fun findByIdAnyStatus(id: Long): ServiceResult<FamilyMember> =
        try {
            val familyMember = familyMemberRepository.findById(id).orElse(null)
            if (familyMember != null) {
                ServiceResult.Success(familyMember)
            } else {
                ServiceResult.NotFound("FamilyMember not found: $id")
            }
        } catch (e: Exception) {
            logger.error("Error retrieving family member by ID (any status): $id", e)
            ServiceResult.SystemError(e)
        }

    fun save(entity: FamilyMember): ServiceResult<FamilyMember> {
        return try {
            // Check if family member already exists first (before validation for test compatibility)
            val existingMember = familyMemberRepository.findByOwnerAndMemberName(entity.owner, entity.memberName)
            if (existingMember != null) {
                return ServiceResult.BusinessError(
                    "Family member already exists for owner='${entity.owner}', name='${entity.memberName}'",
                    "DATA_INTEGRITY_VIOLATION",
                )
            }

            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                return ServiceResult.ValidationError(
                    violations.associate { (it.propertyPath?.toString() ?: "unknown") to it.message },
                )
            }

            val timestamp = Timestamp(System.currentTimeMillis())
            entity.dateAdded = timestamp
            entity.dateUpdated = timestamp

            ServiceResult.Success(familyMemberRepository.save(entity))
        } catch (e: jakarta.validation.ConstraintViolationException) {
            val errorMap =
                e.constraintViolations.associate {
                    (it.propertyPath?.toString() ?: "unknown") to it.message
                }
            ServiceResult.ValidationError(errorMap)
        } catch (e: Exception) {
            logger.error("Error saving family member", e)
            ServiceResult.SystemError(e)
        }
    }

    fun update(entity: FamilyMember): ServiceResult<FamilyMember> {
        return try {
            val existingMember =
                familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(entity.familyMemberId)
                    ?: return ServiceResult.NotFound("FamilyMember not found: ${entity.familyMemberId}")

            // Keep immutable fields from existing if needed; for now, update timestamps only
            entity.dateAdded = existingMember.dateAdded
            entity.dateUpdated = Timestamp(System.currentTimeMillis())
            ServiceResult.Success(familyMemberRepository.save(entity))
        } catch (e: Exception) {
            logger.error("Error updating family member", e)
            ServiceResult.SystemError(e)
        }
    }

    fun deleteById(id: Long): ServiceResult<Boolean> {
        return try {
            val existingMember =
                familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(id)
                    ?: return ServiceResult.NotFound("FamilyMember not found: $id")

            val updatedRows = familyMemberRepository.softDeleteByFamilyMemberId(id)
            ServiceResult.Success(updatedRows > 0)
        } catch (e: Exception) {
            logger.error("Error deleting family member", e)
            ServiceResult.SystemError(e)
        }
    }

    /**
     * Delete by id regardless of current active status.
     * Used by controller to support deleting already-deactivated members (functional spec).
     */
    fun deleteByIdAnyStatus(id: Long): ServiceResult<Boolean> {
        return try {
            val existingMember = familyMemberRepository.findById(id).orElse(null)
            if (existingMember == null) {
                return ServiceResult.NotFound("FamilyMember not found: $id")
            }
            val updatedRows = familyMemberRepository.softDeleteByFamilyMemberId(id)
            ServiceResult.Success(updatedRows > 0)
        } catch (e: Exception) {
            logger.error("Error deleting family member (any status)", e)
            ServiceResult.SystemError(e)
        }
    }

    // ===== Legacy Methods for Backward Compatibility =====

    fun findById(id: Long): FamilyMember? {
        logger.debug("Finding family member by ID: $id")
        return familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(id)
    }

    fun insertFamilyMember(member: FamilyMember): FamilyMember {
        logger.info("Inserting family member for owner: ${member.owner}")

        val result = save(member)
        return when (result) {
            is ServiceResult.Success -> {
                logger.info("Successfully inserted family member with ID: ${result.data.familyMemberId}")
                result.data
            }

            is ServiceResult.ValidationError -> {
                val message = "Validation failed: ${result.errors}"
                logger.error(message)
                throw ValidationException(message)
            }

            is ServiceResult.BusinessError -> {
                logger.error("Business error inserting family member: ${result.message}")
                throw DataIntegrityViolationException(result.message)
            }

            else -> {
                val message = "Failed to insert family member: $result"
                logger.error(message)
                throw RuntimeException(message)
            }
        }
    }

    fun updateFamilyMember(member: FamilyMember): FamilyMember {
        logger.info("Updating family member with ID: ${member.familyMemberId}")

        val result = update(member)
        return when (result) {
            is ServiceResult.Success -> {
                logger.info("Successfully updated family member with ID: ${result.data.familyMemberId}")
                result.data
            }

            is ServiceResult.NotFound -> {
                val message = "Family member not found with ID: ${member.familyMemberId}"
                logger.error(message)
                throw IllegalArgumentException(message)
            }

            is ServiceResult.BusinessError -> {
                logger.error("Business error updating family member: ${result.message}")
                throw DataIntegrityViolationException(result.message)
            }

            else -> {
                val message = "Failed to update family member: $result"
                logger.error(message)
                throw RuntimeException(message)
            }
        }
    }

    fun findFamilyMemberById(id: Long): FamilyMember? {
        logger.debug("Finding family member by ID: $id")
        return familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(id)
    }

    fun findByOwner(owner: String): List<FamilyMember> {
        logger.debug("Finding family members by owner: $owner")
        return familyMemberRepository.findByOwnerAndActiveStatusTrue(owner)
    }

    fun findByOwnerAndRelationship(
        owner: String,
        relationship: FamilyRelationship,
    ): List<FamilyMember> {
        logger.debug("Finding family members by owner: $owner and relationship: $relationship")
        return familyMemberRepository.findByOwnerAndRelationshipAndActiveStatusTrue(owner, relationship)
    }

    fun findAll(): List<FamilyMember> {
        logger.debug("Finding all active family members")
        return familyMemberRepository.findByActiveStatusTrue()
    }

    fun updateActiveStatus(
        id: Long,
        active: Boolean,
    ): Boolean {
        logger.info("Updating active status for family member ID: $id to: $active")
        return try {
            // Check if family member exists first
            val existingMember = familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(id)
            if (existingMember == null) {
                return false
            }

            val updatedRows = familyMemberRepository.updateActiveStatus(id, active)
            updatedRows > 0
        } catch (e: Exception) {
            logger.error("Error updating active status for family member ID: $id", e)
            false
        }
    }

    fun softDelete(id: Long): Boolean {
        logger.info("Soft deleting family member with ID: $id")
        return try {
            // Check if family member exists first (regardless of active status)
            val existingMember = familyMemberRepository.findById(id).orElse(null)
            if (existingMember == null) {
                return false
            }

            val updatedRows = familyMemberRepository.softDeleteByFamilyMemberId(id)
            updatedRows > 0
        } catch (e: Exception) {
            logger.error("Error soft deleting family member with ID: $id", e)
            false
        }
    }
}
