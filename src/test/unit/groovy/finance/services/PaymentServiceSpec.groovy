package finance.services
import finance.configurations.ResilienceComponents

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Payment
import finance.domain.ServiceResult
import finance.helpers.PaymentBuilder
import finance.repositories.PaymentRepository
import finance.services.TransactionService
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import jakarta.persistence.EntityNotFoundException
import java.math.BigDecimal

/**
 * TDD Specification for PaymentService
 * Tests the Payment service using new ServiceResult pattern with comprehensive error handling
 */
class PaymentServiceSpec extends BaseServiceSpec {

    def paymentRepositoryMock = Mock(PaymentRepository)
    def transactionServiceMock = Mock(TransactionService)
    def standardizedPaymentService = new PaymentService(paymentRepositoryMock, transactionServiceMock, accountServiceMock, meterService, validatorMock, ResilienceComponents.noOp())

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
        1 * paymentRepositoryMock.findByOwnerAndActiveStatusOrderByTransactionDateDesc(TEST_OWNER, true, _) >> new org.springframework.data.domain.PageImpl(payments)
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
        1 * paymentRepositoryMock.findByOwnerAndActiveStatusOrderByTransactionDateDesc(TEST_OWNER, true, _) >> new org.springframework.data.domain.PageImpl([])
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
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,1L) >> Optional.of(payment)
        result instanceof ServiceResult.Success
        result.data.paymentId == 1L
        0 * _
    }

    def "findById should return NotFound when payment does not exist"() {
        when: "finding by non-existent ID"
        def result = standardizedPaymentService.findById(999L)

        then: "should return NotFound result"
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,999L) >> Optional.empty()
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
        def mockPath = Mock(jakarta.validation.Path)
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
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,1L) >> Optional.of(existingPayment)
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
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Payment not found: 999")
        0 * _
    }

    // ===== TDD Tests for deleteById() =====

    def "deleteById should return Success when payment exists with no GUIDs"() {
        given: "existing payment with null GUIDs"
        def payment = PaymentBuilder.builder()
                .withPaymentId(1L)
                .withGuidSource(null)
                .withGuidDestination(null)
                .build()

        when: "deleting existing payment"
        def result = standardizedPaymentService.deleteById(1L)

        then: "should return Success"
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,1L) >> Optional.of(payment)
        0 * transactionServiceMock.deleteByIdInternal(_)  // No transactions to delete
        1 * paymentRepositoryMock.delete(payment)
        1 * paymentRepositoryMock.flush()
        result instanceof ServiceResult.Success
        result.data != null
    }

    def "deleteById should return NotFound when payment does not exist"() {
        when: "deleting non-existent payment"
        def result = standardizedPaymentService.deleteById(999L)

        then: "should return NotFound result"
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Payment not found: 999")
        0 * _
    }

    // ===== TDD Tests for Cascade Delete =====

    def "deleteById should cascade delete source and destination transactions"() {
        given: "a payment with valid transaction GUIDs"
        def paymentId = 1L
        def payment = PaymentBuilder.builder()
                .withPaymentId(paymentId)
                .withGuidSource("source-guid-123")
                .withGuidDestination("dest-guid-456")
                .build()

        when: "deleteById is called"
        def result = standardizedPaymentService.deleteById(paymentId)

        then: "repository finds the payment"
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,paymentId) >> Optional.of(payment)

        and: "transaction service deletes source transaction"
        1 * transactionServiceMock.deleteByIdInternal("source-guid-123") >>
                new ServiceResult.Success(true)

        and: "transaction service deletes destination transaction"
        1 * transactionServiceMock.deleteByIdInternal("dest-guid-456") >>
                new ServiceResult.Success(true)

        and: "payment repository deletes the payment"
        1 * paymentRepositoryMock.delete(payment)
        1 * paymentRepositoryMock.flush()

        and: "result is Success"
        result instanceof ServiceResult.Success
        result.data != null
    }

    def "deleteById should handle missing source transaction gracefully"() {
        given: "a payment where source transaction doesn't exist"
        def paymentId = 2L
        def payment = PaymentBuilder.builder()
                .withPaymentId(paymentId)
                .withGuidSource("missing-source")
                .withGuidDestination("valid-dest")
                .build()

        when: "deleteById is called"
        def result = standardizedPaymentService.deleteById(paymentId)

        then: "repository finds the payment"
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,paymentId) >> Optional.of(payment)

        and: "source transaction delete returns NotFound (logged as warning)"
        1 * transactionServiceMock.deleteByIdInternal("missing-source") >>
                new ServiceResult.NotFound("Transaction not found")

        and: "destination transaction still gets deleted"
        1 * transactionServiceMock.deleteByIdInternal("valid-dest") >>
                new ServiceResult.Success(true)

        and: "payment is still deleted"
        1 * paymentRepositoryMock.delete(payment)
        1 * paymentRepositoryMock.flush()

        and: "result is Success"
        result instanceof ServiceResult.Success
    }

    def "deleteById should handle null transaction GUIDs"() {
        given: "a payment with null transaction GUIDs"
        def paymentId = 3L
        def payment = PaymentBuilder.builder()
                .withPaymentId(paymentId)
                .withGuidSource(null)
                .withGuidDestination(null)
                .build()

        when: "deleteById is called"
        def result = standardizedPaymentService.deleteById(paymentId)

        then: "repository finds the payment"
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,paymentId) >> Optional.of(payment)

        and: "no transaction deletes are attempted"
        0 * transactionServiceMock.deleteByIdInternal(_)

        and: "payment is deleted"
        1 * paymentRepositoryMock.delete(payment)
        1 * paymentRepositoryMock.flush()

        and: "result is Success"
        result instanceof ServiceResult.Success
    }

    def "deleteById should handle blank transaction GUIDs"() {
        given: "a payment with blank transaction GUIDs"
        def paymentId = 4L
        def payment = PaymentBuilder.builder()
                .withPaymentId(paymentId)
                .withGuidSource("")
                .withGuidDestination("  ")
                .build()

        when: "deleteById is called"
        def result = standardizedPaymentService.deleteById(paymentId)

        then: "repository finds the payment"
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,paymentId) >> Optional.of(payment)

        and: "no transaction deletes are attempted"
        0 * transactionServiceMock.deleteByIdInternal(_)

        and: "payment is deleted"
        1 * paymentRepositoryMock.delete(payment)

        and: "result is Success"
        result instanceof ServiceResult.Success
    }

    def "deleteById should fail if source transaction delete has BusinessError (payment delete invoked, transaction rolls back)"() {
        given: "a payment where source transaction delete fails"
        def paymentId = 5L
        def payment = PaymentBuilder.builder()
                .withPaymentId(paymentId)
                .withGuidSource("locked-transaction")
                .withGuidDestination("valid-dest")
                .build()

        when: "deleteById is called"
        def result = standardizedPaymentService.deleteById(paymentId)

        then: "repository finds the payment"
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,paymentId) >> Optional.of(payment)

        and: "payment delete is attempted first (will be rolled back by transaction)"
        1 * paymentRepositoryMock.delete(payment)

        and: "source transaction delete returns BusinessError"
        1 * transactionServiceMock.deleteByIdInternal("locked-transaction") >>
                new ServiceResult.BusinessError("Transaction is locked", "TRANSACTION_LOCKED")

        and: "destination transaction delete is NOT attempted"
        0 * transactionServiceMock.deleteByIdInternal("valid-dest")

        and: "result is BusinessError with clear message"
        result instanceof ServiceResult.BusinessError
        result.message.contains("Cannot delete payment")
        result.message.contains("source transaction")
        result.message.contains("locked-transaction")
    }

    def "deleteById should fail if destination transaction delete has BusinessError (payment delete invoked, transaction rolls back)"() {
        given: "a payment where destination transaction delete fails"
        def paymentId = 6L
        def payment = PaymentBuilder.builder()
                .withPaymentId(paymentId)
                .withGuidSource("valid-source")
                .withGuidDestination("locked-dest")
                .build()

        when: "deleteById is called"
        def result = standardizedPaymentService.deleteById(paymentId)

        then: "repository finds the payment"
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,paymentId) >> Optional.of(payment)

        and: "payment delete is attempted first (will be rolled back by transaction)"
        1 * paymentRepositoryMock.delete(payment)

        and: "source transaction deletes successfully"
        1 * transactionServiceMock.deleteByIdInternal("valid-source") >>
                new ServiceResult.Success(true)

        and: "destination transaction delete returns BusinessError"
        1 * transactionServiceMock.deleteByIdInternal("locked-dest") >>
                new ServiceResult.BusinessError("Transaction is locked", "TRANSACTION_LOCKED")

        and: "result is BusinessError with clear message"
        result instanceof ServiceResult.BusinessError
        result.message.contains("Cannot delete payment")
        result.message.contains("destination transaction")
        result.message.contains("locked-dest")
    }

    def "updatePayment should delegate to update and return data"() {
        given: "existing payment to update"
        def existingPayment = PaymentBuilder.builder().withPaymentId(1L).withAmount(new BigDecimal("100.00")).build()
        def updatedPayment = PaymentBuilder.builder().withPaymentId(1L).withAmount(new BigDecimal("200.00")).build()

        when: "calling legacy updatePayment method"
        def result = standardizedPaymentService.updatePayment(1L, updatedPayment)

        then: "should return updated payment"
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,1L) >> Optional.of(existingPayment)
        1 * paymentRepositoryMock.saveAndFlush(_ as Payment) >> { Payment payment -> return payment }
        result.amount == new BigDecimal("200.00")
        0 * _
    }

    def "save should return Success and create transactions when GUIDs are missing"() {
        given: "a payment without transaction GUIDs"
        def payment = PaymentBuilder.builder()
                .withGuidSource(null)
                .withGuidDestination(null)
                .withSourceAccount("chase_checking")
                .withDestinationAccount("discover_credit")
                .withAmount(new BigDecimal("100.00"))
                .build()
        
        def sourceAccount = new Account()
        sourceAccount.accountType = AccountType.Checking
        
        def destinationAccount = new Account()
        destinationAccount.accountType = AccountType.CreditCard
        
        def noViolations = [] as Set

        when: "saving the payment"
        def result = standardizedPaymentService.save(payment)

        then: "should return Success and have populated GUIDs"
        1 * validatorMock.validate(payment) >> noViolations
        
        // Account processing order: destination then source
        1 * accountServiceMock.account("discover_credit") >> Optional.of(destinationAccount)
        1 * accountServiceMock.account("chase_checking") >> Optional.of(sourceAccount)
        
        // Transaction creation order: destination then source
        1 * transactionServiceMock.save(_ as finance.domain.Transaction) >> { finance.domain.Transaction t ->
            assert t.accountNameOwner == "discover_credit"
            // PaymentService.kt: calculateDestinationAmount for BILL_PAYMENT is -amount.abs()
            assert t.amount == new BigDecimal("-100.00")
            t.guid = "new-dest-guid"
            return ServiceResult.Success.of(t)
        }
        1 * transactionServiceMock.save(_ as finance.domain.Transaction) >> { finance.domain.Transaction t ->
            assert t.accountNameOwner == "chase_checking"
            // PaymentService.kt: calculateSourceAmount for BILL_PAYMENT is -amount.abs()
            assert t.amount == new BigDecimal("-100.00")
            t.guid = "new-source-guid"
            return ServiceResult.Success.of(t)
        }
        
        1 * paymentRepositoryMock.saveAndFlush(payment) >> payment
        
        result instanceof ServiceResult.Success
        payment.guidSource == "new-source-guid"
        payment.guidDestination == "new-dest-guid"
    }

    def "save should create missing accounts during transaction creation"() {
        given: "a payment with a non-existent source account"
        def payment = PaymentBuilder.builder()
                .withGuidSource(null)
                .withGuidDestination(null)
                .withSourceAccount("new_account")
                .withDestinationAccount("existing_account")
                .build()
        
        def noViolations = [] as Set

        when: "saving the payment"
        standardizedPaymentService.save(payment)

        then: "should create the missing account"
        1 * validatorMock.validate(payment) >> noViolations
        
        // Destination account exists
        1 * accountServiceMock.account("existing_account") >> Optional.of(new Account(accountType: AccountType.CreditCard))
        
        // Source account missing
        1 * accountServiceMock.account("new_account") >> Optional.empty()
        1 * accountServiceMock.insertAccount(_ as Account) >> { Account a -> 
            assert a.accountNameOwner == "new_account"
            return a 
        }
        
        // Continue with transaction creation
        2 * transactionServiceMock.save(_) >> ServiceResult.Success.of(new finance.domain.Transaction(guid: "guid"))
        1 * paymentRepositoryMock.saveAndFlush(_) >> payment
    }


    def "updatePayment should throw RuntimeException when payment not found"() {
        given: "payment with non-existent ID"
        def payment = PaymentBuilder.builder().withPaymentId(999L).build()

        when: "calling legacy updatePayment with non-existent payment"
        standardizedPaymentService.updatePayment(999L, payment)

        then: "should throw RuntimeException"
        1 * paymentRepositoryMock.findByOwnerAndPaymentId(TEST_OWNER,999L) >> Optional.empty()
        thrown(RuntimeException)
        0 * _
    }
}
