package finance.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

class PaymentRepositorySpec extends Specification {

    def "extends JpaRepository and is interface"() {
        expect:
        PaymentRepository.interfaces.any { it == JpaRepository }
        PaymentRepository.isInterface()
        PaymentRepository.name == 'finance.repositories.PaymentRepository'
    }

    def "declares all expected methods"() {
        when:
        def names = PaymentRepository.declaredMethods*.name as Set

        then:
        names.containsAll([
            'findByPaymentId',
            'findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot',
            'findByGuidSourceOrGuidDestination',
            'findByActiveStatusOrderByTransactionDateDesc',
            'findByOwnerAndPaymentId',
            'findByOwnerAndDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot',
            'findByOwnerAndGuidSourceOrOwnerAndGuidDestination',
            'findByOwnerAndActiveStatusOrderByTransactionDateDesc',
        ])
    }

    def "legacy methods have correct return types"() {
        when:
        def methods = PaymentRepository.declaredMethods

        then:
        methods.find { it.name == 'findByPaymentId' }?.returnType == Optional.class
        methods.find { it.name == 'findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot' }?.returnType == Optional.class
        methods.find { it.name == 'findByGuidSourceOrGuidDestination' }?.returnType == List.class
        methods.find { it.name == 'findByActiveStatusOrderByTransactionDateDesc' }?.returnType == Page.class
    }

    def "owner-scoped methods have correct return types"() {
        when:
        def methods = PaymentRepository.declaredMethods

        then:
        methods.find { it.name == 'findByOwnerAndPaymentId' }?.returnType == Optional.class
        methods.find { it.name == 'findByOwnerAndDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot' }?.returnType == Optional.class
        methods.find { it.name == 'findByOwnerAndGuidSourceOrOwnerAndGuidDestination' }?.returnType == List.class
        methods.find { it.name == 'findByOwnerAndActiveStatusOrderByTransactionDateDesc' }?.returnType == Page.class
    }

    def "paginated method accepts Pageable parameter"() {
        when:
        def paginatedLegacy = PaymentRepository.declaredMethods.find { it.name == 'findByActiveStatusOrderByTransactionDateDesc' }
        def paginatedOwner = PaymentRepository.declaredMethods.find { it.name == 'findByOwnerAndActiveStatusOrderByTransactionDateDesc' }

        then:
        paginatedLegacy.parameterTypes.any { it == Pageable.class }
        paginatedOwner.parameterTypes.any { it == Pageable.class }
    }
}

