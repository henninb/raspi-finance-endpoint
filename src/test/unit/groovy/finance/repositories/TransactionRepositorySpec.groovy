package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import spock.lang.Specification

class TransactionRepositorySpec extends Specification {
    def "extends JpaRepository and is interface"() {
        expect:
        TransactionRepository.interfaces.any { it == JpaRepository }
        TransactionRepository.isInterface()
        TransactionRepository.name == 'finance.repositories.TransactionRepository'
    }

    def "declares expected query methods and return types"() {
        when:
        def methods = TransactionRepository.declaredMethods
        def names = methods*.name as Set

        then:
        names.containsAll([
            'findByGuid',
            'findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc',
            'findByCategoryAndActiveStatusOrderByTransactionDateDesc',
            'findByDescriptionAndActiveStatusOrderByTransactionDateDesc',
            'countByDescriptionName',
            'countByCategoryName',
            'sumTotalsForActiveTransactionsByAccountNameOwner',
            'findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc',
            'findByTransactionDateBetween',
        ])

        methods.find { it.name == 'findByGuid' }?.returnType == Optional.class
        methods.find { it.name == 'countByDescriptionName' }?.isAnnotationPresent(Query)
        methods.find { it.name == 'countByCategoryName' }?.isAnnotationPresent(Query)
        methods.find { it.name == 'sumTotalsForActiveTransactionsByAccountNameOwner' }?.returnType == List.class
    }
}

