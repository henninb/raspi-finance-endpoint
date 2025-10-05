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

    def "declares expected query methods subset and return types"() {
        when:
        def methods = MedicalExpenseRepository.declaredMethods
        def names = methods*.name as Set

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
        ])

        (methods.find { it.name == 'findByTransactionId' }?.returnType?.name) == 'finance.domain.MedicalExpense'
        methods.find { it.name == 'getTotalPaidAmountByYear' }?.returnType == java.math.BigDecimal.class
        methods.find { it.name == 'getTotalUnpaidBalance' }?.returnType == java.math.BigDecimal.class
        methods.find { it.name == 'updatePaidAmount' }?.returnType == int.class
    }

    def "updatePaidAmount has proper annotations"() {
        when:
        def method = MedicalExpenseRepository.declaredMethods.find { it.name == 'updatePaidAmount' }

        then:
        method.isAnnotationPresent(Modifying)
        method.isAnnotationPresent(Transactional)
        method.isAnnotationPresent(Query)
    }
}
