package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class CategoryRepositorySpec extends Specification {
    def "extends JpaRepository and is interface"() {
        expect:
        CategoryRepository.interfaces.any { it == JpaRepository }
        CategoryRepository.isInterface()
        CategoryRepository.name == 'finance.repositories.CategoryRepository'
    }

    def "declares expected query methods and return types"() {
        when:
        def methods = CategoryRepository.declaredMethods

        then:
        (methods*.name as Set).containsAll(['findByCategoryName','findByCategoryId','findByActiveStatusOrderByCategoryName'])
        methods.find { it.name == 'findByCategoryName' }?.returnType == Optional.class
        methods.find { it.name == 'findByCategoryId' }?.returnType == Optional.class
        methods.find { it.name == 'findByActiveStatusOrderByCategoryName' }?.returnType == List.class
    }
}

