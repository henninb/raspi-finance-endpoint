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

    def "declares expected query method and return type"() {
        when:
        def method = ReceiptImageRepository.declaredMethods.find { it.name == 'findByTransactionId' }

        then:
        method != null
        method.returnType == Optional.class
    }
}

