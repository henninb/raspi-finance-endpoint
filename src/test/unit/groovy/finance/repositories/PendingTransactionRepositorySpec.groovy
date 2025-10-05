package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class PendingTransactionRepositorySpec extends Specification {
    def "extends JpaRepository and is interface"() {
        expect:
        PendingTransactionRepository.interfaces.any { it == JpaRepository }
        PendingTransactionRepository.isInterface()
        PendingTransactionRepository.name == 'finance.repositories.PendingTransactionRepository'
    }

    def "declares expected query method and return type"() {
        when:
        def method = PendingTransactionRepository.declaredMethods.find { it.name == 'findByPendingTransactionIdOrderByTransactionDateDesc' }

        then:
        method != null
        method.returnType == Optional.class
    }
}

