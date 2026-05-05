package finance.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class DescriptionRepositorySpec extends Specification {

    def "extends JpaRepository and is interface"() {
        expect:
        DescriptionRepository.interfaces.any { it == JpaRepository }
        DescriptionRepository.isInterface()
        DescriptionRepository.name == 'finance.repositories.DescriptionRepository'
    }

    def "declares all expected methods"() {
        when:
        def names = DescriptionRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByActiveStatusOrderByDescriptionName',
            'findByDescriptionName',
            'findByDescriptionId',
            'findAllByActiveStatusOrderByDescriptionName',
            'findByOwnerAndDescriptionName',
            'findByOwnerAndDescriptionId',
            'findByOwnerAndActiveStatusOrderByDescriptionName',
            'findAllByOwnerAndActiveStatusOrderByDescriptionName',
        ])
    }

    def "legacy methods have correct return types"() {
        when:
        def methods = DescriptionRepository.declaredMethods

        then:
        methods.find { it.name == 'findByActiveStatusOrderByDescriptionName' }?.returnType == List.class
        methods.find { it.name == 'findByDescriptionName' }?.returnType == Optional.class
        methods.find { it.name == 'findByDescriptionId' }?.returnType == Optional.class
        methods.find { it.name == 'findAllByActiveStatusOrderByDescriptionName' }?.returnType == Page.class
    }

    def "owner-scoped methods have correct return types"() {
        when:
        def methods = DescriptionRepository.declaredMethods

        then:
        methods.find { it.name == 'findByOwnerAndDescriptionName' }?.returnType == Optional.class
        methods.find { it.name == 'findByOwnerAndDescriptionId' }?.returnType == Optional.class
        methods.find { it.name == 'findByOwnerAndActiveStatusOrderByDescriptionName' }?.returnType == List.class
        methods.find { it.name == 'findAllByOwnerAndActiveStatusOrderByDescriptionName' }?.returnType == Page.class
    }

    def "paginated methods accept Pageable parameter"() {
        when:
        def legacy = DescriptionRepository.declaredMethods.find { it.name == 'findAllByActiveStatusOrderByDescriptionName' }
        def ownerScoped = DescriptionRepository.declaredMethods.find { it.name == 'findAllByOwnerAndActiveStatusOrderByDescriptionName' }

        then:
        legacy.parameterTypes.any { it == Pageable.class }
        ownerScoped.parameterTypes.any { it == Pageable.class }
    }
}

