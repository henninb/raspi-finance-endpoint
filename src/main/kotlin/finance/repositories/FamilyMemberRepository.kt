package finance.repositories

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface FamilyMemberRepository : JpaRepository<FamilyMember, Long> {
    fun findByFamilyMemberIdAndActiveStatusTrue(familyMemberId: Long): FamilyMember?

    fun findByOwnerAndActiveStatusTrue(owner: String): List<FamilyMember>

    fun findByOwnerAndRelationshipAndActiveStatusTrue(
        owner: String,
        relationship: FamilyRelationship,
    ): List<FamilyMember>

    fun findByActiveStatusTrue(): List<FamilyMember>

    fun findByOwnerAndMemberName(
        owner: String,
        memberName: String,
    ): FamilyMember?

    fun findByOwnerAndFamilyMemberIdAndActiveStatusTrue(
        owner: String,
        familyMemberId: Long,
    ): FamilyMember?

    fun findByOwnerAndFamilyMemberId(
        owner: String,
        familyMemberId: Long,
    ): FamilyMember?

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        """
        UPDATE FamilyMember f
        SET f.activeStatus = false, f.dateUpdated = CURRENT_TIMESTAMP
        WHERE f.familyMemberId = :familyMemberId
        """,
    )
    fun softDeleteByFamilyMemberId(
        @Param("familyMemberId") familyMemberId: Long,
    ): Int

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        """
        UPDATE FamilyMember f
        SET f.activeStatus = false, f.dateUpdated = CURRENT_TIMESTAMP
        WHERE f.familyMemberId = :familyMemberId AND f.owner = :owner
        """,
    )
    fun softDeleteByOwnerAndFamilyMemberId(
        @Param("owner") owner: String,
        @Param("familyMemberId") familyMemberId: Long,
    ): Int

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        """
        UPDATE FamilyMember f
        SET f.activeStatus = :active, f.dateUpdated = CURRENT_TIMESTAMP
        WHERE f.familyMemberId = :familyMemberId
        """,
    )
    fun updateActiveStatus(
        @Param("familyMemberId") familyMemberId: Long,
        @Param("active") active: Boolean,
    ): Int

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        """
        UPDATE FamilyMember f
        SET f.activeStatus = :active, f.dateUpdated = CURRENT_TIMESTAMP
        WHERE f.familyMemberId = :familyMemberId AND f.owner = :owner
        """,
    )
    fun updateActiveStatusByOwner(
        @Param("owner") owner: String,
        @Param("familyMemberId") familyMemberId: Long,
        @Param("active") active: Boolean,
    ): Int
}
