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

    def "declares all expected methods"() {
        when:
        def names = FamilyMemberRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByFamilyMemberIdAndActiveStatusTrue',
            'findByOwnerAndActiveStatusTrue',
            'findByOwnerAndRelationshipAndActiveStatusTrue',
            'findByActiveStatusTrue',
            'findByOwnerAndMemberName',
            'findByOwnerAndFamilyMemberIdAndActiveStatusTrue',
            'findByOwnerAndFamilyMemberId',
            'softDeleteByFamilyMemberId',
            'softDeleteByOwnerAndFamilyMemberId',
            'updateActiveStatus',
            'updateActiveStatusByOwner',
        ])
    }

    def "find methods return correct types"() {
        when:
        def methods = FamilyMemberRepository.declaredMethods

        then:
        methods.find { it.name == 'findByOwnerAndActiveStatusTrue' }?.returnType == List.class
        methods.find { it.name == 'findByOwnerAndRelationshipAndActiveStatusTrue' }?.returnType == List.class
        methods.find { it.name == 'findByActiveStatusTrue' }?.returnType == List.class
    }

    def "legacy softDelete and updateActiveStatus have proper annotations"() {
        when:
        def softDelete = FamilyMemberRepository.declaredMethods.find { it.name == 'softDeleteByFamilyMemberId' }
        def updateActive = FamilyMemberRepository.declaredMethods.find { it.name == 'updateActiveStatus' }

        then:
        softDelete != null
        softDelete.isAnnotationPresent(Modifying)
        softDelete.isAnnotationPresent(Transactional)
        softDelete.isAnnotationPresent(Query)
        softDelete.returnType == int.class
        softDelete.parameterCount == 1
        softDelete.parameterTypes[0] == long.class

        updateActive != null
        updateActive.isAnnotationPresent(Modifying)
        updateActive.isAnnotationPresent(Transactional)
        updateActive.isAnnotationPresent(Query)
        updateActive.returnType == int.class
        updateActive.parameterCount == 2
        updateActive.parameterTypes[0] == long.class
        updateActive.parameterTypes[1] == boolean.class
    }

    def "owner-scoped softDelete has proper annotations"() {
        when:
        def method = FamilyMemberRepository.declaredMethods.find { it.name == 'softDeleteByOwnerAndFamilyMemberId' }

        then:
        method != null
        method.isAnnotationPresent(Modifying)
        method.isAnnotationPresent(Transactional)
        method.isAnnotationPresent(Query)
        method.returnType == int.class
        method.parameterCount == 2
        method.parameterTypes[0] == String.class
        method.parameterTypes[1] == long.class
    }

    def "owner-scoped updateActiveStatus has proper annotations"() {
        when:
        def method = FamilyMemberRepository.declaredMethods.find { it.name == 'updateActiveStatusByOwner' }

        then:
        method != null
        method.isAnnotationPresent(Modifying)
        method.isAnnotationPresent(Transactional)
        method.isAnnotationPresent(Query)
        method.returnType == int.class
        method.parameterCount == 3
        method.parameterTypes[0] == String.class
        method.parameterTypes[1] == long.class
        method.parameterTypes[2] == boolean.class
    }

    def "JPQL queries for soft delete reference FamilyMember entity"() {
        when:
        def legacy = FamilyMemberRepository.declaredMethods.find { it.name == 'softDeleteByFamilyMemberId' }
        def ownerScoped = FamilyMemberRepository.declaredMethods.find { it.name == 'softDeleteByOwnerAndFamilyMemberId' }

        then:
        legacy.getAnnotation(Query).value().contains('FamilyMember')
        ownerScoped.getAnnotation(Query).value().contains('FamilyMember')
    }
}

