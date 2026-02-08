package finance.services

import finance.domain.PendingTransaction
import finance.domain.ServiceResult
import finance.repositories.PendingTransactionRepository
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification
import spock.lang.Subject

import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate

class PendingTransactionServiceSpec extends Specification {

    protected static final String TEST_OWNER = "test_owner"

    PendingTransactionRepository mockPendingTransactionRepository = Mock()
    Validator mockValidator = Mock()

    @Subject
    PendingTransactionService standardizedPendingTransactionService

    def setup() {
        def auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(TEST_OWNER, "password")
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth)
        standardizedPendingTransactionService = new PendingTransactionService(mockPendingTransactionRepository)
        standardizedPendingTransactionService.validator = mockValidator
    }

    def cleanup() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext()
    }

    // ===== Test Data Builders =====

    PendingTransaction createTestPendingTransaction() {
        return new PendingTransaction(
                1L,
                "test_account",
                LocalDate.parse("2023-01-01"),
                "Test pending transaction",
                new BigDecimal("100.00"),
                "pending",
                "test_owner",
                null
        )
    }

    PendingTransaction createTestPendingTransactionWithoutId() {
        return new PendingTransaction(
                0L,
                "test_account",
                LocalDate.parse("2023-01-01"),
                "Test pending transaction",
                new BigDecimal("100.00"),
                "pending",
                "test_owner",
                null
        )
    }

    // ===== findAllActive Tests =====

    def "findAllActive should return Success with list of pending transactions"() {
        given: "a list of pending transactions"
        def pendingTransactions = [createTestPendingTransaction()]

        when: "findAllActive is called"
        def result = standardizedPendingTransactionService.findAllActive()

        then: "repository findAll is called"
        1 * mockPendingTransactionRepository.findAllByOwner(TEST_OWNER) >> pendingTransactions

        and: "result is Success with pending transactions"
        result instanceof ServiceResult.Success
        result.data == pendingTransactions
    }

    def "findAllActive should return Success with empty list when no pending transactions exist"() {
        given: "no pending transactions"
        def emptyList = []

        when: "findAllActive is called"
        def result = standardizedPendingTransactionService.findAllActive()

        then: "repository findAll is called"
        1 * mockPendingTransactionRepository.findAllByOwner(TEST_OWNER) >> emptyList

        and: "result is Success with empty list"
        result instanceof ServiceResult.Success
        result.data == emptyList
    }

    // ===== findById Tests =====

    def "findById should return Success when pending transaction exists"() {
        given: "a pending transaction ID and existing pending transaction"
        def pendingTransactionId = 1L
        def pendingTransaction = createTestPendingTransaction()

        when: "findById is called"
        def result = standardizedPendingTransactionService.findById(pendingTransactionId)

        then: "repository findByPendingTransactionIdOrderByTransactionDateDesc is called"
        1 * mockPendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(TEST_OWNER, pendingTransactionId) >> Optional.of(pendingTransaction)

        and: "result is Success with pending transaction"
        result instanceof ServiceResult.Success
        result.data == pendingTransaction
    }

    def "findById should return NotFound when pending transaction does not exist"() {
        given: "a pending transaction ID that doesn't exist"
        def pendingTransactionId = 999L

        when: "findById is called"
        def result = standardizedPendingTransactionService.findById(pendingTransactionId)

        then: "repository findByPendingTransactionIdOrderByTransactionDateDesc is called"
        1 * mockPendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(TEST_OWNER, pendingTransactionId) >> Optional.empty()

        and: "result is NotFound"
        result instanceof ServiceResult.NotFound
        result.message.contains("PendingTransaction not found: 999")
    }

    // ===== save Tests =====

    def "save should return Success when pending transaction is valid"() {
        given: "a valid pending transaction"
        def pendingTransaction = createTestPendingTransactionWithoutId()
        def savedTransaction = createTestPendingTransaction()

        when: "save is called"
        def result = standardizedPendingTransactionService.save(pendingTransaction)

        then: "validation is successful"
        1 * mockValidator.validate(pendingTransaction) >> Collections.emptySet()

        and: "repository saveAndFlush is called"
        1 * mockPendingTransactionRepository.saveAndFlush(pendingTransaction) >> savedTransaction

        and: "result is Success with saved pending transaction"
        result instanceof ServiceResult.Success
        result.data == savedTransaction
    }

    def "save should set dateAdded when creating new transaction"() {
        given: "a pending transaction for creation"
        def pendingTransaction = createTestPendingTransactionWithoutId()
        def savedTransaction = createTestPendingTransaction()

        when: "save is called"
        def result = standardizedPendingTransactionService.save(pendingTransaction)

        then: "validation is successful"
        1 * mockValidator.validate(pendingTransaction) >> Collections.emptySet()

        and: "repository saveAndFlush is called"
        1 * mockPendingTransactionRepository.saveAndFlush(pendingTransaction) >> savedTransaction

        and: "dateAdded is set (within last 5 seconds)"
        def now = new Timestamp(System.currentTimeMillis())
        def timeDiff = Math.abs(now.time - pendingTransaction.dateAdded.time)
        timeDiff < 5000 // within 5 seconds

        and: "result is Success"
        result instanceof ServiceResult.Success
    }

    def "save should return ValidationError when pending transaction is invalid"() {
        given: "an invalid pending transaction"
        def pendingTransaction = createTestPendingTransactionWithoutId()
        def violation = Mock(ConstraintViolation)
        violation.propertyPath >> Mock(jakarta.validation.Path) {
            toString() >> "accountNameOwner"
        }
        violation.message >> "Account name owner is required"

        when: "save is called"
        def result = standardizedPendingTransactionService.save(pendingTransaction)

        then: "validation fails"
        1 * mockValidator.validate(pendingTransaction) >> Set.of(violation)

        and: "result is ValidationError"
        result instanceof ServiceResult.ValidationError
        result.errors.containsKey("accountNameOwner")
    }

    def "save should return BusinessError when data integrity violation occurs"() {
        given: "a pending transaction that causes data integrity violation"
        def pendingTransaction = createTestPendingTransactionWithoutId()

        when: "save is called"
        def result = standardizedPendingTransactionService.save(pendingTransaction)

        then: "validation is successful"
        1 * mockValidator.validate(pendingTransaction) >> Collections.emptySet()

        and: "repository saveAndFlush throws DataIntegrityViolationException"
        1 * mockPendingTransactionRepository.saveAndFlush(pendingTransaction) >> { throw new DataIntegrityViolationException("Duplicate entry") }

        and: "result is BusinessError"
        result instanceof ServiceResult.BusinessError
        result.message.contains("Data integrity violation")
        result.errorCode == "DATA_INTEGRITY_VIOLATION"
    }

    // ===== update Tests =====

    def "update should return Success when pending transaction exists and is valid"() {
        given: "an existing pending transaction and updated data"
        def existingTransaction = createTestPendingTransaction()
        def updatedTransaction = createTestPendingTransaction()
        updatedTransaction.description = "Updated description"

        when: "update is called"
        def result = standardizedPendingTransactionService.update(updatedTransaction)

        then: "repository findByPendingTransactionIdOrderByTransactionDateDesc is called"
        1 * mockPendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(TEST_OWNER, updatedTransaction.pendingTransactionId) >> Optional.of(existingTransaction)

        and: "repository saveAndFlush is called"
        1 * mockPendingTransactionRepository.saveAndFlush(existingTransaction) >> existingTransaction

        and: "existing transaction is updated"
        existingTransaction.description == "Updated description"

        and: "result is Success"
        result instanceof ServiceResult.Success
        result.data == existingTransaction
    }

    def "update should return NotFound when pending transaction does not exist"() {
        given: "a pending transaction that doesn't exist"
        def pendingTransaction = createTestPendingTransaction()
        pendingTransaction.pendingTransactionId = 999L

        when: "update is called"
        def result = standardizedPendingTransactionService.update(pendingTransaction)

        then: "repository findByPendingTransactionIdOrderByTransactionDateDesc is called"
        1 * mockPendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(TEST_OWNER, 999L) >> Optional.empty()

        and: "result is NotFound"
        result instanceof ServiceResult.NotFound
        result.message.contains("PendingTransaction not found: 999")
    }

    // ===== deleteById Tests =====

    def "deleteById should return Success when pending transaction exists"() {
        given: "an existing pending transaction"
        def pendingTransactionId = 1L
        def pendingTransaction = createTestPendingTransaction()

        when: "deleteById is called"
        def result = standardizedPendingTransactionService.deleteById(pendingTransactionId)

        then: "repository findByPendingTransactionIdOrderByTransactionDateDesc is called"
        1 * mockPendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(TEST_OWNER, pendingTransactionId) >> Optional.of(pendingTransaction)

        and: "repository delete is called"
        1 * mockPendingTransactionRepository.delete(pendingTransaction)

        and: "result is Success with true"
        result instanceof ServiceResult.Success
        result.data == true
    }

    def "deleteById should return NotFound when pending transaction does not exist"() {
        given: "a pending transaction ID that doesn't exist"
        def pendingTransactionId = 999L

        when: "deleteById is called"
        def result = standardizedPendingTransactionService.deleteById(pendingTransactionId)

        then: "repository findByPendingTransactionIdOrderByTransactionDateDesc is called"
        1 * mockPendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(TEST_OWNER, pendingTransactionId) >> Optional.empty()

        and: "result is NotFound"
        result instanceof ServiceResult.NotFound
        result.message.contains("PendingTransaction not found: 999")
    }

    // ===== deleteAll Tests =====

    def "deleteAll should return Success"() {
        when: "deleteAll is called"
        def result = standardizedPendingTransactionService.deleteAll()

        then: "repository deleteAll is called"
        1 * mockPendingTransactionRepository.deleteAllByOwner(TEST_OWNER)

        and: "result is Success with true"
        result instanceof ServiceResult.Success
        result.data == true
    }

    // ===== Legacy Method Compatibility Tests =====

    def "insertPendingTransaction should delegate to save and return pending transaction"() {
        given: "a valid pending transaction"
        def pendingTransaction = createTestPendingTransactionWithoutId()
        def savedTransaction = createTestPendingTransaction()

        when: "insertPendingTransaction is called"
        def result = standardizedPendingTransactionService.insertPendingTransaction(pendingTransaction)

        then: "validation is successful"
        1 * mockValidator.validate(pendingTransaction) >> Collections.emptySet()

        and: "repository saveAndFlush is called"
        1 * mockPendingTransactionRepository.saveAndFlush(pendingTransaction) >> savedTransaction

        and: "result is the saved pending transaction"
        result == savedTransaction
    }

    def "insertPendingTransaction should throw ValidationException on validation failure"() {
        given: "an invalid pending transaction"
        def pendingTransaction = createTestPendingTransactionWithoutId()
        def violation = Mock(ConstraintViolation)
        violation.propertyPath >> Mock(jakarta.validation.Path) {
            toString() >> "accountNameOwner"
        }
        violation.message >> "Account name owner is required"

        when: "insertPendingTransaction is called"
        standardizedPendingTransactionService.insertPendingTransaction(pendingTransaction)

        then: "validation fails"
        1 * mockValidator.validate(pendingTransaction) >> Set.of(violation)

        and: "ValidationException is thrown"
        thrown(jakarta.validation.ValidationException)
    }

    def "deletePendingTransaction should delegate to deleteById and return true"() {
        given: "an existing pending transaction"
        def pendingTransactionId = 1L
        def pendingTransaction = createTestPendingTransaction()

        when: "deletePendingTransaction is called"
        def result = standardizedPendingTransactionService.deletePendingTransaction(pendingTransactionId)

        then: "repository findByPendingTransactionIdOrderByTransactionDateDesc is called"
        1 * mockPendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(TEST_OWNER, pendingTransactionId) >> Optional.of(pendingTransaction)

        and: "repository delete is called"
        1 * mockPendingTransactionRepository.delete(pendingTransaction)

        and: "result is true"
        result == true
    }

    def "deletePendingTransaction should throw ResponseStatusException when pending transaction not found"() {
        given: "a pending transaction ID that doesn't exist"
        def pendingTransactionId = 999L

        when: "deletePendingTransaction is called"
        standardizedPendingTransactionService.deletePendingTransaction(pendingTransactionId)

        then: "repository findByPendingTransactionIdOrderByTransactionDateDesc is called"
        1 * mockPendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(TEST_OWNER, pendingTransactionId) >> Optional.empty()

        and: "ResponseStatusException is thrown"
        thrown(org.springframework.web.server.ResponseStatusException)
    }

    def "getAllPendingTransactions should delegate to findAllActive and return list"() {
        given: "a list of pending transactions"
        def pendingTransactions = [createTestPendingTransaction()]

        when: "getAllPendingTransactions is called"
        def result = standardizedPendingTransactionService.getAllPendingTransactions()

        then: "repository findAll is called"
        1 * mockPendingTransactionRepository.findAllByOwner(TEST_OWNER) >> pendingTransactions

        and: "result is the list of pending transactions"
        result == pendingTransactions
    }

    def "deleteAllPendingTransactions should delegate to deleteAll and return true"() {
        when: "deleteAllPendingTransactions is called"
        def result = standardizedPendingTransactionService.deleteAllPendingTransactions()

        then: "repository deleteAll is called"
        1 * mockPendingTransactionRepository.deleteAllByOwner(TEST_OWNER)

        and: "result is true"
        result == true
    }

    def "findByPendingTransactionId should delegate to findById and return Optional"() {
        given: "an existing pending transaction"
        def pendingTransactionId = 1L
        def pendingTransaction = createTestPendingTransaction()

        when: "findByPendingTransactionId is called"
        def result = standardizedPendingTransactionService.findByPendingTransactionId(pendingTransactionId)

        then: "repository findByPendingTransactionIdOrderByTransactionDateDesc is called"
        1 * mockPendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(TEST_OWNER, pendingTransactionId) >> Optional.of(pendingTransaction)

        and: "result is Optional with pending transaction"
        result.isPresent()
        result.get() == pendingTransaction
    }

    def "findByPendingTransactionId should return empty Optional when pending transaction not found"() {
        given: "a pending transaction ID that doesn't exist"
        def pendingTransactionId = 999L

        when: "findByPendingTransactionId is called"
        def result = standardizedPendingTransactionService.findByPendingTransactionId(pendingTransactionId)

        then: "repository findByPendingTransactionIdOrderByTransactionDateDesc is called"
        1 * mockPendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(TEST_OWNER, pendingTransactionId) >> Optional.empty()

        and: "result is empty Optional"
        result.isEmpty()
    }

    def "updatePendingTransaction should delegate to update and return pending transaction"() {
        given: "an existing pending transaction and updated data"
        def existingTransaction = createTestPendingTransaction()
        def updatedTransaction = createTestPendingTransaction()
        updatedTransaction.description = "Updated description"

        when: "updatePendingTransaction is called"
        def result = standardizedPendingTransactionService.updatePendingTransaction(updatedTransaction)

        then: "repository findByPendingTransactionIdOrderByTransactionDateDesc is called"
        1 * mockPendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(TEST_OWNER, updatedTransaction.pendingTransactionId) >> Optional.of(existingTransaction)

        and: "repository saveAndFlush is called"
        1 * mockPendingTransactionRepository.saveAndFlush(existingTransaction) >> existingTransaction

        and: "result is the updated pending transaction"
        result == existingTransaction
    }

    def "updatePendingTransaction should throw RuntimeException when pending transaction not found"() {
        given: "a pending transaction that doesn't exist"
        def pendingTransaction = createTestPendingTransaction()
        pendingTransaction.pendingTransactionId = 999L

        when: "updatePendingTransaction is called"
        standardizedPendingTransactionService.updatePendingTransaction(pendingTransaction)

        then: "repository findByPendingTransactionIdOrderByTransactionDateDesc is called"
        1 * mockPendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(TEST_OWNER, 999L) >> Optional.empty()

        and: "RuntimeException is thrown"
        thrown(RuntimeException)
    }
}
