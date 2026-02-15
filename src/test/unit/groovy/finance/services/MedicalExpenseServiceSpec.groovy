package finance.services

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
    def standardizedMedicalExpenseService = new MedicalExpenseService(medicalExpenseRepositoryMock)

    void setup() {
        standardizedMedicalExpenseService.meterService = meterService
        standardizedMedicalExpenseService.validator = validator
    }

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

        when:
        standardizedMedicalExpenseService.validator = validatorMock
        validatorMock.validate(expense) >> violations
        def result = standardizedMedicalExpenseService.save(expense)

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

        when:
        medicalExpenseRepositoryMock.softDeleteByOwnerAndMedicalExpenseId(TEST_OWNER, expenseId) >> 1
        def result = standardizedMedicalExpenseService.deleteById(expenseId)

        then:
        result instanceof ServiceResult.Success
        result.data == true
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

        when:
        standardizedMedicalExpenseService.validator = validatorMock
        validatorMock.validate(expense) >> violations
        standardizedMedicalExpenseService.insertMedicalExpense(expense)

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
}
