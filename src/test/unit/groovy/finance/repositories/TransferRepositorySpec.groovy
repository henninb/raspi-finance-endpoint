package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class TransferRepositorySpec extends Specification {
    def "extends JpaRepository and is interface"() {
        expect:
        TransferRepository.interfaces.any { it == JpaRepository }
        TransferRepository.isInterface()
        TransferRepository.name == 'finance.repositories.TransferRepository'
    }

    def "declares expected query method and return type"() {
        when:
        def method = TransferRepository.declaredMethods.find { it.name == 'findByTransferId' }

        then:
        method != null
        method.returnType == Optional.class
    }
}

