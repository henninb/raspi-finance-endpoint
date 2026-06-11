package finance.services

import finance.configurations.ResilienceComponents

import finance.domain.Account
import finance.domain.ValidationAmount
import finance.domain.ServiceResult
import finance.domain.TransactionState
import finance.helpers.ValidationAmountBuilder
import finance.repositories.ValidationAmountRepository
import finance.repositories.AccountRepository
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException

/**
 * TDD Specification for ValidationAmountService
 * Tests the ValidationAmount service using new ServiceResult pattern with comprehensive error handling
 */
class ValidationAmountServiceSpec extends BaseServiceSpec {

    def validationAmountRepositoryMock = Mock(ValidationAmountRepository)
    def accountRepositoryMock = Mock(AccountRepository)
    def standardizedValidationAmountService = new ValidationAmountService(validationAmountRepositoryMock, accountRepositoryMock, meterService, validatorMock, ResilienceComponents.noOp())

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
        1 * validationAmountRepositoryMock.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> validationAmounts
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
        1 * validationAmountRepositoryMock.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> []
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
        1 * validationAmountRepositoryMock.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, 1L) >> Optional.of(validationAmount)
        result instanceof ServiceResult.Success
        result.data.validationId == 1L
        0 * _
    }

    def "findById should return NotFound when validation amount does not exist"() {
        when: "finding by non-existent ID"
        def result = standardizedValidationAmountService.findById(999L)

        then: "should return NotFound result"
        1 * validationAmountRepositoryMock.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, 999L) >> Optional.empty()
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
        1 * accountRepositoryMock.updateValidationDateForAccountByOwner(_ as Long, TEST_OWNER) >> 1
        result instanceof ServiceResult.Success
        result.data.validationId == 1L
        0 * _
    }

    def "save should return ValidationError when validation amount has constraint violations"() {
        given: "invalid validation amount"
        def validationAmount = ValidationAmountBuilder.builder().withAmount(new BigDecimal("-100.00")).build()
        ConstraintViolation<ValidationAmount> violation = Mock(ConstraintViolation)
        def mockPath = Mock(jakarta.validation.Path)
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
        1 * validationAmountRepositoryMock.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, 1L) >> Optional.of(existingValidationAmount)
        1 * validationAmountRepositoryMock.saveAndFlush(_ as ValidationAmount) >> { ValidationAmount va ->
            assert va.amount == new BigDecimal("200.00")
            return va
        }
        1 * accountRepositoryMock.updateValidationDateForAccountByOwner(_ as Long, TEST_OWNER) >> 1
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
        1 * validationAmountRepositoryMock.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, 999L) >> Optional.empty()
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
        1 * validationAmountRepositoryMock.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, 1L) >> Optional.of(validationAmount)
        1 * validationAmountRepositoryMock.delete(validationAmount)
        result instanceof ServiceResult.Success
        result.data != null
        0 * _
    }

    def "deleteById should return NotFound when validation amount does not exist"() {
        when: "deleting non-existent validation amount"
        def result = standardizedValidationAmountService.deleteById(999L)

        then: "should return NotFound result"
        1 * validationAmountRepositoryMock.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, 999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("ValidationAmount not found: 999")
        0 * _
    }

    // ===== TDD Tests for remaining legacy methods =====

    // ===== TDD Tests for Account-specific method =====

    def "findValidationAmountByAccountNameOwner should return validation amount when found"() {
        given: "existing account and validation amount"
        def account = new Account()
        account.accountId = 1L
        account.accountNameOwner = "testAccount"
        def validationAmount = ValidationAmountBuilder.builder().withAccountId(1L).withTransactionState(TransactionState.Cleared).build()

        when: "finding validation amount by account name owner and transaction state"
        def result = standardizedValidationAmountService.findValidationAmountByAccountNameOwner("testAccount", TransactionState.Cleared)

        then: "should return validation amount"
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "testAccount") >> Optional.of(account)
        1 * validationAmountRepositoryMock.findByOwnerAndTransactionStateAndAccountId(TEST_OWNER, TransactionState.Cleared, 1L) >> [validationAmount]
        result.accountId == 1L
        result.transactionState == TransactionState.Cleared
        0 * _
    }

    def "findValidationAmountByAccountNameOwner should throw RuntimeException when account not found"() {
        when: "finding validation amount for non-existent account"
        standardizedValidationAmountService.findValidationAmountByAccountNameOwner("missingAccount", TransactionState.Cleared)

        then: "should throw RuntimeException"
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "missingAccount") >> Optional.empty()
        thrown(RuntimeException)
        0 * _
    }

    // ===== TDD Test for Latest Clear Amount Bug =====

    def "findValidationAmountByAccountNameOwner should return LATEST validation amount by date when multiple exist"() {
        given: "account with multiple validation amounts at different dates"
        def account = new Account()
        account.accountId = 1L
        account.accountNameOwner = "testAccount"

        // Create validation amounts with different dates - oldest first, newest last
        def oldestDate = new java.sql.Timestamp(System.currentTimeMillis() - 86400000L * 2) // 2 days ago
        def middleDate = new java.sql.Timestamp(System.currentTimeMillis() - 86400000L) // 1 day ago
        def newestDate = new java.sql.Timestamp(System.currentTimeMillis()) // now

        def oldestValidationAmount = ValidationAmountBuilder.builder()
            .withAccountId(1L)
            .withTransactionState(TransactionState.Cleared)
            .withValidationDate(oldestDate)
            .withAmount(new BigDecimal("100.00"))
            .build()

        def middleValidationAmount = ValidationAmountBuilder.builder()
            .withAccountId(1L)
            .withTransactionState(TransactionState.Cleared)
            .withValidationDate(middleDate)
            .withAmount(new BigDecimal("200.00"))
            .build()

        def newestValidationAmount = ValidationAmountBuilder.builder()
            .withAccountId(1L)
            .withTransactionState(TransactionState.Cleared)
            .withValidationDate(newestDate)
            .withAmount(new BigDecimal("300.00"))
            .build()

        when: "finding validation amount by account name owner"
        def result = standardizedValidationAmountService.findValidationAmountByAccountNameOwner("testAccount", TransactionState.Cleared)

        then: "should return the LATEST validation amount by date (not the first one)"
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "testAccount") >> Optional.of(account)
        // Repository returns in database order (oldest first) - this simulates the current behavior
        1 * validationAmountRepositoryMock.findByOwnerAndTransactionStateAndAccountId(TEST_OWNER, TransactionState.Cleared, 1L) >>
            [oldestValidationAmount, middleValidationAmount, newestValidationAmount]

        // This test will FAIL initially because current code returns .first() which is the oldest
        // After fix, it should return the newest validation amount (300.00)
        result.amount == new BigDecimal("300.00")
        result.validationDate == newestDate
        0 * _
    }

    def "findAllActiveFiltered should filter by account and state"() {
        given:
        def account = new Account(accountId: 1L)
        def va1 = new ValidationAmount(accountId: 1L, transactionState: TransactionState.Cleared, activeStatus: true)
        def va2 = new ValidationAmount(accountId: 1L, transactionState: TransactionState.Outstanding, activeStatus: true)
        def va3 = new ValidationAmount(accountId: 2L, transactionState: TransactionState.Cleared, activeStatus: true)

        when:
        def result = standardizedValidationAmountService.findAllActiveFiltered("acc1", TransactionState.Cleared)

        then:
        1 * validationAmountRepositoryMock.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> [va1, va2, va3]
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "acc1") >> Optional.of(account)
        result instanceof ServiceResult.Success
        result.data.size() == 1
        result.data[0].accountId == 1L
        result.data[0].transactionState == TransactionState.Cleared
    }

    def "findAllActiveFiltered should return empty list if account not found"() {
        when:
        def result = standardizedValidationAmountService.findAllActiveFiltered("missing", null)

        then:
        1 * validationAmountRepositoryMock.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> []
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "missing") >> Optional.empty()
        result instanceof ServiceResult.Success
        result.data.isEmpty()
    }

    def "insertValidationAmount should resolve accountId and save"() {
        given:
        def account = new Account(accountId: 5L)
        def va = new ValidationAmount(amount: 100.0G)

        when:
        def result = standardizedValidationAmountService.insertValidationAmount("acc1", va)

        then:
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "acc1") >> Optional.of(account)
        1 * validatorMock.validate(va) >> ([] as Set)
        1 * validationAmountRepositoryMock.saveAndFlush(va) >> va
        result.accountId == 5L
    }

    def "insertValidationAmount should throw when account not found by name and no accountId provided"() {
        given:
        def va = new ValidationAmount(amount: 50.0G)

        when:
        standardizedValidationAmountService.insertValidationAmount("unknown_account", va)

        then:
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "unknown_account") >> Optional.empty()
        thrown(org.springframework.web.server.ResponseStatusException)
    }

    def "findAllActiveFiltered should return all active when called with null filters"() {
        given:
        def va1 = new ValidationAmount(accountId: 1L, transactionState: TransactionState.Cleared, activeStatus: true)
        def va2 = new ValidationAmount(accountId: 2L, transactionState: TransactionState.Outstanding, activeStatus: true)

        when:
        def result = standardizedValidationAmountService.findAllActiveFiltered(null, null)

        then:
        1 * validationAmountRepositoryMock.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> [va1, va2]
        result instanceof ServiceResult.Success
        result.data.size() == 2
    }

    def "findAllActiveFiltered should filter by transactionState only when accountNameOwner is null"() {
        given:
        def va1 = new ValidationAmount(accountId: 1L, transactionState: TransactionState.Cleared, activeStatus: true)
        def va2 = new ValidationAmount(accountId: 2L, transactionState: TransactionState.Outstanding, activeStatus: true)

        when:
        def result = standardizedValidationAmountService.findAllActiveFiltered(null, TransactionState.Cleared)

        then:
        1 * validationAmountRepositoryMock.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> [va1, va2]
        result instanceof ServiceResult.Success
        result.data.size() == 1
        result.data[0].transactionState == TransactionState.Cleared
    }

    // ===== Edge cases: save SystemError =====

    def "save should return SystemError when repository throws RuntimeException"() {
        given:
        def validationAmount = ValidationAmountBuilder.builder().build()
        Set<ConstraintViolation<ValidationAmount>> noViolations = [] as Set

        when:
        def result = standardizedValidationAmountService.save(validationAmount)

        then:
        1 * validatorMock.validate(validationAmount) >> noViolations
        1 * validationAmountRepositoryMock.saveAndFlush(validationAmount) >> { throw new RuntimeException("db failure") }
        result instanceof ServiceResult.SystemError
        0 * _
    }

    // ===== Edge cases: update error paths =====

    def "update should return ValidationError when saveAndFlush throws ConstraintViolationException"() {
        given:
        def existing = ValidationAmountBuilder.builder().withValidationId(1L).build()
        def patch = ValidationAmountBuilder.builder().withValidationId(1L).withAmount(new BigDecimal("-5.00")).build()
        ConstraintViolation<ValidationAmount> violation = Mock(ConstraintViolation)
        def mockPath = Mock(jakarta.validation.Path)
        mockPath.toString() >> "amount"
        violation.propertyPath >> mockPath
        violation.message >> "must be dollar precision"
        Set<ConstraintViolation<ValidationAmount>> violations = [violation] as Set

        when:
        def result = standardizedValidationAmountService.update(patch)

        then:
        1 * validationAmountRepositoryMock.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, 1L) >> Optional.of(existing)
        1 * validationAmountRepositoryMock.saveAndFlush(_ as ValidationAmount) >> { throw new ConstraintViolationException("bad amount", violations) }
        result instanceof ServiceResult.ValidationError
        // extractValidationErrors calls violation.propertyPath internally — no strict 0 * _ constraint here
    }

    def "update should return BusinessError when saveAndFlush throws DataIntegrityViolationException"() {
        given:
        def existing = ValidationAmountBuilder.builder().withValidationId(5L).build()
        def patch = ValidationAmountBuilder.builder().withValidationId(5L).build()

        when:
        def result = standardizedValidationAmountService.update(patch)

        then:
        1 * validationAmountRepositoryMock.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, 5L) >> Optional.of(existing)
        1 * validationAmountRepositoryMock.saveAndFlush(_ as ValidationAmount) >> { throw new DataIntegrityViolationException("duplicate key") }
        result instanceof ServiceResult.BusinessError
        result.errorCode == "DATA_INTEGRITY_VIOLATION"
        0 * _
    }

    def "update should return SystemError when saveAndFlush throws RuntimeException"() {
        given:
        def existing = ValidationAmountBuilder.builder().withValidationId(6L).build()
        def patch = ValidationAmountBuilder.builder().withValidationId(6L).build()

        when:
        def result = standardizedValidationAmountService.update(patch)

        then:
        1 * validationAmountRepositoryMock.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, 6L) >> Optional.of(existing)
        1 * validationAmountRepositoryMock.saveAndFlush(_ as ValidationAmount) >> { throw new RuntimeException("db error") }
        result instanceof ServiceResult.SystemError
        0 * _
    }

    // ===== Edge cases: deleteById SystemError =====

    def "deleteById should return SystemError when repository delete throws RuntimeException"() {
        given:
        def existing = ValidationAmountBuilder.builder().withValidationId(7L).build()

        when:
        def result = standardizedValidationAmountService.deleteById(7L)

        then:
        1 * validationAmountRepositoryMock.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, 7L) >> Optional.of(existing)
        1 * validationAmountRepositoryMock.delete(existing) >> { throw new RuntimeException("delete failure") }
        result instanceof ServiceResult.SystemError
        0 * _
    }

    // ===== Edge cases: findById SystemError =====

    def "findById should return SystemError when repository throws RuntimeException"() {
        when:
        def result = standardizedValidationAmountService.findById(8L)

        then:
        1 * validationAmountRepositoryMock.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, 8L) >> { throw new RuntimeException("db error") }
        result instanceof ServiceResult.SystemError
        0 * _
    }

    // ===== Edge cases: findValidationAmountByAccountNameOwner — account found, no records =====

    def "findValidationAmountByAccountNameOwner should throw EntityNotFoundException when account exists but no matching validation amounts"() {
        given:
        def account = new Account()
        account.accountId = 1L
        account.accountNameOwner = "emptyAccount"

        when:
        standardizedValidationAmountService.findValidationAmountByAccountNameOwner("emptyAccount", TransactionState.Cleared)

        then:
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "emptyAccount") >> Optional.of(account)
        1 * validationAmountRepositoryMock.findByOwnerAndTransactionStateAndAccountId(TEST_OWNER, TransactionState.Cleared, 1L) >> []
        thrown(jakarta.persistence.EntityNotFoundException)
        0 * _
    }

    // ===== Edge cases: insertValidationAmount — payload accountId skips lookup =====

    def "insertValidationAmount should use payload accountId when greater than zero without calling account lookup"() {
        given:
        def va = new ValidationAmount(accountId: 9L, amount: new BigDecimal("100.00"))

        when:
        def result = standardizedValidationAmountService.insertValidationAmount("anyAccountName", va)

        then:
        0 * accountRepositoryMock.findByOwnerAndAccountNameOwner(_, _)
        1 * validatorMock.validate(va) >> ([] as Set)
        1 * validationAmountRepositoryMock.saveAndFlush(va) >> va
        // after save, service tries to refresh account validation_date — allow it
        _ * accountRepositoryMock.updateValidationDateForAccountByOwner(9L, TEST_OWNER) >> 1
        result.accountId == 9L
    }

    def "insertValidationAmount should throw ValidationException when save returns ValidationError"() {
        given:
        def va = new ValidationAmount(accountId: 5L, amount: new BigDecimal("-1.00"))
        ConstraintViolation<ValidationAmount> violation = Mock(ConstraintViolation)
        def mockPath = Mock(jakarta.validation.Path)
        mockPath.toString() >> "amount"
        violation.propertyPath >> mockPath
        violation.message >> "must be dollar precision"
        Set<ConstraintViolation<ValidationAmount>> violations = [violation] as Set

        when:
        standardizedValidationAmountService.insertValidationAmount("acc1", va)

        then:
        1 * validatorMock.validate(va) >> { throw new ConstraintViolationException("Validation failed", violations) }
        thrown(jakarta.validation.ValidationException)
    }
}
