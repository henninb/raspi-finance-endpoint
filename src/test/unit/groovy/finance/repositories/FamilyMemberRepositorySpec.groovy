package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

class FamilyMemberRepositorySpec extends Specification {
    def "extends JpaRepository and is interface"() {
        expect:
        FamilyMemberRepository.interfaces.any { it == JpaRepository }
        FamilyMemberRepository.isInterface()
        FamilyMemberRepository.name == 'finance.repositories.FamilyMemberRepository'
    }

    def "declares expected query methods"() {
        when:
        def names = FamilyMemberRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByFamilyMemberIdAndActiveStatusTrue',
            'findByOwnerAndActiveStatusTrue',
            'findByOwnerAndRelationshipAndActiveStatusTrue',
            'findByActiveStatusTrue',
            'findByOwnerAndMemberName',
            'softDeleteByFamilyMemberId',
            'updateActiveStatus',
        ])
    }

    def "softDelete and updateActiveStatus have proper annotations"() {
        when:
        def softDelete = FamilyMemberRepository.declaredMethods.find { it.name == 'softDeleteByFamilyMemberId' }
        def updateActive = FamilyMemberRepository.declaredMethods.find { it.name == 'updateActiveStatus' }

        then:
        softDelete != null && updateActive != null
        softDelete.isAnnotationPresent(Modifying)
        softDelete.isAnnotationPresent(Transactional)
        softDelete.isAnnotationPresent(Query)
        updateActive.isAnnotationPresent(Modifying)
        updateActive.isAnnotationPresent(Transactional)
        updateActive.isAnnotationPresent(Query)
        updateActive.returnType == int.class
    }
}

