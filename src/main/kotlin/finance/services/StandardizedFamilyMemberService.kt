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
 * Standardized FamilyMember Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
class StandardizedFamilyMemberService(
    private val familyMemberRepository: FamilyMemberRepository
) : StandardizedBaseService<FamilyMember, Long>() {

    override fun getEntityName(): String = "FamilyMember"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<FamilyMember>> {
        return handleServiceOperation("findAllActive", null) {
            familyMemberRepository.findByActiveStatusTrue()
        }
    }

    override fun findById(id: Long): ServiceResult<FamilyMember> {
        return handleServiceOperation("findById", id) {
            val member = familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(id)
            if (member != null) {
                member
            } else {
                throw jakarta.persistence.EntityNotFoundException("FamilyMember not found: $id")
            }
        }
    }

    override fun save(entity: FamilyMember): ServiceResult<FamilyMember> {
        return handleServiceOperation("save", entity.familyMemberId) {
            // Check if family member already exists
            val existingMember = familyMemberRepository.findByOwnerAndMemberName(entity.owner, entity.memberName)
            if (existingMember != null) {
                throw DataIntegrityViolationException("Family member already exists for owner='${entity.owner}', name='${entity.memberName}'")
            }

            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }

            // Set timestamps
            val timestamp = Timestamp(System.currentTimeMillis())
            entity.dateAdded = timestamp
            entity.dateUpdated = timestamp

            familyMemberRepository.save(entity)
        }
    }

    override fun update(entity: FamilyMember): ServiceResult<FamilyMember> {
        return handleServiceOperation("update", entity.familyMemberId) {
            val existingMember = familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(entity.familyMemberId!!)
            if (existingMember == null) {
                throw jakarta.persistence.EntityNotFoundException("FamilyMember not found: ${entity.familyMemberId}")
            }

            // Update timestamp
            entity.dateUpdated = Timestamp(System.currentTimeMillis())

            familyMemberRepository.save(entity)
        }
    }

    override fun deleteById(id: Long): ServiceResult<Boolean> {
        return handleServiceOperation("deleteById", id) {
            val member = familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(id)
            if (member == null) {
                throw jakarta.persistence.EntityNotFoundException("FamilyMember not found: $id")
            }
            val rowsAffected = familyMemberRepository.softDeleteByFamilyMemberId(id)
            rowsAffected > 0
        }
    }

    // ===== Additional Business Logic Methods =====

    private fun findByOwnerServiceResult(owner: String): ServiceResult<List<FamilyMember>> {
        return handleServiceOperation("findByOwner", null) {
            familyMemberRepository.findByOwnerAndActiveStatusTrue(owner)
        }
    }

    private fun findByOwnerAndRelationshipServiceResult(owner: String, relationship: FamilyRelationship): ServiceResult<List<FamilyMember>> {
        return handleServiceOperation("findByOwnerAndRelationship", null) {
            familyMemberRepository.findByOwnerAndRelationshipAndActiveStatusTrue(owner, relationship)
        }
    }

    private fun updateActiveStatusServiceResult(id: Long, active: Boolean): ServiceResult<Boolean> {
        return handleServiceOperation("updateActiveStatus", id) {
            val member = familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(id)
            if (member == null) {
                throw jakarta.persistence.EntityNotFoundException("FamilyMember not found: $id")
            }
            val rowsAffected = familyMemberRepository.updateActiveStatus(id, active)
            rowsAffected > 0
        }
    }

    // ===== Legacy Method Compatibility =====

    fun insertFamilyMember(member: FamilyMember): FamilyMember {
        val result = save(member)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.ValidationError -> {
                val violations = result.errors.map { (field, message) ->
                    object : jakarta.validation.ConstraintViolation<FamilyMember> {
                        override fun getMessage(): String = message
                        override fun getMessageTemplate(): String = message
                        override fun getRootBean(): FamilyMember = member
                        override fun getRootBeanClass(): Class<FamilyMember> = FamilyMember::class.java
                        override fun getLeafBean(): Any = member
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
                if (result.message.contains("already exists")) {
                    throw DataIntegrityViolationException("Family member already exists for owner='${member.owner}', name='${member.memberName}'")
                } else {
                    throw RuntimeException("Failed to insert family member: ${result}")
                }
            }
            else -> throw RuntimeException("Failed to insert family member: ${result}")
        }
    }

    fun updateFamilyMember(member: FamilyMember): FamilyMember {
        val result = update(member)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> throw IllegalArgumentException("Family member not found: ${member.familyMemberId}")
            else -> throw RuntimeException("Failed to update family member: ${result}")
        }
    }

    fun findByIdLegacy(id: Long): FamilyMember? {
        val result = findById(id)
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> null
        }
    }

    fun findByOwner(owner: String): List<FamilyMember> {
        val result = findByOwnerServiceResult(owner)
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    fun findByOwnerAndRelationship(owner: String, relationship: FamilyRelationship): List<FamilyMember> {
        val result = findByOwnerAndRelationshipServiceResult(owner, relationship)
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    fun findAll(): List<FamilyMember> {
        val result = findAllActive()
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    fun updateActiveStatus(id: Long, active: Boolean): Boolean {
        val result = updateActiveStatusServiceResult(id, active)
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> false
        }
    }

    fun softDelete(id: Long): Boolean {
        val result = deleteById(id)
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> false
        }
    }
}