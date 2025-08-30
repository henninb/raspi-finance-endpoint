package finance.repositories

import finance.Application
import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.helpers.SmartFamilyMemberBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import jakarta.validation.ConstraintViolationException

@ActiveProfiles("func")
@DataJpaTest
@ContextConfiguration(classes = [Application])
class FamilyMemberJpaSpec extends Specification {

    @Autowired
    protected FamilyMemberRepository familyMemberRepository

    @Autowired
    protected TestEntityManager entityManager

    @Shared
    protected String testOwner = "jpa_${UUID.randomUUID().toString().replace('-', '')[0..7]}"

    void 'test FamilyMember - valid insert'() {
        given:
        long beforeFamilyMember = familyMemberRepository.count()
        FamilyMember familyMember = SmartFamilyMemberBuilder.builderForOwner(testOwner)
            .withMemberName("john smith")
            .withDateOfBirth("1985-03-15")
            .asRelationship(FamilyRelationship.Spouse)
            .asActive()
            .build()

        when:
        FamilyMember result = entityManager.persist(familyMember)

        then:
        familyMemberRepository.count() == beforeFamilyMember + 1
        result.owner == testOwner
        result.memberName == "john smith"
        result.relationship == FamilyRelationship.Spouse
        result.activeStatus == true
    }

    void 'test FamilyMember - different relationships'() {
        given:
        FamilyMember spouse = SmartFamilyMemberBuilder.builderForOwner(testOwner)
            .withMemberName("jane smith")
            .asRelationship(FamilyRelationship.Spouse)
            .build()

        FamilyMember child = SmartFamilyMemberBuilder.builderForOwner(testOwner)
            .withMemberName("emily smith")
            .asRelationship(FamilyRelationship.Child)
            .build()

        FamilyMember other = SmartFamilyMemberBuilder.builderForOwner(testOwner)
            .withMemberName("robert smith")
            .asRelationship(FamilyRelationship.Other)
            .build()

        when:
        FamilyMember spouseResult = entityManager.persist(spouse)
        FamilyMember childResult = entityManager.persist(child)
        FamilyMember otherResult = entityManager.persist(other)

        then:
        spouseResult.relationship == FamilyRelationship.Spouse
        childResult.relationship == FamilyRelationship.Child
        otherResult.relationship == FamilyRelationship.Other
        spouseResult.memberName == "jane smith"
        childResult.memberName == "emily smith"
        otherResult.memberName == "robert smith"
    }

    void 'test FamilyMember - different active status'() {
        given:
        FamilyMember activeMember = SmartFamilyMemberBuilder.builderForOwner(testOwner)
            .withMemberName("active member")
            .asActive()
            .build()

        FamilyMember inactiveMember = SmartFamilyMemberBuilder.builderForOwner(testOwner)
            .withMemberName("inactive member")
            .asInactive()
            .build()

        when:
        FamilyMember activeResult = entityManager.persist(activeMember)
        FamilyMember inactiveResult = entityManager.persist(inactiveMember)

        then:
        activeResult.activeStatus == true
        inactiveResult.activeStatus == false
        activeResult.memberName == "active member"
        inactiveResult.memberName == "inactive member"
    }

    void 'test FamilyMember - find by owner'() {
        given:
        String uniqueOwner = "owner_${testOwner}"
        FamilyMember member1 = SmartFamilyMemberBuilder.builderForOwner(uniqueOwner)
            .withMemberName("find test1")
            .asRelationship(FamilyRelationship.Spouse)
            .build()
        FamilyMember member2 = SmartFamilyMemberBuilder.builderForOwner(uniqueOwner)
            .withMemberName("find test2")
            .asRelationship(FamilyRelationship.Child)
            .build()
        entityManager.persist(member1)
        entityManager.persist(member2)

        when:
        List<FamilyMember> members = familyMemberRepository.findByOwnerAndActiveStatusTrue(uniqueOwner)

        then:
        members.size() >= 2
        members.any { it.memberName == "find test1" }
        members.any { it.memberName == "find test2" }
        members.every { it.owner == uniqueOwner }
    }

    void 'test FamilyMember - delete record'() {
        given:
        long beforeFamilyMember = familyMemberRepository.count()
        FamilyMember familyMember = SmartFamilyMemberBuilder.builderForOwner(testOwner)
            .withMemberName("delete test")
            .build()
        entityManager.persist(familyMember)

        when:
        familyMemberRepository.delete(familyMember)

        then:
        familyMemberRepository.count() == beforeFamilyMember
    }

    void 'test FamilyMember - invalid empty member name'() {
        given:
        FamilyMember familyMember = SmartFamilyMemberBuilder.builderForOwner(testOwner)
            .withMemberName("")
            .build()

        when:
        entityManager.persist(familyMember)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.toLowerCase().contains('member name') || ex.message.toLowerCase().contains('between 1 and 100')
        0 * _
    }

    void 'test FamilyMember - invalid ssn last four format'() {
        given:
        FamilyMember familyMember = SmartFamilyMemberBuilder.builderForOwner(testOwner)
            .withMemberName("invalid ssn")
            .withSsnLastFour("12a4") // not 4 digits
            .build()

        when:
        entityManager.persist(familyMember)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.toLowerCase().contains('ssn last four') || ex.message.toLowerCase().contains('4 digits')
        0 * _
    }
}
