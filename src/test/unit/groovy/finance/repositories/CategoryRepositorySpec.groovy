package finance.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class CategoryRepositorySpec extends Specification {

    def "extends JpaRepository and is interface"() {
        expect:
        CategoryRepository.interfaces.any { it == JpaRepository }
        CategoryRepository.isInterface()
        CategoryRepository.name == 'finance.repositories.CategoryRepository'
    }

    def "declares all expected query methods"() {
        when:
        def names = CategoryRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByCategoryName',
            'findByCategoryId',
            'findByActiveStatusOrderByCategoryName',
            'findAllByActiveStatusOrderByCategoryName',
            'findByOwnerAndCategoryName',
            'findByOwnerAndCategoryId',
            'findByOwnerAndActiveStatusOrderByCategoryName',
            'findAllByOwnerAndActiveStatusOrderByCategoryName',
        ])
    }

    def "legacy methods have correct return types"() {
        when:
        def methods = CategoryRepository.declaredMethods

        then:
        methods.find { it.name == 'findByCategoryName' }?.returnType == Optional.class
        methods.find { it.name == 'findByCategoryId' }?.returnType == Optional.class
        methods.find { it.name == 'findByActiveStatusOrderByCategoryName' }?.returnType == List.class
        methods.find { it.name == 'findAllByActiveStatusOrderByCategoryName' }?.returnType == Page.class
    }

    def "owner-scoped methods have correct return types"() {
        when:
        def methods = CategoryRepository.declaredMethods

        then:
        methods.find { it.name == 'findByOwnerAndCategoryName' }?.returnType == Optional.class
        methods.find { it.name == 'findByOwnerAndCategoryId' }?.returnType == Optional.class
        methods.find { it.name == 'findByOwnerAndActiveStatusOrderByCategoryName' }?.returnType == List.class
        methods.find { it.name == 'findAllByOwnerAndActiveStatusOrderByCategoryName' }?.returnType == Page.class
    }

    def "paginated methods accept Pageable parameter"() {
        when:
        def paginatedLegacy = CategoryRepository.declaredMethods.find { it.name == 'findAllByActiveStatusOrderByCategoryName' }
        def paginatedOwner = CategoryRepository.declaredMethods.find { it.name == 'findAllByOwnerAndActiveStatusOrderByCategoryName' }

        then:
        paginatedLegacy.parameterTypes.any { it == Pageable.class }
        paginatedOwner.parameterTypes.any { it == Pageable.class }
    }
}

