package finance.repositories

import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class PaymentRepositorySpec extends Specification {
    def "extends JpaRepository and is interface"() {
        expect:
        PaymentRepository.interfaces.any { it == JpaRepository }
        PaymentRepository.isInterface()
        PaymentRepository.name == 'finance.repositories.PaymentRepository'
    }

    def "declares expected query methods and return types"() {
        when:
        def methods = PaymentRepository.declaredMethods
        def names = methods*.name as Set

        then:
        names.containsAll([
            'findByPaymentId',
            'findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot',
        ])
        methods.find { it.name == 'findByPaymentId' }?.returnType == Optional.class
        methods.find { it.name == 'findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot' }?.returnType == Optional.class
    }
}

