package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class UserRepositorySpec extends Specification {

    def "extends JpaRepository and is interface"() {
        expect:
        UserRepository.interfaces.any { it == JpaRepository }
        UserRepository.isInterface()
        UserRepository.name == 'finance.repositories.UserRepository'
    }

    def "declares all expected methods"() {
        when:
        def names = UserRepository.declaredMethods*.name as Set

        then:
        names.containsAll(['findByUsername', 'findByUsernameAndPassword'])
    }

    def "methods have correct return types"() {
        when:
        def methods = UserRepository.declaredMethods

        then:
        methods.find { it.name == 'findByUsername' }?.returnType == Optional.class
        methods.find { it.name == 'findByUsernameAndPassword' }?.returnType == Optional.class
    }

    def "findByUsername accepts a single String parameter"() {
        when:
        def method = UserRepository.declaredMethods.find { it.name == 'findByUsername' }

        then:
        method.parameterCount == 1
        method.parameterTypes[0] == String.class
    }

    def "findByUsernameAndPassword accepts two String parameters"() {
        when:
        def method = UserRepository.declaredMethods.find { it.name == 'findByUsernameAndPassword' }

        then:
        method.parameterCount == 2
        method.parameterTypes[0] == String.class
        method.parameterTypes[1] == String.class
    }

    def "repository declares exactly the expected number of own methods"() {
        when:
        def count = UserRepository.declaredMethods.size()

        then:
        count == 2
    }
}

