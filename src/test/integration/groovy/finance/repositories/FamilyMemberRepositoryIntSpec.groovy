package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.helpers.SmartFamilyMemberBuilder
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.sql.Date

class FamilyMemberRepositoryIntSpec extends BaseIntegrationSpec {

    @Autowired
    FamilyMemberRepository familyMemberRepository

    @Shared
    String owner

    def setupSpec() {
        owner = testOwner
    }

    void 'family member basic CRUD and finders'() {
        given:
        FamilyMember member = SmartFamilyMemberBuilder.builderForOwner(owner)
                .withMemberName("primary_${owner.replaceAll(/[^a-z]/,'')}")
                .asRelationship(FamilyRelationship.Self)
                .asActive()
                .build()

        when:
        FamilyMember saved = familyMemberRepository.save(member)

        then:
        saved.familyMemberId != null
        saved.owner == owner
        saved.memberName.startsWith("primary_")
        saved.activeStatus == true

        when:
        def byOwner = familyMemberRepository.findByOwnerAndActiveStatusTrue(owner)
        def byRel = familyMemberRepository.findByOwnerAndRelationshipAndActiveStatusTrue(owner, FamilyRelationship.Self)
        def byId = familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(saved.familyMemberId)

        then:
        byOwner.any { it.familyMemberId == saved.familyMemberId }
        byRel.any { it.familyMemberId == saved.familyMemberId }
        byId != null && byId.familyMemberId == saved.familyMemberId
    }

    void 'update active status and soft delete'() {
        given:
        FamilyMember member = SmartFamilyMemberBuilder.builderForOwner(owner)
                .withMemberName("dependent_${owner.replaceAll(/[^a-z]/,'')}")
                .asRelationship(FamilyRelationship.Child)
                .asActive()
                .build()
        FamilyMember saved = familyMemberRepository.save(member)

        when: 'toggle active status via custom update'
        int updated = familyMemberRepository.updateActiveStatus(saved.familyMemberId, false)
        def refreshed = familyMemberRepository.findById(saved.familyMemberId).get()

        then:
        updated == 1
        refreshed.activeStatus == false

        when: 'soft delete the record'
        int deleted = familyMemberRepository.softDeleteByFamilyMemberId(saved.familyMemberId)
        def afterDelete = familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(saved.familyMemberId)

        then:
        deleted == 1
        afterDelete == null
    }

    void 'find by owner and member name returns single match'() {
        given:
        String memberName = "member_${owner.replaceAll(/[^a-z]/,'')}"
        FamilyMember member = SmartFamilyMemberBuilder.builderForOwner(owner)
                .withMemberName(memberName)
                .asRelationship(FamilyRelationship.Spouse)
                .asActive()
                .build()
        familyMemberRepository.save(member)

        when:
        def found = familyMemberRepository.findByOwnerAndMemberName(owner, memberName)

        then:
        found != null
        found.memberName == memberName
        found.owner == owner
    }

    void 'find all active family members for owner'() {
        given:
        def active1 = familyMemberRepository.save(
                SmartFamilyMemberBuilder.builderForOwner(owner)
                        .withMemberName("active1_${owner.replaceAll(/[^a-z]/,'')}")
                        .asActive()
                        .build()
        )
        def active2 = familyMemberRepository.save(
                SmartFamilyMemberBuilder.builderForOwner(owner)
                        .withMemberName("active2_${owner.replaceAll(/[^a-z]/,'')}")
                        .asActive()
                        .build()
        )
        def inactive = familyMemberRepository.save(
                SmartFamilyMemberBuilder.builderForOwner(owner)
                        .withMemberName("inactive_${owner.replaceAll(/[^a-z]/,'')}")
                        .asInactive()
                        .build()
        )

        when:
        def actives = familyMemberRepository.findByActiveStatusTrue()

        then:
        actives.any { it.familyMemberId == active1.familyMemberId }
        actives.any { it.familyMemberId == active2.familyMemberId }
        !actives.any { it.familyMemberId == inactive.familyMemberId }
    }
}
