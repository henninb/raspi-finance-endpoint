package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLQueryController
import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.services.MedicalExpenseService
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import java.math.BigDecimal
import java.time.LocalDate

class MedicalExpenseQueryIntSpec extends BaseIntegrationSpec {

    @Shared @Autowired
    MedicalExpenseService medicalExpenseService

    @Shared @Autowired
    GraphQLQueryController queryController

    def "fetch all medicalExpenses via query controller"() {
        given:
        createTestMedicalExpense("PROC001", "2024-01-15")
        createTestMedicalExpense("PROC002", "2024-02-20")

        when:
        def medicalExpenses = queryController.medicalExpenses()

        then:
        medicalExpenses != null
        medicalExpenses.size() >= 2
        medicalExpenses.any { it.procedureCode == "PROC001" }
        medicalExpenses.any { it.procedureCode == "PROC002" }
    }

    def "fetch medicalExpense by id via query controller"() {
        given:
        def savedExpense = createTestMedicalExpense("PROC003", "2024-03-10")

        when:
        def result = queryController.medicalExpense(savedExpense.medicalExpenseId)

        then:
        result != null
        result.medicalExpenseId == savedExpense.medicalExpenseId
        result.procedureCode == "PROC003"
        result.activeStatus == true
    }

    def "handle medicalExpense not found via query controller"() {
        expect:
        queryController.medicalExpense(999999L) == null
    }

    def "fetch medicalExpenses by claimStatus via query controller"() {
        given:
        createTestMedicalExpense("PROC004", "2024-04-01", ClaimStatus.Submitted)
        createTestMedicalExpense("PROC005", "2024-04-05", ClaimStatus.Approved)

        when:
        def submittedExpenses = queryController.medicalExpensesByClaimStatus(ClaimStatus.Submitted)

        then:
        submittedExpenses != null
        submittedExpenses.size() >= 1
        submittedExpenses.every { it.claimStatus == ClaimStatus.Submitted }
    }

    private MedicalExpense createTestMedicalExpense(String procedureCode, String serviceDateStr, ClaimStatus claimStatus = ClaimStatus.Submitted) {
        MedicalExpense expense = new MedicalExpense()
        expense.serviceDate = LocalDate.parse(serviceDateStr)
        expense.serviceDescription = "Test medical service"
        expense.procedureCode = procedureCode
        expense.diagnosisCode = "A00-A09"
        expense.billedAmount = new BigDecimal("500.00")
        expense.insuranceDiscount = new BigDecimal("100.00")
        expense.insurancePaid = new BigDecimal("300.00")
        expense.patientResponsibility = new BigDecimal("100.00")
        expense.isOutOfNetwork = false
        expense.claimNumber = "CLAIM-${procedureCode}"
        expense.claimStatus = claimStatus
        expense.activeStatus = true
        expense.paidAmount = BigDecimal.ZERO

        def result = medicalExpenseService.save(expense)
        return result.data
    }
}
