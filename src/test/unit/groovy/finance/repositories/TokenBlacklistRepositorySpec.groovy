package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

class TokenBlacklistRepositorySpec extends Specification {

    def "extends JpaRepository and is interface"() {
        expect:
        TokenBlacklistRepository.interfaces.any { it == JpaRepository }
        TokenBlacklistRepository.isInterface()
        TokenBlacklistRepository.name == 'finance.repositories.TokenBlacklistRepository'
    }

    def "declares expected methods"() {
        when:
        def names = TokenBlacklistRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'existsByTokenHash',
            'deleteAllExpiredBefore',
        ])
    }

    def "existsByTokenHash returns boolean"() {
        when:
        def method = TokenBlacklistRepository.declaredMethods.find { it.name == 'existsByTokenHash' }

        then:
        method != null
        method.returnType == boolean.class
        method.parameterCount == 1
        method.parameterTypes[0] == String.class
    }

    def "deleteAllExpiredBefore has proper annotations and return type"() {
        when:
        def method = TokenBlacklistRepository.declaredMethods.find { it.name == 'deleteAllExpiredBefore' }

        then:
        method != null
        method.returnType == int.class
        method.isAnnotationPresent(Modifying)
        method.isAnnotationPresent(Transactional)
        method.isAnnotationPresent(Query)
        method.parameterCount == 1
        method.parameterTypes[0] == java.time.Instant.class

        def queryValue = method.getAnnotation(Query).value()
        queryValue.contains('DELETE FROM TokenBlacklist')
        queryValue.contains('expiresAt')
    }
}
