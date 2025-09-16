package finance.services

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.helpers.FamilyMemberBuilder
import finance.repositories.FamilyMemberRepository
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

class FamilyMemberServiceSpec extends Specification {

    FamilyMemberRepository familyMemberRepository = GroovyMock(FamilyMemberRepository)
    FamilyMemberService familyMemberService = new FamilyMemberService(familyMemberRepository)

    def "insertFamilyMember - success"() {
        given:
        def member = FamilyMemberBuilder.builder().withOwner('owner_a').withMemberName('alice').withRelationship(FamilyRelationship.Child).build()
        def saved = FamilyMemberBuilder.builder().withOwner('owner_a').withMemberName('alice').withRelationship(FamilyRelationship.Child).withFamilyMemberId(10L).build()

        when:
        def result = familyMemberService.insertFamilyMember(member)

        then:
        1 * familyMemberRepository.findByOwnerAndMemberName('owner_a', 'alice') >> null
        1 * familyMemberRepository.save(member) >> saved
        result.familyMemberId == 10L
        result.owner == 'owner_a'
        result.memberName == 'alice'
    }

    def "insertFamilyMember - duplicate throws DataIntegrityViolationException"() {
        given:
        def existing = FamilyMemberBuilder.builder().withOwner('owner_b').withMemberName('bob').build()
        def member = FamilyMemberBuilder.builder().withOwner('owner_b').withMemberName('bob').build()

        when:
        familyMemberService.insertFamilyMember(member)

        then:
        1 * familyMemberRepository.findByOwnerAndMemberName('owner_b', 'bob') >> existing
        0 * familyMemberRepository.save(_ as FamilyMember)
        thrown(DataIntegrityViolationException)
    }

    def "findById returns active member"() {
        given:
        def member = FamilyMemberBuilder.builder().withFamilyMemberId(5L).build()

        when:
        def result = familyMemberService.findById(5L)

        then:
        1 * familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(5L) >> member
        result.familyMemberId == 5L
    }

    def "findByOwner returns list"() {
        given:
        def m1 = FamilyMemberBuilder.builder().withOwner('own').withMemberName('a').build()
        def m2 = FamilyMemberBuilder.builder().withOwner('own').withMemberName('b').build()

        when:
        def result = familyMemberService.findByOwner('own')

        then:
        1 * familyMemberRepository.findByOwnerAndActiveStatusTrue('own') >> [m1, m2]
        result*.memberName == ['a','b']
    }

    def "findByOwnerAndRelationship returns filtered list"() {
        given:
        def m1 = FamilyMemberBuilder.builder().withOwner('own').withMemberName('kid').withRelationship(FamilyRelationship.Child).build()

        when:
        def result = familyMemberService.findByOwnerAndRelationship('own', FamilyRelationship.Child)

        then:
        1 * familyMemberRepository.findByOwnerAndRelationshipAndActiveStatusTrue('own', FamilyRelationship.Child) >> [m1]
        result.size() == 1
        result[0].relationship == FamilyRelationship.Child
    }

    def "findAll returns active members"() {
        given:
        def m1 = FamilyMemberBuilder.builder().build()

        when:
        def result = familyMemberService.findAll()

        then:
        1 * familyMemberRepository.findByActiveStatusTrue() >> [m1]
        result.size() == 1
    }

    def "updateActiveStatus returns true when updated"() {
        when:
        def result = familyMemberService.updateActiveStatus(7L, false)

        then:
        1 * familyMemberRepository.updateActiveStatus(7L, false) >> 1
        result
    }

    def "updateActiveStatus returns false when no rows updated"() {
        when:
        def result = familyMemberService.updateActiveStatus(8L, true)

        then:
        1 * familyMemberRepository.updateActiveStatus(8L, true) >> 0
        !result
    }

    def "softDelete returns true when updated"() {
        when:
        def result = familyMemberService.softDelete(9L)

        then:
        1 * familyMemberRepository.softDeleteByFamilyMemberId(9L) >> 1
        result
    }

    def "softDelete returns false when no rows updated"() {
        when:
        def result = familyMemberService.softDelete(11L)

        then:
        1 * familyMemberRepository.softDeleteByFamilyMemberId(11L) >> 0
        !result
    }

    def "updateFamilyMember - success"() {
        given:
        def existing = FamilyMemberBuilder.builder().withFamilyMemberId(12L).withOwner('own').withMemberName('x').build()
        def patch = FamilyMemberBuilder.builder().withFamilyMemberId(12L).withOwner('own').withMemberName('y').build()

        when:
        def result = familyMemberService.updateFamilyMember(patch)

        then:
        1 * familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(12L) >> existing
        1 * familyMemberRepository.save(patch) >> patch
        result.memberName == 'y'
    }

    def "updateFamilyMember - not found throws IllegalArgumentException"() {
        given:
        def patch = FamilyMemberBuilder.builder().withFamilyMemberId(13L).build()

        when:
        familyMemberService.updateFamilyMember(patch)

        then:
        1 * familyMemberRepository.findByFamilyMemberIdAndActiveStatusTrue(13L) >> null
        thrown(IllegalArgumentException)
        0 * familyMemberRepository.save(_ as FamilyMember)
    }
}

