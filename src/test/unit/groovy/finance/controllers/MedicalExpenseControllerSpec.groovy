package finance.controllers

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.services.MedicalExpenseService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

class MedicalExpenseControllerSpec extends Specification {

    MedicalExpenseService medicalExpenseService = Mock()
    MedicalExpenseController controller = new MedicalExpenseController(medicalExpenseService)

    def "should insert medical expense successfully via controller"() {
        given: "a valid medical expense"
        MedicalExpense medicalExpense = new MedicalExpense(
                transactionId: 123L,
                serviceDate: Date.valueOf("2024-01-15"),
                billedAmount: new BigDecimal("100.00"),
                insuranceDiscount: new BigDecimal("10.00"),
                insurancePaid: new BigDecimal("70.00"),
                patientResponsibility: new BigDecimal("20.00"),
                claimStatus: ClaimStatus.Submitted,
                activeStatus: true,
                isOutOfNetwork: false,
                dateAdded: new Timestamp(System.currentTimeMillis()),
                dateUpdated: new Timestamp(System.currentTimeMillis())
        )

        and: "service returns successfully"
        medicalExpenseService.insertMedicalExpense(medicalExpense) >> { args ->
            MedicalExpense saved = args[0] as MedicalExpense
            saved.medicalExpenseId = 456L
            return saved
        }

        when: "calling controller insert method"
        ResponseEntity<MedicalExpense> response = controller.insertMedicalExpense(medicalExpense)

        then: "should return CREATED status"
        response.statusCode == HttpStatus.CREATED
        response.body != null
        response.body.medicalExpenseId == 456L
        response.body.transactionId == 123L
        response.body.claimStatus == ClaimStatus.Submitted
    }

    def "should handle ClaimStatus enum properly"() {
        given: "a medical expense with Submitted claim status"
        MedicalExpense medicalExpense = new MedicalExpense(
                transactionId: 123L,
                serviceDate: Date.valueOf("2024-01-15"),
                billedAmount: new BigDecimal("100.00"),
                claimStatus: ClaimStatus.Submitted,
                activeStatus: true,
                isOutOfNetwork: false
        )

        when: "accessing the claim status"
        ClaimStatus status = medicalExpense.claimStatus

        then: "should be properly set"
        status == ClaimStatus.Submitted
        status.label == "submitted"
        status.toValue() == "submitted"
    }
}