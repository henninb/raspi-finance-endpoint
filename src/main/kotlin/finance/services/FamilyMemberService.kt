package finance.services

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.repositories.FamilyMemberRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
open class FamilyMemberService(
    private val familyMemberRepository: FamilyMemberRepository
) : IFamilyMemberService, BaseService() {

    private val logger = LoggerFactory.getLogger(FamilyMemberService::class.java)

    override fun insertFamilyMember(member: FamilyMember): FamilyMember {
        try {
            // Enforce uniqueness on (owner, memberName) at service layer for a clearer error
            val existing = familyMemberRepository.findByOwnerAndMemberName(member.owner, member.memberName)
            if (existing != null) {
                throw DataIntegrityViolationException("Family member already exists for owner='${member.owner}', name='${member.memberName}'")
            }
            return familyMemberRepository.save(member)
        } catch (e: Exception) {
            logger.error("Failed to insert family member for owner='${member.owner}'", e)
            throw e
        }
    }

    override fun findById(id: Long): FamilyMember? =
        familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(id)

    override fun findByOwner(owner: String): List<FamilyMember> =
        familyMemberRepository.findByOwnerAndActiveStatusTrue(owner)

    override fun findByOwnerAndRelationship(owner: String, relationship: FamilyRelationship): List<FamilyMember> =
        familyMemberRepository.findByOwnerAndRelationshipAndActiveStatusTrue(owner, relationship)

    override fun updateActiveStatus(id: Long, active: Boolean): Boolean =
        familyMemberRepository.updateActiveStatus(id, active) > 0

    override fun softDelete(id: Long): Boolean =
        familyMemberRepository.softDeleteByFamilyMemberId(id) > 0
}

