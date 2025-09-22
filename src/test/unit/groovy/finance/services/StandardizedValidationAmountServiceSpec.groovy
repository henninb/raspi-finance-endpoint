package finance.services

import finance.domain.ValidationAmount
import finance.domain.ServiceResult
import finance.domain.TransactionState
import finance.helpers.ValidationAmountBuilder
import finance.repositories.ValidationAmountRepository
import finance.repositories.AccountRepository
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import jakarta.persistence.EntityNotFoundException
import java.math.BigDecimal

/**
 * TDD Specification for StandardizedValidationAmountService
 * Tests the ValidationAmount service using new ServiceResult pattern with comprehensive error handling
 */
class StandardizedValidationAmountServiceSpec extends BaseServiceSpec {

    def validationAmountRepositoryMock = Mock(ValidationAmountRepository)
    def accountRepositoryMock = Mock(AccountRepository)
    def standardizedValidationAmountService = new StandardizedValidationAmountService(validationAmountRepositoryMock, accountRepositoryMock)

    void setup() {
        standardizedValidationAmountService.meterService = meterService
        standardizedValidationAmountService.validator = validatorMock
    }

    // ===== TDD Tests for findAllActive() =====

    def "findAllActive should return Success with validation amounts when found"() {
        given: "existing active validation amounts"
        def validationAmounts = [
            ValidationAmountBuilder.builder().withValidationId(1L).withAccountId(100L).withAmount(new BigDecimal("150.00")).build(),
            ValidationAmountBuilder.builder().withValidationId(2L).withAccountId(200L).withAmount(new BigDecimal("250.00")).build()
        ]

        when: "finding all active validation amounts"
        def result = standardizedValidationAmountService.findAllActive()

        then: "should return Success with validation amounts"
        1 * validationAmountRepositoryMock.findByActiveStatusTrueOrderByValidationDateDesc() >> validationAmounts
        result instanceof ServiceResult.Success
        result.data.size() == 2
        result.data[0].validationId == 1L
        result.data[0].amount == new BigDecimal("150.00")
        result.data[1].validationId == 2L
        result.data[1].amount == new BigDecimal("250.00")
        0 * _
    }

    def "findAllActive should return Success with empty list when no validation amounts found"() {
        when: "finding all active validation amounts with none existing"
        def result = standardizedValidationAmountService.findAllActive()

        then: "should return Success with empty list"
        1 * validationAmountRepositoryMock.findByActiveStatusTrueOrderByValidationDateDesc() >> []
        result instanceof ServiceResult.Success
        result.data.isEmpty()
        0 * _
    }

    // ===== TDD Tests for findById() =====

    def "findById should return Success with validation amount when found"() {
        given: "existing validation amount"
        def validationAmount = ValidationAmountBuilder.builder().withValidationId(1L).build()

        when: "finding by valid ID"
        def result = standardizedValidationAmountService.findById(1L)

        then: "should return Success with validation amount"
        1 * validationAmountRepositoryMock.findByValidationIdAndActiveStatusTrue(1L) >> Optional.of(validationAmount)
        result instanceof ServiceResult.Success
        result.data.validationId == 1L
        0 * _
    }

    def "findById should return NotFound when validation amount does not exist"() {
        when: "finding by non-existent ID"
        def result = standardizedValidationAmountService.findById(999L)

        then: "should return NotFound result"
        1 * validationAmountRepositoryMock.findByValidationIdAndActiveStatusTrue(999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("ValidationAmount not found: 999")
        0 * _
    }

    // ===== TDD Tests for save() =====

    def "save should return Success with saved validation amount when valid"() {
        given: "valid validation amount"
        def validationAmount = ValidationAmountBuilder.builder().build()
        def savedValidationAmount = ValidationAmountBuilder.builder().withValidationId(1L).build()
        Set<ConstraintViolation<ValidationAmount>> noViolations = [] as Set

        when: "saving validation amount"
        def result = standardizedValidationAmountService.save(validationAmount)

        then: "should return Success with saved validation amount"
        1 * validatorMock.validate(validationAmount) >> noViolations
        1 * validationAmountRepositoryMock.saveAndFlush(validationAmount) >> savedValidationAmount
        result instanceof ServiceResult.Success
        result.data.validationId == 1L
        0 * _
    }

    def "save should return ValidationError when validation amount has constraint violations"() {
        given: "invalid validation amount"
        def validationAmount = ValidationAmountBuilder.builder().withAmount(new BigDecimal("-100.00")).build()
        ConstraintViolation<ValidationAmount> violation = Mock(ConstraintViolation)
        def mockPath = Mock(javax.validation.Path)
        mockPath.toString() >> "amount"
        violation.propertyPath >> mockPath
        violation.message >> "must be greater than or equal to 0"
        Set<ConstraintViolation<ValidationAmount>> violations = [violation] as Set

        when: "saving invalid validation amount"
        def result = standardizedValidationAmountService.save(validationAmount)

        then: "should return ValidationError result"
        1 * validatorMock.validate(validationAmount) >> { throw new ConstraintViolationException("Validation failed", violations) }
        result instanceof ServiceResult.ValidationError
        result.errors.size() == 1
        result.errors.values().contains("must be greater than or equal to 0")
    }

    def "save should return BusinessError when duplicate validation amount exists"() {
        given: "validation amount that will cause duplicate key violation"
        def validationAmount = ValidationAmountBuilder.builder().build()
        Set<ConstraintViolation<ValidationAmount>> noViolations = [] as Set

        when: "saving duplicate validation amount"
        def result = standardizedValidationAmountService.save(validationAmount)

        then: "should return BusinessError result"
        1 * validatorMock.validate(validationAmount) >> noViolations
        1 * validationAmountRepositoryMock.saveAndFlush(validationAmount) >> {
            throw new DataIntegrityViolationException("Duplicate entry")
        }
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("data integrity")
        result.errorCode == "DATA_INTEGRITY_VIOLATION"
        0 * _
    }

    // ===== TDD Tests for update() =====

    def "update should return Success with updated validation amount when exists"() {
        given: "existing validation amount to update"
        def existingValidationAmount = ValidationAmountBuilder.builder().withValidationId(1L).withAmount(new BigDecimal("100.00")).build()
        def updatedValidationAmount = ValidationAmountBuilder.builder().withValidationId(1L).withAmount(new BigDecimal("200.00")).build()

        when: "updating existing validation amount"
        def result = standardizedValidationAmountService.update(updatedValidationAmount)

        then: "should return Success with updated validation amount"
        1 * validationAmountRepositoryMock.findByValidationIdAndActiveStatusTrue(1L) >> Optional.of(existingValidationAmount)
        1 * validationAmountRepositoryMock.saveAndFlush(_ as ValidationAmount) >> { ValidationAmount va ->
            assert va.amount == new BigDecimal("200.00")
            return va
        }
        result instanceof ServiceResult.Success
        result.data.amount == new BigDecimal("200.00")
        0 * _
    }

    def "update should return NotFound when validation amount does not exist"() {
        given: "validation amount with non-existent ID"
        def validationAmount = ValidationAmountBuilder.builder().withValidationId(999L).build()

        when: "updating non-existent validation amount"
        def result = standardizedValidationAmountService.update(validationAmount)

        then: "should return NotFound result"
        1 * validationAmountRepositoryMock.findByValidationIdAndActiveStatusTrue(999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("ValidationAmount not found: 999")
        0 * _
    }

    // ===== TDD Tests for deleteById() =====

    def "deleteById should return Success when validation amount exists"() {
        given: "existing validation amount"
        def validationAmount = ValidationAmountBuilder.builder().withValidationId(1L).build()

        when: "deleting existing validation amount"
        def result = standardizedValidationAmountService.deleteById(1L)

        then: "should return Success"
        1 * validationAmountRepositoryMock.findByValidationIdAndActiveStatusTrue(1L) >> Optional.of(validationAmount)
        1 * validationAmountRepositoryMock.delete(validationAmount)
        result instanceof ServiceResult.Success
        result.data == true
        0 * _
    }

    def "deleteById should return NotFound when validation amount does not exist"() {
        when: "deleting non-existent validation amount"
        def result = standardizedValidationAmountService.deleteById(999L)

        then: "should return NotFound result"
        1 * validationAmountRepositoryMock.findByValidationIdAndActiveStatusTrue(999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("ValidationAmount not found: 999")
        0 * _
    }

    // ===== TDD Tests for remaining legacy methods =====

    // ===== TDD Tests for Account-specific method =====

    def "findValidationAmountByAccountNameOwner should return validation amount when found"() {
        given: "existing account and validation amount"
        def account = new finance.domain.Account()
        account.accountId = 1L
        account.accountNameOwner = "testAccount"
        def validationAmount = ValidationAmountBuilder.builder().withAccountId(1L).withTransactionState(TransactionState.Cleared).build()

        when: "finding validation amount by account name owner and transaction state"
        def result = standardizedValidationAmountService.findValidationAmountByAccountNameOwner("testAccount", TransactionState.Cleared)

        then: "should return validation amount"
        1 * accountRepositoryMock.findByAccountNameOwner("testAccount") >> Optional.of(account)
        1 * validationAmountRepositoryMock.findByTransactionStateAndAccountId(TransactionState.Cleared, 1L) >> [validationAmount]
        result.accountId == 1L
        result.transactionState == TransactionState.Cleared
        0 * _
    }

    def "findValidationAmountByAccountNameOwner should throw RuntimeException when account not found"() {
        when: "finding validation amount for non-existent account"
        standardizedValidationAmountService.findValidationAmountByAccountNameOwner("missingAccount", TransactionState.Cleared)

        then: "should throw RuntimeException"
        1 * accountRepositoryMock.findByAccountNameOwner("missingAccount") >> Optional.empty()
        thrown(RuntimeException)
        0 * _
    }
}