package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class ReceiptImageRepositorySpec extends Specification {

    def "extends JpaRepository and is interface"() {
        expect:
        ReceiptImageRepository.interfaces.any { it == JpaRepository }
        ReceiptImageRepository.isInterface()
        ReceiptImageRepository.name == 'finance.repositories.ReceiptImageRepository'
    }

    def "declares all expected methods"() {
        when:
        def names = ReceiptImageRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByTransactionId',
            'findByOwnerAndTransactionId',
            'findByOwnerAndReceiptImageId',
            'findAllByOwner',
        ])
    }

    def "legacy method has correct return type"() {
        when:
        def method = ReceiptImageRepository.declaredMethods.find { it.name == 'findByTransactionId' }

        then:
        method != null
        method.returnType == Optional.class
        method.parameterCount == 1
        method.parameterTypes[0] == long.class
    }

    def "owner-scoped find by transaction returns Optional"() {
        when:
        def method = ReceiptImageRepository.declaredMethods.find { it.name == 'findByOwnerAndTransactionId' }

        then:
        method != null
        method.returnType == Optional.class
        method.parameterCount == 2
        method.parameterTypes[0] == String.class
        method.parameterTypes[1] == long.class
    }

    def "owner-scoped find by receipt image id returns Optional"() {
        when:
        def method = ReceiptImageRepository.declaredMethods.find { it.name == 'findByOwnerAndReceiptImageId' }

        then:
        method != null
        method.returnType == Optional.class
        method.parameterCount == 2
        method.parameterTypes[0] == String.class
        method.parameterTypes[1] == long.class
    }

    def "findAllByOwner returns List"() {
        when:
        def method = ReceiptImageRepository.declaredMethods.find { it.name == 'findAllByOwner' }

        then:
        method != null
        method.returnType == List.class
        method.parameterCount == 1
        method.parameterTypes[0] == String.class
    }
}

