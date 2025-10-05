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

    def "declares expected query methods and return types"() {
        when:
        def methods = UserRepository.declaredMethods
        def names = methods*.name as Set

        then:
        names.containsAll(['findByUsername','findByUsernameAndPassword'])
        methods.find { it.name == 'findByUsername' }?.returnType == Optional.class
        methods.find { it.name == 'findByUsernameAndPassword' }?.returnType == Optional.class
    }
}

