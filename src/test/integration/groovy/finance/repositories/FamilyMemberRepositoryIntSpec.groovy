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

    void 'findByOwnerAndFamilyMemberIdAndActiveStatusTrue returns correct member'() {
        given:
        def member = familyMemberRepository.save(
                SmartFamilyMemberBuilder.builderForOwner(owner)
                        .withMemberName("ownerid_${owner.replaceAll(/[^a-z]/,'')}")
                        .asActive()
                        .build()
        )

        when:
        def found = familyMemberRepository.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(owner, member.familyMemberId)
        def wrongOwner = familyMemberRepository.findByOwnerAndFamilyMemberIdAndActiveStatusTrue("wrong-owner", member.familyMemberId)

        then:
        found != null
        found.familyMemberId == member.familyMemberId
        wrongOwner == null
    }

    void 'findByOwnerAndFamilyMemberId returns member regardless of active status'() {
        given:
        def member = familyMemberRepository.save(
                SmartFamilyMemberBuilder.builderForOwner(owner)
                        .withMemberName("anyactive_${owner.replaceAll(/[^a-z]/,'')}")
                        .asInactive()
                        .build()
        )

        when:
        def found = familyMemberRepository.findByOwnerAndFamilyMemberId(owner, member.familyMemberId)

        then:
        found != null
        found.familyMemberId == member.familyMemberId
        found.activeStatus == false
    }

    void 'softDeleteByOwnerAndFamilyMemberId only soft-deletes for correct owner'() {
        given:
        def member = familyMemberRepository.save(
                SmartFamilyMemberBuilder.builderForOwner(owner)
                        .withMemberName("softdel_${owner.replaceAll(/[^a-z]/,'')}")
                        .asActive()
                        .build()
        )

        when:
        int wrongOwnerResult = familyMemberRepository.softDeleteByOwnerAndFamilyMemberId("wrong-owner", member.familyMemberId)

        then:
        wrongOwnerResult == 0
        familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(member.familyMemberId) != null

        when:
        int result = familyMemberRepository.softDeleteByOwnerAndFamilyMemberId(owner, member.familyMemberId)

        then:
        result == 1
        familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(member.familyMemberId) == null
    }

    void 'updateActiveStatusByOwner updates active status for correct owner only'() {
        given:
        def member = familyMemberRepository.save(
                SmartFamilyMemberBuilder.builderForOwner(owner)
                        .withMemberName("updatestatus_${owner.replaceAll(/[^a-z]/,'')}")
                        .asActive()
                        .build()
        )

        when:
        int wrongOwnerResult = familyMemberRepository.updateActiveStatusByOwner("wrong-owner", member.familyMemberId, false)

        then:
        wrongOwnerResult == 0
        familyMemberRepository.findById(member.familyMemberId).get().activeStatus == true

        when:
        int result = familyMemberRepository.updateActiveStatusByOwner(owner, member.familyMemberId, false)

        then:
        result == 1
        familyMemberRepository.findById(member.familyMemberId).get().activeStatus == false
    }
}
