package finance.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class TransferRepositorySpec extends Specification {

    def "extends JpaRepository and is interface"() {
        expect:
        TransferRepository.interfaces.any { it == JpaRepository }
        TransferRepository.isInterface()
        TransferRepository.name == 'finance.repositories.TransferRepository'
    }

    def "declares all expected methods"() {
        when:
        def names = TransferRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByTransferId',
            'findByActiveStatusOrderByTransactionDateDesc',
            'findByOwnerAndTransferId',
            'findByOwnerAndActiveStatusOrderByTransactionDateDesc',
        ])
    }

    def "legacy methods have correct return types"() {
        when:
        def methods = TransferRepository.declaredMethods

        then:
        methods.find { it.name == 'findByTransferId' }?.returnType == Optional.class
        methods.find { it.name == 'findByActiveStatusOrderByTransactionDateDesc' }?.returnType == Page.class
    }

    def "owner-scoped methods have correct return types"() {
        when:
        def methods = TransferRepository.declaredMethods

        then:
        methods.find { it.name == 'findByOwnerAndTransferId' }?.returnType == Optional.class
        methods.find { it.name == 'findByOwnerAndActiveStatusOrderByTransactionDateDesc' }?.returnType == Page.class
    }

    def "paginated methods accept Pageable parameter"() {
        when:
        def legacy = TransferRepository.declaredMethods.find { it.name == 'findByActiveStatusOrderByTransactionDateDesc' }
        def ownerScoped = TransferRepository.declaredMethods.find { it.name == 'findByOwnerAndActiveStatusOrderByTransactionDateDesc' }

        then:
        legacy.parameterTypes.any { it == Pageable.class }
        ownerScoped.parameterTypes.any { it == Pageable.class }
    }

    def "findByOwnerAndTransferId accepts String and Long"() {
        when:
        def method = TransferRepository.declaredMethods.find { it.name == 'findByOwnerAndTransferId' }

        then:
        method.parameterCount == 2
        method.parameterTypes[0] == String.class
        method.parameterTypes[1] == long.class
    }
}

