package finance.controllers.graphql

import finance.controllers.dto.PaymentInputDto
import finance.controllers.dto.TransferInputDto
import finance.domain.Category
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.ServiceResult
import finance.domain.Transfer
import finance.services.StandardizedAccountService
import finance.services.StandardizedCategoryService
import finance.services.StandardizedDescriptionService
import finance.services.StandardizedMedicalExpenseService
import finance.services.StandardizedParameterService
import finance.services.StandardizedPaymentService
import finance.services.StandardizedTransactionService
import finance.services.StandardizedTransferService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import spock.lang.Specification

import java.sql.Date

class GraphQLMutationControllerSpec extends Specification {

    GraphQLMutationController controller
    StandardizedAccountService mockAccountService
    StandardizedCategoryService mockCategoryService
    StandardizedDescriptionService mockDescriptionService
    StandardizedMedicalExpenseService mockMedicalExpenseService
    StandardizedParameterService mockParameterService
    StandardizedPaymentService mockPaymentService
    StandardizedTransactionService mockTransactionService
    StandardizedTransferService mockTransferService
    finance.services.StandardizedValidationAmountService mockValidationAmountService
    MeterRegistry mockMeterRegistry
    Counter mockCounter

    def setup() {
        mockAccountService = Mock(StandardizedAccountService)
        mockCategoryService = Mock(StandardizedCategoryService)
        mockDescriptionService = Mock(StandardizedDescriptionService)
        mockMedicalExpenseService = Mock(StandardizedMedicalExpenseService)
        mockParameterService = Mock(StandardizedParameterService)
        mockPaymentService = Mock(StandardizedPaymentService)
        mockTransactionService = Mock(StandardizedTransactionService)
        mockTransferService = Mock(StandardizedTransferService)
        mockValidationAmountService = Mock(finance.services.StandardizedValidationAmountService)
        mockMeterRegistry = Mock(MeterRegistry)
        mockCounter = Mock(Counter)

        controller = new GraphQLMutationController(
            mockAccountService,
            mockCategoryService,
            mockDescriptionService,
            mockMedicalExpenseService,
            mockParameterService,
            mockPaymentService,
            mockTransactionService,
            mockTransferService,
            mockValidationAmountService,
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

    def "createTransfer should create transfer with generated GUIDs"() {
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

    def "createParameter should create parameter successfully"() {
        given: "a valid parameter"
        def parameter = new Parameter(0L, "test_param", "test_value", true)

        and: "a saved parameter"
        def savedParameter = new Parameter(123L, "test_param", "test_value", true)

        when: "createParameter is called"
        def result = controller.createParameter(parameter)

        then: "service returns success result"
        1 * mockParameterService.save(parameter) >> ServiceResult.Success.of(savedParameter)

        and: "meter registry is called"
        1 * mockMeterRegistry.counter("graphql.parameter.create.success") >> mockCounter
        1 * mockCounter.increment()

        and: "saved parameter is returned"
        result == savedParameter
        result.parameterId == 123L
    }

    def "createParameter should handle validation errors"() {
        given: "an invalid parameter"
        def parameter = new Parameter(0L, "", "test_value", true)

        when: "createParameter is called"
        controller.createParameter(parameter)

        then: "service returns validation error"
        1 * mockParameterService.save(parameter) >> ServiceResult.ValidationError.of(["parameterName": "must not be blank"])

        and: "exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "updateParameter should update parameter successfully"() {
        given: "an existing parameter"
        def parameter = new Parameter(123L, "test_param", "updated_value", true)

        when: "updateParameter is called"
        def result = controller.updateParameter(parameter)

        then: "service returns success result"
        1 * mockParameterService.update(parameter) >> ServiceResult.Success.of(parameter)

        and: "meter registry is called"
        1 * mockMeterRegistry.counter("graphql.parameter.update.success") >> mockCounter
        1 * mockCounter.increment()

        and: "updated parameter is returned"
        result == parameter
        result.parameterValue == "updated_value"
    }

    def "updateParameter should handle not found errors"() {
        given: "a non-existent parameter"
        def parameter = new Parameter(999L, "test_param", "test_value", true)

        when: "updateParameter is called"
        controller.updateParameter(parameter)

        then: "service returns not found"
        1 * mockParameterService.update(parameter) >> ServiceResult.NotFound.of("Parameter not found")

        and: "exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "deleteParameter should delete parameter successfully"() {
        given: "an existing parameter ID"
        def parameterId = 123L

        when: "deleteParameter is called"
        def result = controller.deleteParameter(parameterId)

        then: "service returns success result"
        1 * mockParameterService.deleteById(parameterId) >> ServiceResult.Success.of(true)

        and: "meter registry is called"
        1 * mockMeterRegistry.counter("graphql.parameter.delete.success") >> mockCounter
        1 * mockCounter.increment()

        and: "deletion success is returned"
        result == true
    }

    def "deleteParameter should return false when parameter not found"() {
        given: "a non-existent parameter ID"
        def parameterId = 999L

        when: "deleteParameter is called"
        def result = controller.deleteParameter(parameterId)

        then: "service returns not found"
        1 * mockParameterService.deleteById(parameterId) >> ServiceResult.NotFound.of("Parameter not found")

        and: "deletion failure is returned"
        result == false
    }
}
