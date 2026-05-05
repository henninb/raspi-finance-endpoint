package finance.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import jakarta.transaction.Transactional
import spock.lang.Specification

class TransactionRepositorySpec extends Specification {

    def "extends JpaRepository and is interface"() {
        expect:
        TransactionRepository.interfaces.any { it == JpaRepository }
        TransactionRepository.isInterface()
        TransactionRepository.name == 'finance.repositories.TransactionRepository'
    }

    def "declares all expected legacy methods"() {
        when:
        def names = TransactionRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByGuid',
            'findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc',
            'findByCategoryAndActiveStatusOrderByTransactionDateDesc',
            'findByDescriptionAndActiveStatusOrderByTransactionDateDesc',
            'countByDescriptionName',
            'countByDescriptionNameIn',
            'countByCategoryName',
            'countByCategoryNameIn',
            'sumTotalsForActiveTransactionsByAccountNameOwner',
            'findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc',
            'findByTransactionDateBetween',
            'findByActiveStatus',
            'findByAccountNameOwnerAndActiveStatus',
            'findByCategoryAndActiveStatus',
            'findByDescriptionAndActiveStatus',
            'deactivateAllTransactionsByAccountNameOwner',
            'updateAccountNameOwnerForAllTransactions',
        ])
    }

    def "declares all expected owner-scoped methods"() {
        when:
        def names = TransactionRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByOwnerAndGuid',
            'findByOwnerAndAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc',
            'findByOwnerAndCategoryAndActiveStatusOrderByTransactionDateDesc',
            'findByOwnerAndDescriptionAndActiveStatusOrderByTransactionDateDesc',
            'countByOwnerAndDescriptionName',
            'countByOwnerAndDescriptionNameIn',
            'countByOwnerAndCategoryName',
            'countByOwnerAndCategoryNameIn',
            'sumTotalsForActiveTransactionsByOwnerAndAccountNameOwner',
            'findByOwnerAndAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc',
            'findByOwnerAndTransactionDateBetween',
            'findByOwnerAndActiveStatus',
            'findByOwnerAndAccountNameOwnerAndActiveStatus',
            'findByOwnerAndCategoryAndActiveStatus',
            'findByOwnerAndDescriptionAndActiveStatus',
            'deactivateAllTransactionsByOwnerAndAccountNameOwner',
            'updateAccountNameOwnerForAllTransactionsByOwner',
            'bulkUpdateCategoryByOwner',
            'bulkUpdateDescriptionByOwner',
        ])
    }

    def "legacy return types are correct"() {
        when:
        def methods = TransactionRepository.declaredMethods

        then:
        methods.find { it.name == 'findByGuid' }?.returnType == Optional.class
        methods.find { it.name == 'findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc' }?.returnType == List.class
        methods.find { it.name == 'countByDescriptionName' }?.returnType == long.class
        methods.find { it.name == 'countByCategoryName' }?.returnType == long.class
        methods.find { it.name == 'countByDescriptionNameIn' }?.returnType == List.class
        methods.find { it.name == 'countByCategoryNameIn' }?.returnType == List.class
        methods.find { it.name == 'sumTotalsForActiveTransactionsByAccountNameOwner' }?.returnType == List.class
        methods.find { it.name == 'findByTransactionDateBetween' }?.returnType == Page.class
        methods.find { it.name == 'findByActiveStatus' }?.returnType == Page.class
        methods.find { it.name == 'deactivateAllTransactionsByAccountNameOwner' }?.returnType == int.class
        methods.find { it.name == 'updateAccountNameOwnerForAllTransactions' }?.returnType == int.class
    }

    def "owner-scoped return types are correct"() {
        when:
        def methods = TransactionRepository.declaredMethods

        then:
        methods.find { it.name == 'findByOwnerAndGuid' }?.returnType == Optional.class
        methods.find { it.name == 'findByOwnerAndAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc' }?.returnType == List.class
        methods.find { it.name == 'countByOwnerAndDescriptionName' }?.returnType == long.class
        methods.find { it.name == 'countByOwnerAndCategoryName' }?.returnType == long.class
        methods.find { it.name == 'findByOwnerAndTransactionDateBetween' }?.returnType == Page.class
        methods.find { it.name == 'findByOwnerAndActiveStatus' }?.returnType == Page.class
        methods.find { it.name == 'deactivateAllTransactionsByOwnerAndAccountNameOwner' }?.returnType == int.class
        methods.find { it.name == 'updateAccountNameOwnerForAllTransactionsByOwner' }?.returnType == int.class
        methods.find { it.name == 'bulkUpdateCategoryByOwner' }?.returnType == int.class
        methods.find { it.name == 'bulkUpdateDescriptionByOwner' }?.returnType == int.class
    }

    def "count methods have @Query annotation"() {
        when:
        def methods = TransactionRepository.declaredMethods

        then:
        methods.find { it.name == 'countByDescriptionName' }?.isAnnotationPresent(Query)
        methods.find { it.name == 'countByCategoryName' }?.isAnnotationPresent(Query)
        methods.find { it.name == 'countByOwnerAndDescriptionName' }?.isAnnotationPresent(Query)
        methods.find { it.name == 'countByOwnerAndCategoryName' }?.isAnnotationPresent(Query)
    }

    def "bulk update methods have @Modifying and @Transactional annotations"() {
        when:
        def methods = TransactionRepository.declaredMethods
        def bulkMethods = ['deactivateAllTransactionsByAccountNameOwner',
                           'updateAccountNameOwnerForAllTransactions',
                           'deactivateAllTransactionsByOwnerAndAccountNameOwner',
                           'updateAccountNameOwnerForAllTransactionsByOwner',
                           'bulkUpdateCategoryByOwner',
                           'bulkUpdateDescriptionByOwner']

        then:
        bulkMethods.each { name ->
            def m = methods.find { it.name == name }
            assert m != null
            assert m.isAnnotationPresent(Modifying)
            assert m.isAnnotationPresent(Transactional)
            assert m.isAnnotationPresent(Query)
        }
    }

    def "paginated methods accept Pageable parameter"() {
        when:
        def paginatedMethods = ['findByTransactionDateBetween', 'findByActiveStatus',
                                'findByAccountNameOwnerAndActiveStatus', 'findByCategoryAndActiveStatus',
                                'findByDescriptionAndActiveStatus', 'findByOwnerAndTransactionDateBetween',
                                'findByOwnerAndActiveStatus', 'findByOwnerAndAccountNameOwnerAndActiveStatus',
                                'findByOwnerAndCategoryAndActiveStatus', 'findByOwnerAndDescriptionAndActiveStatus']

        then:
        paginatedMethods.each { name ->
            def m = TransactionRepository.declaredMethods.find { it.name == name }
            assert m != null
            assert m.parameterTypes.any { it == Pageable.class }
        }
    }
}

