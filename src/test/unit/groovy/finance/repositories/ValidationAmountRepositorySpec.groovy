package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

class ValidationAmountRepositorySpec extends Specification {

    def "extends JpaRepository and is interface"() {
        expect:
        ValidationAmountRepository.interfaces.any { it == JpaRepository }
        ValidationAmountRepository.isInterface()
        ValidationAmountRepository.name == 'finance.repositories.ValidationAmountRepository'
    }

    def "declares all expected methods"() {
        when:
        def names = ValidationAmountRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByTransactionStateAndAccountId',
            'findByAccountId',
            'findByActiveStatusTrueOrderByValidationDateDesc',
            'findByValidationIdAndActiveStatusTrue',
            'findByOwnerAndTransactionStateAndAccountId',
            'findByOwnerAndAccountId',
            'findByOwnerAndActiveStatusTrueOrderByValidationDateDesc',
            'findByOwnerAndValidationIdAndActiveStatusTrue',
            'deleteByOwnerAndAccountId',
        ])
    }

    def "legacy methods have correct return types"() {
        when:
        def methods = ValidationAmountRepository.declaredMethods

        then:
        methods.find { it.name == 'findByTransactionStateAndAccountId' }?.returnType == List.class
        methods.find { it.name == 'findByAccountId' }?.returnType == List.class
        methods.find { it.name == 'findByActiveStatusTrueOrderByValidationDateDesc' }?.returnType == List.class
        methods.find { it.name == 'findByValidationIdAndActiveStatusTrue' }?.returnType == Optional.class
    }

    def "owner-scoped find methods have correct return types"() {
        when:
        def methods = ValidationAmountRepository.declaredMethods

        then:
        methods.find { it.name == 'findByOwnerAndTransactionStateAndAccountId' }?.returnType == List.class
        methods.find { it.name == 'findByOwnerAndAccountId' }?.returnType == List.class
        methods.find { it.name == 'findByOwnerAndActiveStatusTrueOrderByValidationDateDesc' }?.returnType == List.class
        methods.find { it.name == 'findByOwnerAndValidationIdAndActiveStatusTrue' }?.returnType == Optional.class
    }

    def "deleteByOwnerAndAccountId has proper annotations and return type"() {
        when:
        def method = ValidationAmountRepository.declaredMethods.find { it.name == 'deleteByOwnerAndAccountId' }

        then:
        method != null
        method.returnType == int.class
        method.isAnnotationPresent(Modifying)
        method.isAnnotationPresent(Transactional)
        method.isAnnotationPresent(Query)

        def queryValue = method.getAnnotation(Query).value()
        queryValue.contains('DELETE FROM ValidationAmount')
        queryValue.contains('owner')
        queryValue.contains('accountId')

        method.parameterCount == 2
        method.parameterTypes[0] == String.class
        method.parameterTypes[1] == long.class
    }
}

