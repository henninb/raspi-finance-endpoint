package finance.services

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship

interface IFamilyMemberService {
    fun insertFamilyMember(member: FamilyMember): FamilyMember
    fun updateFamilyMember(member: FamilyMember): FamilyMember
    fun findById(id: Long): FamilyMember?
    fun findByOwner(owner: String): List<FamilyMember>
    fun findByOwnerAndRelationship(owner: String, relationship: FamilyRelationship): List<FamilyMember>
    fun findAll(): List<FamilyMember>
    fun updateActiveStatus(id: Long, active: Boolean): Boolean
    fun softDelete(id: Long): Boolean
}

