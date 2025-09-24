package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Payment
import finance.domain.ServiceResult
import finance.helpers.PaymentBuilder
import finance.repositories.PaymentRepository
import finance.services.StandardizedTransactionService
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import jakarta.persistence.EntityNotFoundException
import java.math.BigDecimal
import java.sql.Date

/**
 * TDD Specification for StandardizedPaymentService
 * Tests the Payment service using new ServiceResult pattern with comprehensive error handling
 */
class StandardizedPaymentServiceSpec extends BaseServiceSpec {

    def paymentRepositoryMock = Mock(PaymentRepository)
    def transactionServiceMock = Mock(StandardizedTransactionService)
    def standardizedPaymentService = new StandardizedPaymentService(paymentRepositoryMock, transactionServiceMock, accountService)

    void setup() {
        standardizedPaymentService.meterService = meterService
        standardizedPaymentService.validator = validatorMock
    }

    // ===== TDD Tests for findAllActive() =====

    def "findAllActive should return Success with payments when found"() {
        given: "existing active payments"
        def payments = [
            PaymentBuilder.builder().withPaymentId(1L).withAmount(new BigDecimal("150.00")).build(),
            PaymentBuilder.builder().withPaymentId(2L).withAmount(new BigDecimal("250.00")).build()
        ]

        when: "finding all active payments"
        def result = standardizedPaymentService.findAllActive()

        then: "should return Success with payments"
        1 * paymentRepositoryMock.findAll() >> payments
        result instanceof ServiceResult.Success
        result.data.size() == 2
        result.data[0].paymentId == 1L
        result.data[0].amount == new BigDecimal("150.00")
        result.data[1].paymentId == 2L
        result.data[1].amount == new BigDecimal("250.00")
        0 * _
    }

    def "findAllActive should return Success with empty list when no payments found"() {
        when: "finding all active payments with none existing"
        def result = standardizedPaymentService.findAllActive()

        then: "should return Success with empty list"
        1 * paymentRepositoryMock.findAll() >> []
        result instanceof ServiceResult.Success
        result.data.isEmpty()
        0 * _
    }

    // ===== TDD Tests for findById() =====

    def "findById should return Success with payment when found"() {
        given: "existing payment"
        def payment = PaymentBuilder.builder().withPaymentId(1L).build()

        when: "finding by valid ID"
        def result = standardizedPaymentService.findById(1L)

        then: "should return Success with payment"
        1 * paymentRepositoryMock.findByPaymentId(1L) >> Optional.of(payment)
        result instanceof ServiceResult.Success
        result.data.paymentId == 1L
        0 * _
    }

    def "findById should return NotFound when payment does not exist"() {
        when: "finding by non-existent ID"
        def result = standardizedPaymentService.findById(999L)

        then: "should return NotFound result"
        1 * paymentRepositoryMock.findByPaymentId(999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Payment not found: 999")
        0 * _
    }

    // ===== TDD Tests for save() =====

    def "save should return Success with saved payment when valid"() {
        given: "valid payment"
        def payment = PaymentBuilder.builder().build()
        def savedPayment = PaymentBuilder.builder().withPaymentId(1L).build()
        Set<ConstraintViolation<Payment>> noViolations = [] as Set

        when: "saving payment"
        def result = standardizedPaymentService.save(payment)

        then: "should return Success with saved payment"
        1 * validatorMock.validate(payment) >> noViolations
        1 * paymentRepositoryMock.saveAndFlush(payment) >> savedPayment
        result instanceof ServiceResult.Success
        result.data.paymentId == 1L
        0 * _
    }

    def "save should return ValidationError when payment has constraint violations"() {
        given: "invalid payment"
        def payment = PaymentBuilder.builder().withAmount(new BigDecimal("-100.00")).build()
        ConstraintViolation<Payment> violation = Mock(ConstraintViolation)
        def mockPath = Mock(javax.validation.Path)
        mockPath.toString() >> "amount"
        violation.propertyPath >> mockPath
        violation.message >> "must be greater than or equal to 0"
        Set<ConstraintViolation<Payment>> violations = [violation] as Set

        when: "saving invalid payment"
        def result = standardizedPaymentService.save(payment)

        then: "should return ValidationError result"
        1 * validatorMock.validate(payment) >> { throw new ConstraintViolationException("Validation failed", violations) }
        result instanceof ServiceResult.ValidationError
        result.errors.size() == 1
        result.errors.values().contains("must be greater than or equal to 0")
    }

    def "save should return BusinessError when duplicate payment exists"() {
        given: "payment that will cause duplicate key violation"
        def payment = PaymentBuilder.builder().build()
        Set<ConstraintViolation<Payment>> noViolations = [] as Set

        when: "saving duplicate payment"
        def result = standardizedPaymentService.save(payment)

        then: "should return BusinessError result"
        1 * validatorMock.validate(payment) >> noViolations
        1 * paymentRepositoryMock.saveAndFlush(payment) >> {
            throw new DataIntegrityViolationException("Duplicate entry")
        }
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("data integrity")
        result.errorCode == "DATA_INTEGRITY_VIOLATION"
        0 * _
    }

    // ===== TDD Tests for update() =====

    def "update should return Success with updated payment when exists"() {
        given: "existing payment to update"
        def existingPayment = PaymentBuilder.builder().withPaymentId(1L).withAmount(new BigDecimal("100.00")).build()
        def updatedPayment = PaymentBuilder.builder().withPaymentId(1L).withAmount(new BigDecimal("200.00")).build()

        when: "updating existing payment"
        def result = standardizedPaymentService.update(updatedPayment)

        then: "should return Success with updated payment"
        1 * paymentRepositoryMock.findByPaymentId(1L) >> Optional.of(existingPayment)
        1 * paymentRepositoryMock.saveAndFlush(_ as Payment) >> { Payment payment ->
            assert payment.amount == new BigDecimal("200.00")
            return payment
        }
        result instanceof ServiceResult.Success
        result.data.amount == new BigDecimal("200.00")
        0 * _
    }

    def "update should return NotFound when payment does not exist"() {
        given: "payment with non-existent ID"
        def payment = PaymentBuilder.builder().withPaymentId(999L).build()

        when: "updating non-existent payment"
        def result = standardizedPaymentService.update(payment)

        then: "should return NotFound result"
        1 * paymentRepositoryMock.findByPaymentId(999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Payment not found: 999")
        0 * _
    }

    // ===== TDD Tests for deleteById() =====

    def "deleteById should return Success when payment exists"() {
        given: "existing payment"
        def payment = PaymentBuilder.builder().withPaymentId(1L).build()

        when: "deleting existing payment"
        def result = standardizedPaymentService.deleteById(1L)

        then: "should return Success"
        1 * paymentRepositoryMock.findByPaymentId(1L) >> Optional.of(payment)
        1 * paymentRepositoryMock.delete(payment)
        result instanceof ServiceResult.Success
        result.data == true
        0 * _
    }

    def "deleteById should return NotFound when payment does not exist"() {
        when: "deleting non-existent payment"
        def result = standardizedPaymentService.deleteById(999L)

        then: "should return NotFound result"
        1 * paymentRepositoryMock.findByPaymentId(999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Payment not found: 999")
        0 * _
    }

    // ===== TDD Tests for Legacy Method Support =====

    def "findAllPayments should delegate to findAllActive and return data"() {
        given: "existing payments"
        def payments = [PaymentBuilder.builder().build()]

        when: "calling legacy findAllPayments method"
        def result = standardizedPaymentService.findAllPayments()

        then: "should return payment list"
        1 * paymentRepositoryMock.findAll() >> payments
        result.size() == 1
        0 * _
    }

    def "insertPayment should delegate to save and return data"() {
        given: "valid payment"
        def payment = PaymentBuilder.builder().build()
        def savedPayment = PaymentBuilder.builder().withPaymentId(1L).build()
        Set<ConstraintViolation<Payment>> noViolations = [] as Set
        def mockDestAccount = GroovyMock(finance.domain.Account)
        def mockSourceAccount = GroovyMock(finance.domain.Account)
        mockDestAccount.accountType >> finance.domain.AccountType.Credit

        when: "calling legacy insertPayment method"
        def result = standardizedPaymentService.insertPayment(payment)

        then: "should return saved payment"
        1 * validatorMock.validate(payment) >> noViolations
        2 * accountRepositoryMock.findByAccountNameOwner(payment.destinationAccount) >> Optional.of(mockDestAccount)
        1 * accountRepositoryMock.findByAccountNameOwner(payment.sourceAccount) >> Optional.of(mockSourceAccount)
        2 * transactionServiceMock.save(_) >> { new ServiceResult.Success(GroovyMock(finance.domain.Transaction) { getGuid() >> "test-guid" }) }
        1 * paymentRepositoryMock.saveAndFlush(payment) >> savedPayment
        result.paymentId == 1L
    }

    def "updatePayment should delegate to update and return data"() {
        given: "existing payment to update"
        def existingPayment = PaymentBuilder.builder().withPaymentId(1L).withAmount(new BigDecimal("100.00")).build()
        def updatedPayment = PaymentBuilder.builder().withPaymentId(1L).withAmount(new BigDecimal("200.00")).build()

        when: "calling legacy updatePayment method"
        def result = standardizedPaymentService.updatePayment(1L, updatedPayment)

        then: "should return updated payment"
        1 * paymentRepositoryMock.findByPaymentId(1L) >> Optional.of(existingPayment)
        1 * paymentRepositoryMock.saveAndFlush(_ as Payment) >> { Payment payment -> return payment }
        result.amount == new BigDecimal("200.00")
        0 * _
    }

    def "findByPaymentId should return payment when found"() {
        given: "existing payment"
        def payment = PaymentBuilder.builder().withPaymentId(1L).build()

        when: "finding by payment ID"
        def result = standardizedPaymentService.findByPaymentId(1L)

        then: "should return payment optional"
        1 * paymentRepositoryMock.findByPaymentId(1L) >> Optional.of(payment)
        result.isPresent()
        result.get().paymentId == 1L
        0 * _
    }

    def "deleteByPaymentId should delete payment when exists"() {
        given: "existing payment"
        def payment = PaymentBuilder.builder().withPaymentId(1L).build()

        when: "deleting by payment ID"
        def result = standardizedPaymentService.deleteByPaymentId(1L)

        then: "should delete payment and return true"
        1 * paymentRepositoryMock.findByPaymentId(1L) >> Optional.of(payment)
        1 * paymentRepositoryMock.delete(payment)
        result == true
        0 * _
    }

    def "deleteByPaymentId should return false when payment does not exist"() {
        when: "deleting non-existent payment"
        def result = standardizedPaymentService.deleteByPaymentId(999L)

        then: "should return false"
        1 * paymentRepositoryMock.findByPaymentId(999L) >> Optional.empty()
        result == false
        0 * _
    }

    // ===== TDD Tests for Error Handling in Legacy Methods =====

    //@spock.lang.Ignore("TODO: Fix test interaction with interface mocking")
    def "insertPayment should throw ValidationException for invalid payment"() {
        given: "invalid payment"
        def payment = PaymentBuilder.builder().withAmount(new BigDecimal("-100.00")).build()
        ConstraintViolation<Payment> violation = Mock(ConstraintViolation)
        violation.invalidValue >> new BigDecimal("-100.00")
        violation.message >> "must be greater than or equal to 0"
        Set<ConstraintViolation<Payment>> violations = [violation] as Set

        and: "account service returns existing accounts"
        def existingAccount = GroovyMock(Account)
        existingAccount.accountType >> AccountType.Credit
        accountRepositoryMock.findByAccountNameOwner(payment.destinationAccount) >> Optional.of(existingAccount)
        accountRepositoryMock.findByAccountNameOwner(payment.sourceAccount) >> Optional.of(existingAccount)

        when: "calling legacy insertPayment with invalid data"
        standardizedPaymentService.insertPayment(payment)

        then: "should mock transaction service calls and repository save"
        2 * transactionServiceMock.save(_) >> { new ServiceResult.Success(GroovyMock(finance.domain.Transaction) { getGuid() >> "test-guid" }) }
        and: "should throw ConstraintViolationException from save method"
        1 * validatorMock.validate(payment) >> violations
        thrown(ConstraintViolationException)
    }

    def "updatePayment should throw RuntimeException when payment not found"() {
        given: "payment with non-existent ID"
        def payment = PaymentBuilder.builder().withPaymentId(999L).build()

        when: "calling legacy updatePayment with non-existent payment"
        standardizedPaymentService.updatePayment(999L, payment)

        then: "should throw RuntimeException"
        1 * paymentRepositoryMock.findByPaymentId(999L) >> Optional.empty()
        thrown(RuntimeException)
        0 * _
    }
}
