package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.dto.MedicalExpenseInputDto
import finance.controllers.graphql.GraphQLMutationController
import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.services.MedicalExpenseService
import org.springframework.beans.factory.annotation.Autowired
import jakarta.validation.ConstraintViolationException
import java.math.BigDecimal
import java.sql.Date

class MedicalExpenseMutationIntSpec extends BaseIntegrationSpec {

    @Autowired
    GraphQLMutationController mutationController

    @Autowired
    MedicalExpenseService medicalExpenseService

    def "createMedicalExpense mutation succeeds with valid input"() {
        given:
        withUserRole()
        def expenseInput = new MedicalExpenseInputDto(
                null,                               // medicalExpenseId
                null,                               // transactionId
                null,                               // providerId
                null,                               // familyMemberId
                Date.valueOf("2024-01-15"),        // serviceDate
                "Office visit",                     // serviceDescription
                "99213",                           // procedureCode
                "A00-A09",                         // diagnosisCode
                new BigDecimal("200.00"),          // billedAmount
                new BigDecimal("50.00"),           // insuranceDiscount
                new BigDecimal("100.00"),          // insurancePaid
                new BigDecimal("50.00"),           // patientResponsibility
                null,                               // paidDate
                false,                              // isOutOfNetwork
                "CLAIM12345",                      // claimNumber
                ClaimStatus.Submitted,              // claimStatus
                true,                               // activeStatus
                BigDecimal.ZERO                     // paidAmount
        )

        when:
        def result = mutationController.createMedicalExpense(expenseInput)

        then:
        result != null
        result.medicalExpenseId > 0
        result.procedureCode == "99213"
        result.billedAmount == new BigDecimal("200.00")
        result.claimStatus == ClaimStatus.Submitted
        result.activeStatus == true
    }

    def "createMedicalExpense mutation fails validation for null serviceDate"() {
        given:
        withUserRole()
        def expenseInput = new MedicalExpenseInputDto(
                null,                               // medicalExpenseId
                null,                               // transactionId
                null,                               // providerId
                null,                               // familyMemberId
                null,                               // serviceDate - invalid: null
                "Office visit",                     // serviceDescription
                "99213",                           // procedureCode
                "A00-A09",                         // diagnosisCode
                new BigDecimal("200.00"),          // billedAmount
                new BigDecimal("50.00"),           // insuranceDiscount
                new BigDecimal("100.00"),          // insurancePaid
                new BigDecimal("50.00"),           // patientResponsibility
                null,                               // paidDate
                false,                              // isOutOfNetwork
                "CLAIM12345",                      // claimNumber
                ClaimStatus.Submitted,              // claimStatus
                true,                               // activeStatus
                BigDecimal.ZERO                     // paidAmount
        )

        when:
        mutationController.createMedicalExpense(expenseInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createMedicalExpense mutation fails validation for negative billedAmount"() {
        given:
        withUserRole()
        def expenseInput = new MedicalExpenseInputDto(
                null,
                null,
                null,
                null,
                Date.valueOf("2024-01-15"),
                "Office visit",
                "99213",
                "A00-A09",
                new BigDecimal("-200.00"),         // invalid: negative
                new BigDecimal("50.00"),
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                null,
                false,
                "CLAIM12345",
                ClaimStatus.Submitted,
                true,
                BigDecimal.ZERO
        )

        when:
        mutationController.createMedicalExpense(expenseInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createMedicalExpense mutation fails validation for invalid procedureCode pattern"() {
        given:
        withUserRole()
        def expenseInput = new MedicalExpenseInputDto(
                null,
                null,
                null,
                null,
                Date.valueOf("2024-01-15"),
                "Office visit",
                "invalid_code",                    // invalid: contains lowercase
                "A00-A09",
                new BigDecimal("200.00"),
                new BigDecimal("50.00"),
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                null,
                false,
                "CLAIM12345",
                ClaimStatus.Submitted,
                true,
                BigDecimal.ZERO
        )

        when:
        mutationController.createMedicalExpense(expenseInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "updateMedicalExpense mutation succeeds with valid input"() {
        given:
        withUserRole()
        def created = createTestMedicalExpense("PROC123", "2024-02-10")
        def expenseInput = new MedicalExpenseInputDto(
                created.medicalExpenseId,
                null,
                null,
                null,
                Date.valueOf("2024-02-10"),
                "Updated service description",
                "PROC123",
                "A00-A09",
                new BigDecimal("300.00"),          // updated amount
                new BigDecimal("75.00"),
                new BigDecimal("150.00"),
                new BigDecimal("75.00"),
                null,
                false,
                created.claimNumber,
                ClaimStatus.Approved,               // updated status
                true,
                BigDecimal.ZERO
        )

        when:
        def result = mutationController.updateMedicalExpense(expenseInput)

        then:
        result != null
        result.medicalExpenseId == created.medicalExpenseId
        result.billedAmount == new BigDecimal("300.00")
        result.claimStatus == ClaimStatus.Approved
    }

    def "updateMedicalExpense mutation fails for non-existent medicalExpense"() {
        given:
        withUserRole()
        def expenseInput = new MedicalExpenseInputDto(
                999999L,                            // non-existent ID
                null,
                null,
                null,
                Date.valueOf("2024-01-15"),
                "Office visit",
                "99213",
                "A00-A09",
                new BigDecimal("200.00"),
                new BigDecimal("50.00"),
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                null,
                false,
                "CLAIM12345",
                ClaimStatus.Submitted,
                true,
                BigDecimal.ZERO
        )

        when:
        mutationController.updateMedicalExpense(expenseInput)

        then:
        thrown(RuntimeException)
    }

    def "deleteMedicalExpense mutation returns true for existing expense"() {
        given:
        withUserRole()
        def created = createTestMedicalExpense("PROC999", "2024-05-01")

        when:
        def deleted = mutationController.deleteMedicalExpense(created.medicalExpenseId)

        then:
        deleted == true
    }

    def "deleteMedicalExpense mutation returns false for missing expense"() {
        given:
        withUserRole()

        expect:
        mutationController.deleteMedicalExpense(999999L) == false
    }

    private MedicalExpense createTestMedicalExpense(String procedureCode, String serviceDateStr) {
        MedicalExpense expense = new MedicalExpense()
        expense.serviceDate = Date.valueOf(serviceDateStr)
        expense.serviceDescription = "Test medical service"
        expense.procedureCode = procedureCode
        expense.diagnosisCode = "A00-A09"
        expense.billedAmount = new BigDecimal("500.00")
        expense.insuranceDiscount = new BigDecimal("100.00")
        expense.insurancePaid = new BigDecimal("300.00")
        expense.patientResponsibility = new BigDecimal("100.00")
        expense.isOutOfNetwork = false
        expense.claimNumber = "CLAIM-${procedureCode}"
        expense.claimStatus = ClaimStatus.Submitted
        expense.activeStatus = true
        expense.paidAmount = BigDecimal.ZERO

        def result = medicalExpenseService.save(expense)
        return result.data
    }
}
