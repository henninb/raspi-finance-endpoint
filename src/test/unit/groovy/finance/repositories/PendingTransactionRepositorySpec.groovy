package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

class PendingTransactionRepositorySpec extends Specification {

    def "extends JpaRepository and is interface"() {
        expect:
        PendingTransactionRepository.interfaces.any { it == JpaRepository }
        PendingTransactionRepository.isInterface()
        PendingTransactionRepository.name == 'finance.repositories.PendingTransactionRepository'
    }

    def "declares all expected methods"() {
        when:
        def names = PendingTransactionRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByPendingTransactionIdOrderByTransactionDateDesc',
            'findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc',
            'findAllByOwner',
            'deleteAllByOwner',
        ])
    }

    def "legacy method has correct return type"() {
        when:
        def method = PendingTransactionRepository.declaredMethods.find { it.name == 'findByPendingTransactionIdOrderByTransactionDateDesc' }

        then:
        method != null
        method.returnType == Optional.class
        method.parameterCount == 1
        method.parameterTypes[0] == long.class
    }

    def "owner-scoped find method has correct return type and parameter"() {
        when:
        def method = PendingTransactionRepository.declaredMethods.find { it.name == 'findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc' }

        then:
        method != null
        method.returnType == Optional.class
        method.parameterCount == 2
        method.parameterTypes[0] == String.class
        method.parameterTypes[1] == long.class
    }

    def "findAllByOwner returns List"() {
        when:
        def method = PendingTransactionRepository.declaredMethods.find { it.name == 'findAllByOwner' }

        then:
        method != null
        method.returnType == List.class
        method.parameterCount == 1
        method.parameterTypes[0] == String.class
    }

    def "deleteAllByOwner is transactional"() {
        when:
        def method = PendingTransactionRepository.declaredMethods.find { it.name == 'deleteAllByOwner' }

        then:
        method != null
        method.isAnnotationPresent(Transactional)
        method.parameterCount == 1
        method.parameterTypes[0] == String.class
    }
}

