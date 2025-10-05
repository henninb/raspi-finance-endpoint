package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class ValidationAmountRepositorySpec extends Specification {
    def "extends JpaRepository and is interface"() {
        expect:
        ValidationAmountRepository.interfaces.any { it == JpaRepository }
        ValidationAmountRepository.isInterface()
        ValidationAmountRepository.name == 'finance.repositories.ValidationAmountRepository'
    }

    def "declares expected query methods and return types"() {
        when:
        def methods = ValidationAmountRepository.declaredMethods
        def names = methods*.name as Set

        then:
        names.containsAll([
            'findByTransactionStateAndAccountId',
            'findByAccountId',
            'findByActiveStatusTrueOrderByValidationDateDesc',
            'findByValidationIdAndActiveStatusTrue',
        ])

        methods.find { it.name == 'findByTransactionStateAndAccountId' }?.returnType == List.class
        methods.find { it.name == 'findByAccountId' }?.returnType == List.class
        methods.find { it.name == 'findByActiveStatusTrueOrderByValidationDateDesc' }?.returnType == List.class
        methods.find { it.name == 'findByValidationIdAndActiveStatusTrue' }?.returnType == Optional.class
    }
}

