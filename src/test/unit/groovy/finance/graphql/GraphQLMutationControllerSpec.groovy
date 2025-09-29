package finance.graphql

import finance.controllers.GraphQLMutationController
import finance.controllers.dto.PaymentInputDto
import finance.controllers.dto.TransferInputDto
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Payment
import finance.domain.Transaction
import finance.domain.Transfer
import finance.services.BaseServiceSpec
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException

import java.sql.Date

class GraphQLMutationControllerSpec extends BaseServiceSpec {

    def meterRegistryMock = Mock(MeterRegistry)
    def paymentCreateCounterMock = Mock(Counter)
    def transferCreateCounterMock = Mock(Counter)
    def transferServiceMock = GroovyMock(finance.services.StandardizedTransferService)
    def controller = new GraphQLMutationController(paymentService, transferServiceMock, meterRegistryMock)

    void setup() {
        paymentService.meterService = meterService
        paymentService.validator = validator
        meterRegistryMock.counter("graphql.payment.create.success") >> paymentCreateCounterMock
        meterRegistryMock.counter("graphql.transfer.create.success") >> transferCreateCounterMock

        // Mock account service calls for payment processing
        def mockAccount = makeAccount(1L, 'bills_payable')
        accountRepositoryMock.findByAccountNameOwner('bills_payable') >> Optional.of(mockAccount)
        accountRepositoryMock.findByAccountNameOwner('checking_primary') >> Optional.of(makeAccount(2L, 'checking_primary'))
        accountRepositoryMock.findByAccountNameOwner('savings_primary') >> Optional.of(makeAccount(3L, 'savings_primary'))

        // Mock transaction service calls for payment processing - need different transactions for credit and debit
        def mockCreditTransaction = makeTransaction(1L, '11111111-1111-1111-1111-111111111111')
        def mockDebitTransaction = makeTransaction(2L, '22222222-2222-2222-2222-222222222222')
        transactionServiceMock.save(_) >>> [
            finance.domain.ServiceResult.Success.of(mockCreditTransaction),
            finance.domain.ServiceResult.Success.of(mockDebitTransaction)
        ]
    }

    // Helper methods for creating test data
    private static Account makeAccount(Long id = 1L, String owner = 'checking_primary') {
        new Account(
                id,
                owner,
                AccountType.Checking,
                true,
                '1234',
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new java.sql.Timestamp(0),
                new java.sql.Timestamp(System.currentTimeMillis())
        )
    }

    private static Payment makePayment(Long id = 1L) {
        new Payment(
                id,
                'checking_primary',
                'bills_payable',
                new Date(System.currentTimeMillis()),
                BigDecimal.valueOf(100.00),
                'guid-source',
                'guid-dest',
                true
        )
    }

    private static Transfer makeTransfer(Long id = 1L) {
        new Transfer(
                id,
                'checking_primary',
                'savings_primary',
                new Date(System.currentTimeMillis()),
                BigDecimal.valueOf(200.00),
                'guid-source',
                'guid-dest',
                true
        )
    }

    private static Transaction makeTransaction(Long id = 1L, String guid = null) {
        def transaction = new Transaction()
        transaction.transactionId = id
        transaction.guid = guid ?: UUID.randomUUID().toString()
        transaction.accountNameOwner = 'checking_primary'
        transaction.transactionDate = new Date(System.currentTimeMillis())
        transaction.amount = BigDecimal.valueOf(100.00)
        transaction.activeStatus = true
        return transaction
    }

    private static PaymentInputDto makePaymentInputDto(Map params = [:]) {
        def defaults = [
                sourceAccount: 'checking_primary',
                destinationAccount: 'bills_payable',
                amount: BigDecimal.valueOf(100.00),
                guidSource: null,
                guidDestination: null,
                activeStatus: true
        ]
        def merged = defaults + params
        new PaymentInputDto(
                null,                               // paymentId
                merged.sourceAccount,               // sourceAccount
                merged.destinationAccount,          // destinationAccount
                new Date(System.currentTimeMillis()), // transactionDate
                merged.amount,                      // amount
                merged.guidSource,                  // guidSource
                merged.guidDestination,             // guidDestination
                merged.activeStatus                 // activeStatus
        )
    }

    private static TransferInputDto makeTransferInputDto(Map params = [:]) {
        def defaults = [
                sourceAccount: 'checking_primary',
                destinationAccount: 'savings_primary',
                amount: BigDecimal.valueOf(200.00),
                guidSource: null,
                guidDestination: null,
                activeStatus: true
        ]
        def merged = defaults + params
        new TransferInputDto(
                null,
                merged.sourceAccount,
                merged.destinationAccount,
                new Date(System.currentTimeMillis()),
                merged.amount,
                merged.guidSource,
                merged.guidDestination,
                merged.activeStatus
        )
    }

    // Payment mutation tests
    def "createPayment mutation creates payment successfully"() {
        given:
        def inputDto = makePaymentInputDto()
        def savedPayment = makePayment(42L)
        paymentRepositoryMock.saveAndFlush(_ as Payment) >> savedPayment

        when:
        def result = controller.createPayment(inputDto)

        then:
        1 * paymentCreateCounterMock.increment()
        result == savedPayment
        result.paymentId == 42L
        result.sourceAccount == 'checking_primary'
        result.destinationAccount == 'bills_payable'
        result.amount == BigDecimal.valueOf(100.00)
        result.activeStatus == true
    }

    def "createPayment mutation sets default activeStatus when null"() {
        given:
        def inputDto = makePaymentInputDto(activeStatus: null)
        def savedPayment = makePayment(42L)
        paymentRepositoryMock.saveAndFlush(_ as Payment) >> savedPayment

        when:
        def result = controller.createPayment(inputDto)

        then:
        1 * paymentCreateCounterMock.increment()
        result.activeStatus == true
    }

    def "createPayment mutation generates unique GUIDs"() {
        given:
        def inputDto = makePaymentInputDto()
        def savedPayment = makePayment(42L)
        Payment capturedPayment
        paymentRepositoryMock.saveAndFlush(_ as Payment) >> { Payment payment ->
            capturedPayment = payment
            return savedPayment
        }

        when:
        def result = controller.createPayment(inputDto)

        then:
        capturedPayment.guidSource != null
        capturedPayment.guidDestination != null
        capturedPayment.guidSource != capturedPayment.guidDestination
        capturedPayment.guidSource.length() == 36 // UUID format
        capturedPayment.guidDestination.length() == 36 // UUID format
    }

    def "deletePayment mutation deletes payment successfully"() {
        given:
        def payment = makePayment(42L)
        paymentRepositoryMock.findByPaymentId(42L) >> Optional.of(payment)

        when:
        def result = controller.deletePayment(42L)

        then:
        1 * paymentRepositoryMock.delete(payment)
        result == true
    }

    def "deletePayment mutation returns false when payment not found"() {
        given:
        paymentRepositoryMock.findByPaymentId(999L) >> Optional.empty()

        when:
        def result = controller.deletePayment(999L)

        then:
        0 * paymentRepositoryMock.delete(_)
        result == false
    }

    // Transfer mutation tests
    def "createTransfer mutation creates transfer successfully"() {
        given:
        def inputDto = makeTransferInputDto()
        def savedTransfer = makeTransfer(42L)
        transferServiceMock.insertTransfer(_ as Transfer) >> savedTransfer

        when:
        def result = controller.createTransfer(inputDto)

        then:
        1 * transferCreateCounterMock.increment()
        result == savedTransfer
        result.transferId == 42L
        result.sourceAccount == 'checking_primary'
        result.destinationAccount == 'savings_primary'
        result.amount == BigDecimal.valueOf(200.00)
        result.activeStatus == true
    }

    def "createTransfer mutation sets default activeStatus when null"() {
        given:
        def inputDto = makeTransferInputDto(activeStatus: null)
        def savedTransfer = makeTransfer(42L)
        transferServiceMock.insertTransfer(_ as Transfer) >> savedTransfer

        when:
        def result = controller.createTransfer(inputDto)

        then:
        1 * transferCreateCounterMock.increment()
        result.activeStatus == true
    }

    def "createTransfer mutation generates unique GUIDs"() {
        given:
        def inputDto = makeTransferInputDto()
        def savedTransfer = makeTransfer(42L)
        Transfer capturedTransfer
        transferServiceMock.insertTransfer(_ as Transfer) >> { Transfer transfer ->
            capturedTransfer = transfer
            return savedTransfer
        }

        when:
        controller.createTransfer(inputDto)

        then:
        capturedTransfer.guidSource != null
        capturedTransfer.guidDestination != null
        capturedTransfer.guidSource != capturedTransfer.guidDestination
        capturedTransfer.guidSource.length() == 36 // UUID format
        capturedTransfer.guidDestination.length() == 36 // UUID format
    }

    def "deleteTransfer mutation deletes transfer successfully"() {
        given:
        transferServiceMock.deleteByTransferId(42L) >> true

        when:
        def result = controller.deleteTransfer(42L)

        then:
        result == true
    }

    def "deleteTransfer mutation returns false when transfer not found"() {
        given:
        transferServiceMock.deleteByTransferId(999L) >> false

        when:
        def result = controller.deleteTransfer(999L)

        then:
        result == false
    }

    // Validation tests
    def "createPayment mutation handles validation errors gracefully"() {
        given:
        def inputDto = makePaymentInputDto(amount: BigDecimal.valueOf(-1.00)) // Invalid amount
        def violation = Mock(ConstraintViolation)
        violation.getPropertyPath() >> Mock(jakarta.validation.Path) {
            toString() >> "amount"
        }
        violation.getMessage() >> "must be greater than or equal to 0.01"

        def violationException = new ConstraintViolationException([violation] as Set)
        paymentRepositoryMock.saveAndFlush(_ as Payment) >> { throw violationException }

        when:
        controller.createPayment(inputDto)

        then:
        thrown(ConstraintViolationException)
        0 * paymentCreateCounterMock.increment()
    }

    def "createTransfer mutation handles validation errors gracefully"() {
        given:
        def inputDto = makeTransferInputDto(amount: BigDecimal.valueOf(-1.00)) // Invalid amount
        def violation = Mock(ConstraintViolation)
        violation.getPropertyPath() >> Mock(jakarta.validation.Path) {
            toString() >> "amount"
        }
        violation.getMessage() >> "must be greater than or equal to 0.01"

        def violationException = new ConstraintViolationException([violation] as Set)
        transferServiceMock.insertTransfer(_ as Transfer) >> { throw violationException }

        when:
        controller.createTransfer(inputDto)

        then:
        thrown(ConstraintViolationException)
        0 * transferCreateCounterMock.increment()
    }


    // Edge cases and error scenarios
    def "createPayment mutation handles repository exceptions"() {
        given:
        def inputDto = makePaymentInputDto()
        paymentRepositoryMock.saveAndFlush(_ as Payment) >> { throw new RuntimeException("Database error") }

        when:
        controller.createPayment(inputDto)

        then:
        thrown(RuntimeException)
        0 * paymentCreateCounterMock.increment()
    }

    def "createTransfer mutation handles service exceptions"() {
        given:
        def inputDto = makeTransferInputDto()
        transferServiceMock.insertTransfer(_ as Transfer) >> { throw new RuntimeException("Service error") }

        when:
        controller.createTransfer(inputDto)

        then:
        thrown(RuntimeException)
        0 * transferCreateCounterMock.increment()
    }

    def "deletePayment mutation handles repository exceptions"() {
        given:
        paymentRepositoryMock.findByPaymentId(42L) >> { throw new RuntimeException("Database error") }

        when:
        controller.deletePayment(42L)

        then:
        thrown(RuntimeException)
    }

    def "deleteTransfer mutation handles service exceptions"() {
        given:
        transferServiceMock.deleteByTransferId(42L) >> { throw new RuntimeException("Service error") }

        when:
        controller.deleteTransfer(42L)

        then:
        thrown(RuntimeException)
    }

    // insertTransfer mutation tests - uses 'input' parameter name
    def "insertTransfer mutation creates transfer successfully"() {
        given:
        def inputDto = makeTransferInputDto()
        def savedTransfer = makeTransfer(42L)
        transferServiceMock.insertTransfer(_ as Transfer) >> savedTransfer

        when:
        def result = controller.insertTransfer(inputDto)

        then:
        1 * transferCreateCounterMock.increment()
        result == savedTransfer
        result.transferId == 42L
        result.sourceAccount == 'checking_primary'
        result.destinationAccount == 'savings_primary'
        result.amount == BigDecimal.valueOf(200.00)
        result.activeStatus == true
    }

    def "insertTransfer mutation sets default activeStatus when null"() {
        given:
        def inputDto = makeTransferInputDto(activeStatus: null)
        def savedTransfer = makeTransfer(42L)
        transferServiceMock.insertTransfer(_ as Transfer) >> savedTransfer

        when:
        def result = controller.insertTransfer(inputDto)

        then:
        1 * transferCreateCounterMock.increment()
        result.activeStatus == true
    }
}