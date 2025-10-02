package finance.controllers.graphql

import finance.controllers.dto.PaymentInputDto
import finance.controllers.dto.TransferInputDto
import finance.domain.Payment
import finance.domain.Transfer
import finance.services.StandardizedPaymentService
import finance.services.StandardizedTransferService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import spock.lang.Specification

import java.sql.Date

class GraphQLMutationControllerSpec extends Specification {

    GraphQLMutationController controller
    StandardizedPaymentService mockPaymentService
    StandardizedTransferService mockTransferService
    MeterRegistry mockMeterRegistry
    Counter mockCounter

    def setup() {
        mockPaymentService = Mock(StandardizedPaymentService)
        mockTransferService = Mock(StandardizedTransferService)
        mockMeterRegistry = Mock(MeterRegistry)
        mockCounter = Mock(Counter)

        controller = new GraphQLMutationController(
            mockPaymentService,
            mockTransferService,
            mockMeterRegistry
        )
    }

    def "createPayment should create payment with generated GUIDs"() {
        given: "a valid payment input"
        def inputDto = new PaymentInputDto(
            null,
            "checking_primary",
            "bills_payable",
            Date.valueOf("2024-01-15"),
            new BigDecimal("100.00"),
            null,
            null,
            true
        )

        and: "a saved payment"
        def savedPayment = new Payment(
            paymentId: 123L,
            sourceAccount: "checking_primary",
            destinationAccount: "bills_payable",
            transactionDate: Date.valueOf("2024-01-15"),
            amount: new BigDecimal("100.00"),
            guidSource: "generated-guid-source",
            guidDestination: "generated-guid-destination",
            activeStatus: true
        )

        when: "createPayment is called"
        def result = controller.createPayment(inputDto)

        then: "service inserts the payment"
        1 * mockPaymentService.insertPayment(_) >> { Payment payment ->
            assert payment.sourceAccount == "checking_primary"
            assert payment.destinationAccount == "bills_payable"
            assert payment.amount == new BigDecimal("100.00")
            assert payment.guidSource != null
            assert payment.guidDestination != null
            assert payment.activeStatus == true
            return savedPayment
        }

        and: "meter is incremented"
        1 * mockMeterRegistry.counter("graphql.payment.create.success") >> mockCounter
        1 * mockCounter.increment()

        and: "saved payment is returned"
        result == savedPayment
        result.paymentId == 123L
    }

    def "insertPayment should create payment with provided GUIDs"() {
        given: "a payment input with explicit GUIDs"
        def inputDto = new PaymentInputDto(
            null,
            "checking_primary",
            "bills_payable",
            Date.valueOf("2024-01-15"),
            new BigDecimal("100.00"),
            "explicit-guid-source",
            "explicit-guid-destination",
            true
        )

        and: "a saved payment"
        def savedPayment = new Payment(
            paymentId: 456L,
            sourceAccount: "checking_primary",
            destinationAccount: "bills_payable",
            transactionDate: Date.valueOf("2024-01-15"),
            amount: new BigDecimal("100.00"),
            guidSource: "explicit-guid-source",
            guidDestination: "explicit-guid-destination",
            activeStatus: true
        )

        when: "insertPayment is called"
        def result = controller.insertPayment(inputDto)

        then: "service inserts the payment with explicit GUIDs"
        1 * mockPaymentService.insertPayment(_) >> { Payment payment ->
            assert payment.guidSource == "explicit-guid-source"
            assert payment.guidDestination == "explicit-guid-destination"
            return savedPayment
        }

        and: "meter is incremented"
        1 * mockMeterRegistry.counter("graphql.payment.create.success") >> mockCounter
        1 * mockCounter.increment()

        and: "saved payment is returned"
        result == savedPayment
        result.paymentId == 456L
    }

    def "insertPayment should default activeStatus to true when null"() {
        given: "a payment input with null activeStatus"
        def inputDto = new PaymentInputDto(
            null,
            "checking_primary",
            "bills_payable",
            Date.valueOf("2024-01-15"),
            new BigDecimal("100.00"),
            null,
            null,
            null
        )

        and: "a saved payment"
        def savedPayment = new Payment(
            paymentId: 789L,
            sourceAccount: "checking_primary",
            destinationAccount: "bills_payable",
            activeStatus: true
        )

        when: "insertPayment is called"
        def result = controller.insertPayment(inputDto)

        then: "service inserts the payment with activeStatus defaulted to true"
        1 * mockPaymentService.insertPayment(_) >> { Payment payment ->
            assert payment.activeStatus == true
            return savedPayment
        }

        and: "meter is incremented"
        1 * mockMeterRegistry.counter("graphql.payment.create.success") >> mockCounter
        1 * mockCounter.increment()

        and: "saved payment is returned"
        result.activeStatus == true
    }

    def "deletePayment should delete payment by ID"() {
        given: "a payment ID"
        def paymentId = 123L

        when: "deletePayment is called"
        def result = controller.deletePayment(paymentId)

        then: "service deletes the payment"
        1 * mockPaymentService.deleteByPaymentId(123L) >> true

        and: "deletion success is returned"
        result == true
    }

    def "deletePayment should return false when payment not found"() {
        given: "a non-existent payment ID"
        def paymentId = 999L

        when: "deletePayment is called"
        def result = controller.deletePayment(paymentId)

        then: "service returns false"
        1 * mockPaymentService.deleteByPaymentId(999L) >> false

        and: "deletion failure is returned"
        result == false
    }

    def "createTransfer should create transfer with generated GUIDs (deprecated)"() {
        given: "a valid transfer input"
        def inputDto = new TransferInputDto(
            null,
            "checking_primary",
            "savings_primary",
            Date.valueOf("2024-01-15"),
            new BigDecimal("500.00"),
            null,
            null,
            true
        )

        and: "a saved transfer"
        def savedTransfer = new Transfer(
            transferId: 321L,
            sourceAccount: "checking_primary",
            destinationAccount: "savings_primary",
            transactionDate: Date.valueOf("2024-01-15"),
            amount: new BigDecimal("500.00"),
            guidSource: "generated-guid-source",
            guidDestination: "generated-guid-destination",
            activeStatus: true
        )

        when: "createTransfer is called"
        def result = controller.createTransfer(inputDto)

        then: "service inserts the transfer"
        1 * mockTransferService.insertTransfer(_) >> { Transfer transfer ->
            assert transfer.sourceAccount == "checking_primary"
            assert transfer.destinationAccount == "savings_primary"
            assert transfer.amount == new BigDecimal("500.00")
            assert transfer.guidSource != null
            assert transfer.guidDestination != null
            assert transfer.activeStatus == true
            return savedTransfer
        }

        and: "meter is incremented"
        1 * mockMeterRegistry.counter("graphql.transfer.create.success") >> mockCounter
        1 * mockCounter.increment()

        and: "saved transfer is returned"
        result == savedTransfer
        result.transferId == 321L
    }

    def "insertTransfer should create transfer with provided GUIDs"() {
        given: "a transfer input with explicit GUIDs"
        def inputDto = new TransferInputDto(
            null,
            "checking_primary",
            "savings_primary",
            Date.valueOf("2024-01-15"),
            new BigDecimal("500.00"),
            "explicit-guid-source",
            "explicit-guid-destination",
            true
        )

        and: "a saved transfer"
        def savedTransfer = new Transfer(
            transferId: 654L,
            sourceAccount: "checking_primary",
            destinationAccount: "savings_primary",
            transactionDate: Date.valueOf("2024-01-15"),
            amount: new BigDecimal("500.00"),
            guidSource: "explicit-guid-source",
            guidDestination: "explicit-guid-destination",
            activeStatus: true
        )

        when: "insertTransfer is called"
        def result = controller.insertTransfer(inputDto)

        then: "service inserts the transfer with explicit GUIDs"
        1 * mockTransferService.insertTransfer(_) >> { Transfer transfer ->
            assert transfer.guidSource == "explicit-guid-source"
            assert transfer.guidDestination == "explicit-guid-destination"
            return savedTransfer
        }

        and: "meter is incremented"
        1 * mockMeterRegistry.counter("graphql.transfer.create.success") >> mockCounter
        1 * mockCounter.increment()

        and: "saved transfer is returned"
        result == savedTransfer
        result.transferId == 654L
    }

    def "insertTransfer should default activeStatus to true when null"() {
        given: "a transfer input with null activeStatus"
        def inputDto = new TransferInputDto(
            null,
            "checking_primary",
            "savings_primary",
            Date.valueOf("2024-01-15"),
            new BigDecimal("500.00"),
            null,
            null,
            null
        )

        and: "a saved transfer"
        def savedTransfer = new Transfer(
            transferId: 987L,
            sourceAccount: "checking_primary",
            destinationAccount: "savings_primary",
            activeStatus: true
        )

        when: "insertTransfer is called"
        def result = controller.insertTransfer(inputDto)

        then: "service inserts the transfer with activeStatus defaulted to true"
        1 * mockTransferService.insertTransfer(_) >> { Transfer transfer ->
            assert transfer.activeStatus == true
            return savedTransfer
        }

        and: "meter is incremented"
        1 * mockMeterRegistry.counter("graphql.transfer.create.success") >> mockCounter
        1 * mockCounter.increment()

        and: "saved transfer is returned"
        result.activeStatus == true
    }

    def "deleteTransfer should delete transfer by ID"() {
        given: "a transfer ID"
        def transferId = 321L

        when: "deleteTransfer is called"
        def result = controller.deleteTransfer(transferId)

        then: "service deletes the transfer"
        1 * mockTransferService.deleteByTransferId(321L) >> true

        and: "deletion success is returned"
        result == true
    }

    def "deleteTransfer should return false when transfer not found"() {
        given: "a non-existent transfer ID"
        def transferId = 999L

        when: "deleteTransfer is called"
        def result = controller.deleteTransfer(transferId)

        then: "service returns false"
        1 * mockTransferService.deleteByTransferId(999L) >> false

        and: "deletion failure is returned"
        result == false
    }
}
