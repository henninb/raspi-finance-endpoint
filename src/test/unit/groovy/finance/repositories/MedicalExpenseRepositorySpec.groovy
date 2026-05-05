package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

class MedicalExpenseRepositorySpec extends Specification {

    def "extends JpaRepository and is interface"() {
        expect:
        MedicalExpenseRepository.interfaces.any { it == JpaRepository }
        MedicalExpenseRepository.isInterface()
        MedicalExpenseRepository.name == 'finance.repositories.MedicalExpenseRepository'
    }

    def "declares all expected legacy methods"() {
        when:
        def names = MedicalExpenseRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByActiveStatusTrueOrderByServiceDateDesc',
            'findByTransactionId',
            'findByMedicalExpenseIdAndActiveStatusTrue',
            'findByServiceDateBetween',
            'findByServiceDateBetweenAndActiveStatusTrue',
            'findByProviderId',
            'findByProviderIdAndActiveStatusTrue',
            'findByFamilyMemberId',
            'findByFamilyMemberIdAndActiveStatusTrue',
            'findByClaimStatus',
            'findByClaimStatusAndActiveStatusTrue',
            'findByIsOutOfNetwork',
            'findByIsOutOfNetworkAndActiveStatusTrue',
            'findByClaimNumber',
            'findByClaimNumberAndActiveStatusTrue',
            'findFullyPaidMedicalExpenses',
            'findMedicalExpensesWithoutTransaction',
            'findOverpaidMedicalExpenses',
            'getTotalPaidAmountByYear',
            'getTotalUnpaidBalance',
            'updatePaidAmount',
            'softDeleteByMedicalExpenseId',
            'updateClaimStatus',
        ])
    }

    def "declares all expected owner-scoped methods"() {
        when:
        def names = MedicalExpenseRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByOwnerAndActiveStatusTrueOrderByServiceDateDesc',
            'findByOwnerAndTransactionId',
            'findByOwnerAndMedicalExpenseIdAndActiveStatusTrue',
            'findByOwnerAndServiceDateBetweenAndActiveStatusTrue',
            'findByOwnerAndProviderIdAndActiveStatusTrue',
            'findByOwnerAndFamilyMemberIdAndActiveStatusTrue',
            'findByOwnerAndClaimStatusAndActiveStatusTrue',
            'findByOwnerAndIsOutOfNetworkAndActiveStatusTrue',
            'findByOwnerAndClaimNumberAndActiveStatusTrue',
            'findOutstandingPatientBalancesByOwner',
            'findActiveOpenClaimsByOwner',
            'softDeleteByOwnerAndMedicalExpenseId',
            'updateClaimStatusByOwner',
            'findUnpaidMedicalExpensesByOwner',
            'findPartiallyPaidMedicalExpensesByOwner',
            'findFullyPaidMedicalExpensesByOwner',
            'findMedicalExpensesWithoutTransactionByOwner',
            'findOverpaidMedicalExpensesByOwner',
            'getTotalPaidAmountByOwnerAndYear',
            'getTotalUnpaidBalanceByOwner',
            'updatePaidAmountByOwner',
        ])
    }

    def "legacy return types are correct"() {
        when:
        def methods = MedicalExpenseRepository.declaredMethods

        then:
        methods.find { it.name == 'findByActiveStatusTrueOrderByServiceDateDesc' }?.returnType == List.class
        (methods.find { it.name == 'findByTransactionId' }?.returnType?.name) == 'finance.domain.MedicalExpense'
        methods.find { it.name == 'getTotalPaidAmountByYear' }?.returnType == java.math.BigDecimal.class
        methods.find { it.name == 'getTotalUnpaidBalance' }?.returnType == java.math.BigDecimal.class
        methods.find { it.name == 'updatePaidAmount' }?.returnType == int.class
        methods.find { it.name == 'softDeleteByMedicalExpenseId' }?.returnType == int.class
        methods.find { it.name == 'updateClaimStatus' }?.returnType == int.class
    }

    def "owner-scoped return types are correct"() {
        when:
        def methods = MedicalExpenseRepository.declaredMethods

        then:
        methods.find { it.name == 'findByOwnerAndActiveStatusTrueOrderByServiceDateDesc' }?.returnType == List.class
        methods.find { it.name == 'getTotalPaidAmountByOwnerAndYear' }?.returnType == java.math.BigDecimal.class
        methods.find { it.name == 'getTotalUnpaidBalanceByOwner' }?.returnType == java.math.BigDecimal.class
        methods.find { it.name == 'updatePaidAmountByOwner' }?.returnType == int.class
        methods.find { it.name == 'softDeleteByOwnerAndMedicalExpenseId' }?.returnType == int.class
        methods.find { it.name == 'updateClaimStatusByOwner' }?.returnType == int.class
    }

    def "updatePaidAmount has proper annotations"() {
        when:
        def method = MedicalExpenseRepository.declaredMethods.find { it.name == 'updatePaidAmount' }

        then:
        method.isAnnotationPresent(Modifying)
        method.isAnnotationPresent(Transactional)
        method.isAnnotationPresent(Query)
        method.getAnnotation(Query).value().contains('paidAmount')
    }

    def "updatePaidAmountByOwner has proper annotations"() {
        when:
        def method = MedicalExpenseRepository.declaredMethods.find { it.name == 'updatePaidAmountByOwner' }

        then:
        method.isAnnotationPresent(Modifying)
        method.isAnnotationPresent(Transactional)
        method.isAnnotationPresent(Query)
        method.getAnnotation(Query).value().contains('paidAmount')
        method.getAnnotation(Query).value().contains('owner')
        method.parameterCount == 3
        method.parameterTypes[0] == String.class
    }

    def "softDelete methods have proper annotations"() {
        when:
        def legacy = MedicalExpenseRepository.declaredMethods.find { it.name == 'softDeleteByMedicalExpenseId' }
        def ownerScoped = MedicalExpenseRepository.declaredMethods.find { it.name == 'softDeleteByOwnerAndMedicalExpenseId' }

        then:
        [legacy, ownerScoped].each { m ->
            assert m.isAnnotationPresent(Modifying)
            assert m.isAnnotationPresent(Transactional)
            assert m.isAnnotationPresent(Query)
            assert m.getAnnotation(Query).value().contains('activeStatus')
        }
    }
}
