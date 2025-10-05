package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class DescriptionRepositorySpec extends Specification {

    def "should extend JpaRepository interface"() {
        expect:
        DescriptionRepository.interfaces.any { it == JpaRepository }
    }

    def "should be a proper Spring Data repository interface"() {
        expect:
        DescriptionRepository.isInterface()
        DescriptionRepository.name == 'finance.repositories.DescriptionRepository'
    }

    def "should declare expected query methods"() {
        when:
        def names = DescriptionRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByActiveStatusOrderByDescriptionName',
            'findByDescriptionName',
            'findByDescriptionId',
        ])
    }

    def "should have correct method return types"() {
        when:
        def methods = DescriptionRepository.declaredMethods

        then:
        methods.find { it.name == 'findByActiveStatusOrderByDescriptionName' }?.returnType == List.class
        methods.find { it.name == 'findByDescriptionName' }?.returnType == Optional.class
        methods.find { it.name == 'findByDescriptionId' }?.returnType == Optional.class
    }
}

