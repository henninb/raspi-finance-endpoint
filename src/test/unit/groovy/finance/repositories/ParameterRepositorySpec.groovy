package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class ParameterRepositorySpec extends Specification {

    def "should extend JpaRepository interface"() {
        expect:
        ParameterRepository.interfaces.any { it == JpaRepository }
    }

    def "should be a proper Spring Data repository interface"() {
        expect:
        ParameterRepository.isInterface()
        ParameterRepository.name == 'finance.repositories.ParameterRepository'
    }

    def "should declare expected query methods"() {
        when:
        def names = ParameterRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByParameterName',
            'findByParameterId',
            'findByActiveStatusIsTrue',
        ])
    }

    def "should have correct method return types"() {
        when:
        def methods = ParameterRepository.declaredMethods

        then:
        methods.find { it.name == 'findByParameterName' }?.returnType == Optional.class
        methods.find { it.name == 'findByParameterId' }?.returnType == Optional.class
        methods.find { it.name == 'findByActiveStatusIsTrue' }?.returnType == List.class
    }
}

