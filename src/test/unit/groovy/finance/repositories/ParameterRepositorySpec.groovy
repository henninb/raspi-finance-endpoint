package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class ParameterRepositorySpec extends Specification {

    def "extends JpaRepository and is interface"() {
        expect:
        ParameterRepository.interfaces.any { it == JpaRepository }
        ParameterRepository.isInterface()
        ParameterRepository.name == 'finance.repositories.ParameterRepository'
    }

    def "declares all expected methods"() {
        when:
        def names = ParameterRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByParameterName',
            'findByParameterId',
            'findByActiveStatusIsTrue',
            'findByOwnerAndParameterName',
            'findByOwnerAndParameterId',
            'findByOwnerAndActiveStatusIsTrue',
        ])
    }

    def "legacy methods have correct return types"() {
        when:
        def methods = ParameterRepository.declaredMethods

        then:
        methods.find { it.name == 'findByParameterName' }?.returnType == Optional.class
        methods.find { it.name == 'findByParameterId' }?.returnType == Optional.class
        methods.find { it.name == 'findByActiveStatusIsTrue' }?.returnType == List.class
    }

    def "owner-scoped methods have correct return types"() {
        when:
        def methods = ParameterRepository.declaredMethods

        then:
        methods.find { it.name == 'findByOwnerAndParameterName' }?.returnType == Optional.class
        methods.find { it.name == 'findByOwnerAndParameterId' }?.returnType == Optional.class
        methods.find { it.name == 'findByOwnerAndActiveStatusIsTrue' }?.returnType == List.class
    }

    def "owner-scoped methods take String as first parameter"() {
        when:
        def methods = ParameterRepository.declaredMethods

        then:
        ['findByOwnerAndParameterName', 'findByOwnerAndParameterId', 'findByOwnerAndActiveStatusIsTrue'].each { name ->
            def m = methods.find { it.name == name }
            assert m != null
            assert m.parameterTypes[0] == String.class
        }
    }
}

