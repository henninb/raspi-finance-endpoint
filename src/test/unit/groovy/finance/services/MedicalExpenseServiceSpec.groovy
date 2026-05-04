package finance.services
import finance.configurations.ResilienceComponents

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.domain.ServiceResult
import finance.exceptions.DuplicateMedicalExpenseException
import finance.helpers.MedicalExpenseBuilder
import finance.repositories.MedicalExpenseRepository
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal

/**
 * TDD Test Specification for MedicalExpenseService
 * Following the established ServiceResult pattern and TDD methodology
 */
class MedicalExpenseServiceSpec extends BaseServiceSpec {

    def medicalExpenseRepositoryMock = Mock(MedicalExpenseRepository)
    def standardizedMedicalExpenseService = new MedicalExpenseService(medicalExpenseRepositoryMock, meterService, validator, ResilienceComponents.noOp())

    def "should have correct entity name"() {
        expect:
        standardizedMedicalExpenseService.getEntityName() == "MedicalExpense"
    }

    // ===== findAllActive Tests =====

    def "findAllActive should return ServiceResult.Success with list of medical expenses"() {
        given:
        def expense1 = MedicalExpenseBuilder.builder().withMedicalExpenseId(1L).build()
        def expense2 = MedicalExpenseBuilder.builder().withMedicalExpenseId(2L).build()
        def expenses = [expense1, expense2]

        when:
        medicalExpenseRepositoryMock.findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(TEST_OWNER) >> expenses
        def result = standardizedMedicalExpenseService.findAllActive()

        then:
        result instanceof ServiceResult.Success
        result.data.size() == 2
        result.data == expenses
    }

    def "findAllActive should return ServiceResult.Success with empty list when no expenses"() {
        given:
        medicalExpenseRepositoryMock.findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(TEST_OWNER) >> []

        when:
        def result = standardizedMedicalExpenseService.findAllActive()

        then:
        result instanceof ServiceResult.Success
        result.data.isEmpty()
    }

    def "findAllActive should return ServiceResult.SystemError on repository exception"() {
        given:
        medicalExpenseRepositoryMock.findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(TEST_OWNER) >> { throw new RuntimeException("Database error") }

        when:
        def result = standardizedMedicalExpenseService.findAllActive()

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Database error")
    }

    // ===== findById Tests =====

    def "findById should return ServiceResult.Success when medical expense exists"() {
        given:
        def expenseId = 1L
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(expenseId).build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, expenseId) >> expense
        def result = standardizedMedicalExpenseService.findById(expenseId)

        then:
        result instanceof ServiceResult.Success
        result.data == expense
    }

    def "findById should return ServiceResult.NotFound when medical expense does not exist"() {
        given:
        def expenseId = 999L

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, expenseId) >> null
        def result = standardizedMedicalExpenseService.findById(expenseId)

        then:
        result instanceof ServiceResult.NotFound
        result.message == "MedicalExpense not found: 999"
    }

    def "findById should return ServiceResult.SystemError on repository exception"() {
        given:
        def expenseId = 1L
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, expenseId) >> { throw new RuntimeException("Database connection failed") }

        when:
        def result = standardizedMedicalExpenseService.findById(expenseId)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Database connection failed")
    }

    // ===== save Tests =====

    def "save should return ServiceResult.Success when valid medical expense"() {
        given:
        def expense = MedicalExpenseBuilder.builder()
                .withMedicalExpenseId(0L)
                .withTransactionId(null)
                .build()
        def savedExpense = MedicalExpenseBuilder.builder().withMedicalExpenseId(1L).build()

        when:
        medicalExpenseRepositoryMock.save(expense) >> savedExpense
        def result = standardizedMedicalExpenseService.save(expense)

        then:
        result instanceof ServiceResult.Success
        result.data == savedExpense
    }

    def "save should return ServiceResult.ValidationError on constraint violation"() {
        given:
        def expense = MedicalExpenseBuilder.builder()
                .withAmount(new BigDecimal("-100.00")) // Invalid negative amount
                .build()

        // Mock validation to return constraint violations
        def violation = Mock(jakarta.validation.ConstraintViolation)
        def mockPath = Mock(jakarta.validation.Path)
        mockPath.toString() >> "amount"
        violation.propertyPath >> mockPath
        violation.message >> "must be greater than or equal to 0"
        Set<jakarta.validation.ConstraintViolation<MedicalExpense>> violations = [violation] as Set
        validatorMock.validate(expense) >> violations
        def localService = new MedicalExpenseService(medicalExpenseRepositoryMock, meterService, validatorMock, ResilienceComponents.noOp())

        when:
        def result = localService.save(expense)

        then:
        result instanceof ServiceResult.ValidationError
        result.errors.containsKey("amount")
    }

    def "save should return ServiceResult.BusinessError when duplicate transaction ID"() {
        given:
        def expense = MedicalExpenseBuilder.builder()
                .withTransactionId(123L)
                .build()
        def existingExpense = MedicalExpenseBuilder.builder()
                .withMedicalExpenseId(2L)
                .withTransactionId(123L)
                .build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndTransactionId(TEST_OWNER, 123L) >> existingExpense
        medicalExpenseRepositoryMock.save(expense) >> { throw new DataIntegrityViolationException("Duplicate transaction ID") }
        def result = standardizedMedicalExpenseService.save(expense)

        then:
        result instanceof ServiceResult.BusinessError
        result.message.contains("Data integrity violation")
        result.errorCode == "DATA_INTEGRITY_VIOLATION"
    }

    def "save should return ServiceResult.SystemError on repository exception"() {
        given:
        def expense = MedicalExpenseBuilder.builder().build()
        medicalExpenseRepositoryMock.save(expense) >> { throw new RuntimeException("Save failed") }

        when:
        def result = standardizedMedicalExpenseService.save(expense)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Save failed")
    }

    // ===== update Tests =====

    def "update should return ServiceResult.Success when medical expense exists"() {
        given:
        def expense = MedicalExpenseBuilder.builder()
                .withMedicalExpenseId(1L)
                .withAmount(new BigDecimal("200.00"))
                .build()
        def existingExpense = MedicalExpenseBuilder.builder().withMedicalExpenseId(1L).build()
        def updatedExpense = MedicalExpenseBuilder.builder()
                .withMedicalExpenseId(1L)
                .withAmount(new BigDecimal("200.00"))
                .build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 1L) >> existingExpense
        medicalExpenseRepositoryMock.save(expense) >> updatedExpense
        def result = standardizedMedicalExpenseService.update(expense)

        then:
        result instanceof ServiceResult.Success
        result.data == updatedExpense
    }

    def "update should return ServiceResult.NotFound when medical expense does not exist"() {
        given:
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(999L).build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 999L) >> null
        def result = standardizedMedicalExpenseService.update(expense)

        then:
        result instanceof ServiceResult.NotFound
        result.message == "MedicalExpense not found: 999"
    }

    def "update should return ServiceResult.SystemError on repository exception"() {
        given:
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(1L).build()
        def existingExpense = MedicalExpenseBuilder.builder().withMedicalExpenseId(1L).build()
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 1L) >> existingExpense
        medicalExpenseRepositoryMock.save(expense) >> { throw new RuntimeException("Update failed") }

        when:
        def result = standardizedMedicalExpenseService.update(expense)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Update failed")
    }

    // ===== deleteById Tests =====

    def "deleteById should return ServiceResult.Success when medical expense exists"() {
        given:
        def expenseId = 1L
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(expenseId).build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, expenseId) >> expense
        medicalExpenseRepositoryMock.softDeleteByOwnerAndMedicalExpenseId(TEST_OWNER, expenseId) >> 1
        def result = standardizedMedicalExpenseService.deleteById(expenseId)

        then:
        result instanceof ServiceResult.Success
        result.data != null
    }

    def "deleteById should return ServiceResult.NotFound when medical expense does not exist"() {
        given:
        def expenseId = 999L

        when:
        medicalExpenseRepositoryMock.softDeleteByOwnerAndMedicalExpenseId(TEST_OWNER, expenseId) >> 0
        def result = standardizedMedicalExpenseService.deleteById(expenseId)

        then:
        result instanceof ServiceResult.NotFound
        result.message == "MedicalExpense not found: 999"
    }

    def "deleteById should return ServiceResult.SystemError on repository exception"() {
        given:
        def expenseId = 1L
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(expenseId).build()
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, expenseId) >> expense
        medicalExpenseRepositoryMock.softDeleteByOwnerAndMedicalExpenseId(TEST_OWNER, expenseId) >> { throw new RuntimeException("Delete failed") }

        when:
        def result = standardizedMedicalExpenseService.deleteById(expenseId)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Delete failed")
    }

    // ===== Legacy Method Compatibility Tests =====

    def "findAllMedicalExpenses should return list from findAllActive ServiceResult"() {
        given:
        def expense1 = MedicalExpenseBuilder.builder().withMedicalExpenseId(1L).build()
        def expense2 = MedicalExpenseBuilder.builder().withMedicalExpenseId(2L).build()
        def expenses = [expense1, expense2]

        when:
        medicalExpenseRepositoryMock.findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(TEST_OWNER) >> expenses
        def result = standardizedMedicalExpenseService.findAllMedicalExpenses()

        then:
        result.size() == 2
        result == expenses
    }

    def "findAllMedicalExpenses should return empty list on ServiceResult failure"() {
        given:
        medicalExpenseRepositoryMock.findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(TEST_OWNER) >> { throw new RuntimeException("Database error") }

        when:
        def result = standardizedMedicalExpenseService.findAllMedicalExpenses()

        then:
        result.isEmpty()
    }

    def "insertMedicalExpense should return medical expense on ServiceResult.Success"() {
        given:
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(0L).build()
        def savedExpense = MedicalExpenseBuilder.builder().withMedicalExpenseId(1L).build()

        when:
        medicalExpenseRepositoryMock.save(expense) >> savedExpense
        def result = standardizedMedicalExpenseService.insertMedicalExpense(expense)

        then:
        result == savedExpense
    }

    def "insertMedicalExpense should throw ValidationException on ServiceResult.ValidationError"() {
        given:
        def expense = MedicalExpenseBuilder.builder()
                .withAmount(new BigDecimal("-100.00"))
                .build()

        // Mock validation to return constraint violations
        def violation = Mock(jakarta.validation.ConstraintViolation)
        def mockPath = Mock(jakarta.validation.Path)
        mockPath.toString() >> "amount"
        violation.propertyPath >> mockPath
        violation.message >> "must be greater than or equal to 0"
        Set<jakarta.validation.ConstraintViolation<MedicalExpense>> violations = [violation] as Set
        validatorMock.validate(expense) >> violations
        def localService = new MedicalExpenseService(medicalExpenseRepositoryMock, meterService, validatorMock, ResilienceComponents.noOp())

        when:
        localService.insertMedicalExpense(expense)

        then:
        thrown(jakarta.validation.ValidationException)
    }

    def "insertMedicalExpense should throw DuplicateMedicalExpenseException on duplicate transaction ID"() {
        given:
        def expense = MedicalExpenseBuilder.builder()
                .withTransactionId(123L)
                .build()
        def existingExpense = MedicalExpenseBuilder.builder()
                .withMedicalExpenseId(2L)
                .withTransactionId(123L)
                .build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndTransactionId(TEST_OWNER, 123L) >> existingExpense
        standardizedMedicalExpenseService.insertMedicalExpense(expense)

        then:
        thrown(DuplicateMedicalExpenseException)
    }

    def "updateMedicalExpense should return medical expense on ServiceResult.Success"() {
        given:
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(1L).build()
        def existingExpense = MedicalExpenseBuilder.builder().withMedicalExpenseId(1L).build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 1L) >> existingExpense
        medicalExpenseRepositoryMock.save(expense) >> expense
        def result = standardizedMedicalExpenseService.updateMedicalExpense(expense)

        then:
        result == expense
    }

    def "updateMedicalExpense should throw IllegalArgumentException on ServiceResult.NotFound"() {
        given:
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(999L).build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 999L) >> null
        standardizedMedicalExpenseService.updateMedicalExpense(expense)

        then:
        thrown(IllegalArgumentException)
    }

    def "findMedicalExpenseById should return medical expense when found"() {
        given:
        def expenseId = 1L
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(expenseId).build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, expenseId) >> expense
        def result = standardizedMedicalExpenseService.findMedicalExpenseById(expenseId)

        then:
        result == expense
    }

    def "findMedicalExpenseById should return null when not found"() {
        given:
        def expenseId = 999L

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, expenseId) >> null
        def result = standardizedMedicalExpenseService.findMedicalExpenseById(expenseId)

        then:
        result == null
    }

    def "findMedicalExpenseByTransactionId should return medical expense when found"() {
        given:
        def transactionId = 123L
        def expense = MedicalExpenseBuilder.builder().withTransactionId(transactionId).build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndTransactionId(TEST_OWNER, transactionId) >> expense
        def result = standardizedMedicalExpenseService.findMedicalExpenseByTransactionId(transactionId)

        then:
        result == expense
    }

    def "updateClaimStatus should return true when update succeeds"() {
        given:
        def expenseId = 1L
        def claimStatus = ClaimStatus.Processing

        when:
        medicalExpenseRepositoryMock.updateClaimStatusByOwner(TEST_OWNER, expenseId, claimStatus) >> 1
        def result = standardizedMedicalExpenseService.updateClaimStatus(expenseId, claimStatus)

        then:
        result == true
    }

    def "updateClaimStatus should return false when no rows updated"() {
        given:
        def expenseId = 999L
        def claimStatus = ClaimStatus.Processing

        when:
        medicalExpenseRepositoryMock.updateClaimStatusByOwner(TEST_OWNER, expenseId, claimStatus) >> 0
        def result = standardizedMedicalExpenseService.updateClaimStatus(expenseId, claimStatus)

        then:
        result == false
    }

    def "softDeleteMedicalExpense should return true when delete succeeds"() {
        given:
        def expenseId = 1L

        when:
        medicalExpenseRepositoryMock.softDeleteByOwnerAndMedicalExpenseId(TEST_OWNER, expenseId) >> 1
        def result = standardizedMedicalExpenseService.softDeleteMedicalExpense(expenseId)

        then:
        result == true
    }

    def "softDeleteMedicalExpense should return false when no rows updated"() {
        given:
        def expenseId = 999L

        when:
        medicalExpenseRepositoryMock.softDeleteByOwnerAndMedicalExpenseId(TEST_OWNER, expenseId) >> 0
        def result = standardizedMedicalExpenseService.softDeleteMedicalExpense(expenseId)

        then:
        result == false
    }

    def "getTotalBilledAmountByYear should return sum or zero"() {
        given:
        def year = 2024

        when:
        medicalExpenseRepositoryMock.getTotalBilledAmountByOwnerAndYear(TEST_OWNER, year) >> new BigDecimal("1500.00")
        def result = standardizedMedicalExpenseService.getTotalBilledAmountByYear(year)

        then:
        result == new BigDecimal("1500.00")
    }

    def "getTotalBilledAmountByYear should return zero when null"() {
        given:
        def year = 2024

        when:
        medicalExpenseRepositoryMock.getTotalBilledAmountByOwnerAndYear(TEST_OWNER, year) >> null
        def result = standardizedMedicalExpenseService.getTotalBilledAmountByYear(year)

        then:
        result == BigDecimal.ZERO
    }

    def "getClaimStatusCounts should return map of counts"() {
        given:
        def processingCount = 5L
        def approvedCount = 10L

        when:
        medicalExpenseRepositoryMock.countByOwnerAndClaimStatusAndActiveStatusTrue(TEST_OWNER, ClaimStatus.Submitted) >> 0L
        medicalExpenseRepositoryMock.countByOwnerAndClaimStatusAndActiveStatusTrue(TEST_OWNER, ClaimStatus.Processing) >> processingCount
        medicalExpenseRepositoryMock.countByOwnerAndClaimStatusAndActiveStatusTrue(TEST_OWNER, ClaimStatus.Approved) >> approvedCount
        medicalExpenseRepositoryMock.countByOwnerAndClaimStatusAndActiveStatusTrue(TEST_OWNER, ClaimStatus.Denied) >> 0L
        medicalExpenseRepositoryMock.countByOwnerAndClaimStatusAndActiveStatusTrue(TEST_OWNER, ClaimStatus.Paid) >> 0L
        medicalExpenseRepositoryMock.countByOwnerAndClaimStatusAndActiveStatusTrue(TEST_OWNER, ClaimStatus.Closed) >> 0L
        def result = standardizedMedicalExpenseService.getClaimStatusCounts()

        then:
        result[ClaimStatus.Submitted] == 0L
        result[ClaimStatus.Processing] == processingCount
        result[ClaimStatus.Approved] == approvedCount
        result[ClaimStatus.Denied] == 0L
        result[ClaimStatus.Paid] == 0L
        result[ClaimStatus.Closed] == 0L
    }

    // ===== Payment-related Tests for Phase 2.5 =====

    def "linkPaymentTransaction should link transaction successfully"() {
        given:
        def expenseId = 1L
        def transactionId = 123L
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(expenseId).build()
        def savedExpense = MedicalExpenseBuilder.builder()
                .withMedicalExpenseId(expenseId)
                .withTransactionId(transactionId)
                .build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, expenseId) >> expense
        medicalExpenseRepositoryMock.findByOwnerAndTransactionId(TEST_OWNER, transactionId) >> null
        medicalExpenseRepositoryMock.save(_ as MedicalExpense) >> savedExpense
        def result = standardizedMedicalExpenseService.linkPaymentTransaction(expenseId, transactionId)

        then:
        result.transactionId == transactionId
    }

    def "linkPaymentTransaction should throw exception when transaction already linked"() {
        given:
        def expenseId = 1L
        def transactionId = 123L
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(expenseId).build()
        def existingExpense = MedicalExpenseBuilder.builder()
                .withMedicalExpenseId(2L)
                .withTransactionId(transactionId)
                .build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, expenseId) >> expense
        medicalExpenseRepositoryMock.findByOwnerAndTransactionId(TEST_OWNER, transactionId) >> existingExpense
        standardizedMedicalExpenseService.linkPaymentTransaction(expenseId, transactionId)

        then:
        thrown(DuplicateMedicalExpenseException)
    }

    def "unlinkPaymentTransaction should unlink transaction successfully"() {
        given:
        def expenseId = 1L
        def expense = MedicalExpenseBuilder.builder()
                .withMedicalExpenseId(expenseId)
                .withTransactionId(123L)
                .build()
        def savedExpense = MedicalExpenseBuilder.builder()
                .withMedicalExpenseId(expenseId)
                .withTransactionId(null)
                .withPaidAmount(BigDecimal.ZERO)
                .build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, expenseId) >> expense
        medicalExpenseRepositoryMock.save(_ as MedicalExpense) >> savedExpense
        def result = standardizedMedicalExpenseService.unlinkPaymentTransaction(expenseId)

        then:
        result.transactionId == null
        result.paidAmount == BigDecimal.ZERO
    }

    def "updatePaidAmount should update successfully"() {
        given:
        def expenseId = 1L
        def expense = MedicalExpenseBuilder.builder()
                .withMedicalExpenseId(expenseId)
                .withTransactionId(123L)
                .build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, expenseId) >> expense
        medicalExpenseRepositoryMock.save(expense) >> expense
        def result = standardizedMedicalExpenseService.updatePaidAmount(expenseId)

        then:
        result == expense
    }

    def "updatePaidAmount should set paidAmount to zero if no transaction linked"() {
        given:
        def medicalExpenseId = 1L
        def medicalExpense = MedicalExpenseBuilder.builder()
                .withMedicalExpenseId(medicalExpenseId)
                .withTransactionId(null)
                .withPaidAmount(new BigDecimal("50.00"))
                .build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, medicalExpenseId) >> medicalExpense
        medicalExpenseRepositoryMock.save(medicalExpense) >> medicalExpense
        def result = standardizedMedicalExpenseService.updatePaidAmount(medicalExpenseId)

        then:
        result.paidAmount == BigDecimal.ZERO
    }

    def "getClaimStatusCounts should return counts for all statuses"() {
        when:
        def result = standardizedMedicalExpenseService.getClaimStatusCounts()

        then:
        ClaimStatus.values().each { status ->
            1 * medicalExpenseRepositoryMock.countByOwnerAndClaimStatusAndActiveStatusTrue(TEST_OWNER, status) >> 1L
        }
        result.size() == ClaimStatus.values().size()
        result.values().every { it == 1L }
    }

    def "findAllMedicalExpenses should return empty list on error"() {
        when:
        def result = standardizedMedicalExpenseService.findAllMedicalExpenses()

        then:
        1 * medicalExpenseRepositoryMock.findByOwnerAndActiveStatusTrueOrderByServiceDateDesc(TEST_OWNER) >> { throw new RuntimeException("error") }
        result == []
    }

    def "insertMedicalExpense should throw BusinessError if duplicate transactionId"() {
        given:
        def expense = MedicalExpenseBuilder.builder().withTransactionId(100L).build()

        when:
        standardizedMedicalExpenseService.insertMedicalExpense(expense)

        then:
        1 * medicalExpenseRepositoryMock.findByOwnerAndTransactionId(TEST_OWNER, 100L) >> new MedicalExpense()
        thrown(finance.exceptions.DuplicateMedicalExpenseException)
    }

    def "insertMedicalExpense should succeed when transactionId is null (no dup check)"() {
        given:
        def expense = MedicalExpenseBuilder.builder().withTransactionId(null).build()
        def saved = MedicalExpenseBuilder.builder().withMedicalExpenseId(5L).withTransactionId(null).build()

        when:
        medicalExpenseRepositoryMock.save(expense) >> saved
        def result = standardizedMedicalExpenseService.insertMedicalExpense(expense)

        then:
        result.medicalExpenseId == 5L
        0 * medicalExpenseRepositoryMock.findByOwnerAndTransactionId(_, _)
    }

    def "insertMedicalExpense should succeed when transactionId is zero (no dup check)"() {
        given:
        def expense = MedicalExpenseBuilder.builder().withTransactionId(null).build()
        def saved = MedicalExpenseBuilder.builder().withMedicalExpenseId(6L).build()

        when:
        medicalExpenseRepositoryMock.save(expense) >> saved
        def result = standardizedMedicalExpenseService.insertMedicalExpense(expense)

        then:
        result.medicalExpenseId == 6L
        0 * medicalExpenseRepositoryMock.findByOwnerAndTransactionId(_, _)
    }

    def "updateMedicalExpense should throw DataIntegrityViolationException on BusinessError"() {
        given:
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(1L).build()
        def existingExpense = MedicalExpenseBuilder.builder().withMedicalExpenseId(1L).build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 1L) >> existingExpense
        medicalExpenseRepositoryMock.save(expense) >> { throw new org.springframework.dao.DataIntegrityViolationException("constraint violation") }
        standardizedMedicalExpenseService.updateMedicalExpense(expense)

        then:
        thrown(RuntimeException)
    }

    def "findMedicalExpensesByAccountId should delegate to repository"() {
        given:
        def accountId = 42L
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findByOwnerAndAccountId(TEST_OWNER, accountId) >> expenses
        def result = standardizedMedicalExpenseService.findMedicalExpensesByAccountId(accountId)

        then:
        result == expenses
    }

    def "findMedicalExpensesByServiceDateRange should delegate to repository"() {
        given:
        def start = java.time.LocalDate.of(2024, 1, 1)
        def end = java.time.LocalDate.of(2024, 12, 31)
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findByOwnerAndServiceDateBetweenAndActiveStatusTrue(TEST_OWNER, start, end) >> expenses
        def result = standardizedMedicalExpenseService.findMedicalExpensesByServiceDateRange(start, end)

        then:
        result == expenses
    }

    def "findMedicalExpensesByAccountIdAndDateRange should delegate to repository"() {
        given:
        def accountId = 10L
        def start = java.time.LocalDate.of(2024, 1, 1)
        def end = java.time.LocalDate.of(2024, 6, 30)
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findByOwnerAndAccountIdAndServiceDateBetween(TEST_OWNER, accountId, start, end) >> expenses
        def result = standardizedMedicalExpenseService.findMedicalExpensesByAccountIdAndDateRange(accountId, start, end)

        then:
        result == expenses
    }

    def "findMedicalExpensesByProviderId should delegate to repository"() {
        given:
        def providerId = 7L
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findByOwnerAndProviderIdAndActiveStatusTrue(TEST_OWNER, providerId) >> expenses
        def result = standardizedMedicalExpenseService.findMedicalExpensesByProviderId(providerId)

        then:
        result == expenses
    }

    def "findMedicalExpensesByFamilyMemberId should delegate to repository"() {
        given:
        def familyMemberId = 3L
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, familyMemberId) >> expenses
        def result = standardizedMedicalExpenseService.findMedicalExpensesByFamilyMemberId(familyMemberId)

        then:
        result == expenses
    }

    def "findMedicalExpensesByFamilyMemberAndDateRange should delegate to repository"() {
        given:
        def familyMemberId = 2L
        def start = java.time.LocalDate.of(2024, 1, 1)
        def end = java.time.LocalDate.of(2024, 12, 31)
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findByOwnerAndFamilyMemberIdAndServiceDateBetween(TEST_OWNER, familyMemberId, start, end) >> expenses
        def result = standardizedMedicalExpenseService.findMedicalExpensesByFamilyMemberAndDateRange(familyMemberId, start, end)

        then:
        result == expenses
    }

    def "findMedicalExpensesByClaimStatus should delegate to repository"() {
        given:
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findByOwnerAndClaimStatusAndActiveStatusTrue(TEST_OWNER, ClaimStatus.Approved) >> expenses
        def result = standardizedMedicalExpenseService.findMedicalExpensesByClaimStatus(ClaimStatus.Approved)

        then:
        result == expenses
    }

    def "findOutOfNetworkExpenses should delegate to repository"() {
        given:
        def expenses = [MedicalExpenseBuilder.builder().withIsOutOfNetwork(true).build()]

        when:
        medicalExpenseRepositoryMock.findByOwnerAndIsOutOfNetworkAndActiveStatusTrue(TEST_OWNER, true) >> expenses
        def result = standardizedMedicalExpenseService.findOutOfNetworkExpenses()

        then:
        result == expenses
    }

    def "findOutstandingPatientBalances should delegate to repository"() {
        given:
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findOutstandingPatientBalancesByOwner(TEST_OWNER) >> expenses
        def result = standardizedMedicalExpenseService.findOutstandingPatientBalances()

        then:
        result == expenses
    }

    def "findActiveOpenClaims should delegate to repository"() {
        given:
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findActiveOpenClaimsByOwner(TEST_OWNER) >> expenses
        def result = standardizedMedicalExpenseService.findActiveOpenClaims()

        then:
        result == expenses
    }

    def "getTotalPatientResponsibilityByYear should return sum or zero"() {
        when:
        medicalExpenseRepositoryMock.getTotalPatientResponsibilityByOwnerAndYear(TEST_OWNER, 2024) >> new BigDecimal("500.00")
        def result = standardizedMedicalExpenseService.getTotalPatientResponsibilityByYear(2024)

        then:
        result == new BigDecimal("500.00")
    }

    def "getTotalPatientResponsibilityByYear should return zero when null"() {
        when:
        medicalExpenseRepositoryMock.getTotalPatientResponsibilityByOwnerAndYear(TEST_OWNER, 2024) >> null
        def result = standardizedMedicalExpenseService.getTotalPatientResponsibilityByYear(2024)

        then:
        result == BigDecimal.ZERO
    }

    def "getTotalInsurancePaidByYear should return sum or zero"() {
        when:
        medicalExpenseRepositoryMock.getTotalInsurancePaidByOwnerAndYear(TEST_OWNER, 2024) >> new BigDecimal("800.00")
        def result = standardizedMedicalExpenseService.getTotalInsurancePaidByYear(2024)

        then:
        result == new BigDecimal("800.00")
    }

    def "getTotalInsurancePaidByYear should return zero when null"() {
        when:
        medicalExpenseRepositoryMock.getTotalInsurancePaidByOwnerAndYear(TEST_OWNER, 2024) >> null
        def result = standardizedMedicalExpenseService.getTotalInsurancePaidByYear(2024)

        then:
        result == BigDecimal.ZERO
    }

    def "getTotalPaidAmountByYear should return sum or zero"() {
        when:
        medicalExpenseRepositoryMock.getTotalPaidAmountByOwnerAndYear(TEST_OWNER, 2024) >> new BigDecimal("200.00")
        def result = standardizedMedicalExpenseService.getTotalPaidAmountByYear(2024)

        then:
        result == new BigDecimal("200.00")
    }

    def "getTotalPaidAmountByYear should return zero when null"() {
        when:
        medicalExpenseRepositoryMock.getTotalPaidAmountByOwnerAndYear(TEST_OWNER, 2024) >> null
        def result = standardizedMedicalExpenseService.getTotalPaidAmountByYear(2024)

        then:
        result == BigDecimal.ZERO
    }

    def "getTotalUnpaidBalance should return balance or zero"() {
        when:
        medicalExpenseRepositoryMock.getTotalUnpaidBalanceByOwner(TEST_OWNER) >> new BigDecimal("350.00")
        def result = standardizedMedicalExpenseService.getTotalUnpaidBalance()

        then:
        result == new BigDecimal("350.00")
    }

    def "getTotalUnpaidBalance should return zero when null"() {
        when:
        medicalExpenseRepositoryMock.getTotalUnpaidBalanceByOwner(TEST_OWNER) >> null
        def result = standardizedMedicalExpenseService.getTotalUnpaidBalance()

        then:
        result == BigDecimal.ZERO
    }

    def "findMedicalExpensesByProcedureCode should delegate to repository"() {
        given:
        def expenses = [MedicalExpenseBuilder.builder().withProcedureCode("CPT-99213").build()]

        when:
        medicalExpenseRepositoryMock.findByOwnerAndProcedureCodeAndActiveStatusTrue(TEST_OWNER, "CPT-99213") >> expenses
        def result = standardizedMedicalExpenseService.findMedicalExpensesByProcedureCode("CPT-99213")

        then:
        result == expenses
    }

    def "findMedicalExpensesByDiagnosisCode should delegate to repository"() {
        given:
        def expenses = [MedicalExpenseBuilder.builder().withDiagnosisCode("ICD-Z00.00").build()]

        when:
        medicalExpenseRepositoryMock.findByOwnerAndDiagnosisCodeAndActiveStatusTrue(TEST_OWNER, "ICD-Z00.00") >> expenses
        def result = standardizedMedicalExpenseService.findMedicalExpensesByDiagnosisCode("ICD-Z00.00")

        then:
        result == expenses
    }

    def "findUnpaidMedicalExpenses should delegate to repository"() {
        given:
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findUnpaidMedicalExpensesByOwner(TEST_OWNER) >> expenses
        def result = standardizedMedicalExpenseService.findUnpaidMedicalExpenses()

        then:
        result == expenses
    }

    def "findPartiallyPaidMedicalExpenses should delegate to repository"() {
        given:
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findPartiallyPaidMedicalExpensesByOwner(TEST_OWNER) >> expenses
        def result = standardizedMedicalExpenseService.findPartiallyPaidMedicalExpenses()

        then:
        result == expenses
    }

    def "findFullyPaidMedicalExpenses should delegate to repository"() {
        given:
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findFullyPaidMedicalExpensesByOwner(TEST_OWNER) >> expenses
        def result = standardizedMedicalExpenseService.findFullyPaidMedicalExpenses()

        then:
        result == expenses
    }

    def "findMedicalExpensesWithoutTransaction should delegate to repository"() {
        given:
        def expenses = [MedicalExpenseBuilder.builder().withTransactionId(null).build()]

        when:
        medicalExpenseRepositoryMock.findMedicalExpensesWithoutTransactionByOwner(TEST_OWNER) >> expenses
        def result = standardizedMedicalExpenseService.findMedicalExpensesWithoutTransaction()

        then:
        result == expenses
    }

    def "findOverpaidMedicalExpenses should delegate to repository"() {
        given:
        def expenses = [MedicalExpenseBuilder.builder().build()]

        when:
        medicalExpenseRepositoryMock.findOverpaidMedicalExpensesByOwner(TEST_OWNER) >> expenses
        def result = standardizedMedicalExpenseService.findOverpaidMedicalExpenses()

        then:
        result == expenses
    }

    def "linkPaymentTransaction should throw IllegalArgumentException when expense not found"() {
        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 999L) >> null
        standardizedMedicalExpenseService.linkPaymentTransaction(999L, 123L)

        then:
        thrown(IllegalArgumentException)
    }

    def "linkPaymentTransaction should allow linking when transaction is already linked to same expense"() {
        given:
        def expenseId = 1L
        def transactionId = 123L
        def expense = MedicalExpenseBuilder.builder().withMedicalExpenseId(expenseId).build()
        def sameExpenseLinked = MedicalExpenseBuilder.builder()
            .withMedicalExpenseId(expenseId)
            .withTransactionId(transactionId)
            .build()

        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, expenseId) >> expense
        medicalExpenseRepositoryMock.findByOwnerAndTransactionId(TEST_OWNER, transactionId) >> sameExpenseLinked
        medicalExpenseRepositoryMock.save(_ as MedicalExpense) >> sameExpenseLinked
        def result = standardizedMedicalExpenseService.linkPaymentTransaction(expenseId, transactionId)

        then:
        result.transactionId == transactionId
    }

    def "unlinkPaymentTransaction should throw IllegalArgumentException when expense not found"() {
        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 999L) >> null
        standardizedMedicalExpenseService.unlinkPaymentTransaction(999L)

        then:
        thrown(IllegalArgumentException)
    }

    def "updatePaidAmount should throw IllegalArgumentException when expense not found"() {
        when:
        medicalExpenseRepositoryMock.findByOwnerAndMedicalExpenseIdAndActiveStatusTrue(TEST_OWNER, 999L) >> null
        standardizedMedicalExpenseService.updatePaidAmount(999L)

        then:
        thrown(IllegalArgumentException)
    }

    def "softDeleteMedicalExpense should propagate exception from repository"() {
        when:
        medicalExpenseRepositoryMock.softDeleteByOwnerAndMedicalExpenseId(TEST_OWNER, 1L) >> { throw new RuntimeException("db failure") }
        standardizedMedicalExpenseService.softDeleteMedicalExpense(1L)

        then:
        thrown(RuntimeException)
    }

    def "updateClaimStatus should propagate exception from repository"() {
        when:
        medicalExpenseRepositoryMock.updateClaimStatusByOwner(TEST_OWNER, 1L, ClaimStatus.Denied) >> { throw new RuntimeException("lock timeout") }
        standardizedMedicalExpenseService.updateClaimStatus(1L, ClaimStatus.Denied)

        then:
        thrown(RuntimeException)
    }
}
