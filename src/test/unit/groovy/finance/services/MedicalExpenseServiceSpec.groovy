package finance.services

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.exceptions.DuplicateMedicalExpenseException
import finance.repositories.MedicalExpenseRepository
import spock.lang.Specification
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

class MedicalExpenseServiceSpec extends Specification {

    MedicalExpenseRepository medicalExpenseRepository = Mock()
    MedicalExpenseService medicalExpenseService = new MedicalExpenseService(medicalExpenseRepository)

    def "should insert medical expense successfully"() {
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

        and: "no existing medical expense for the transaction"
        medicalExpenseRepository.findByTransactionId(123L) >> null

        and: "repository save returns the saved expense"
        medicalExpenseRepository.save(medicalExpense) >> { args ->
            MedicalExpense saved = args[0] as MedicalExpense
            saved.medicalExpenseId = 456L
            return saved
        }

        when: "inserting the medical expense"
        MedicalExpense result = medicalExpenseService.insertMedicalExpense(medicalExpense)

        then: "the medical expense is saved successfully"
        result != null
        result.medicalExpenseId == 456L
        result.transactionId == 123L
        result.claimStatus == ClaimStatus.Submitted
    }

    def "should throw DuplicateMedicalExpenseException when medical expense already exists for transaction"() {
        given: "a medical expense"
        MedicalExpense medicalExpense = new MedicalExpense(
                transactionId: 123L,
                serviceDate: Date.valueOf("2024-01-15"),
                billedAmount: new BigDecimal("100.00"),
                claimStatus: ClaimStatus.Submitted
        )

        and: "an existing medical expense for the same transaction"
        MedicalExpense existingExpense = new MedicalExpense(medicalExpenseId: 789L, transactionId: 123L)
        medicalExpenseRepository.findByTransactionId(123L) >> existingExpense

        when: "attempting to insert the medical expense"
        medicalExpenseService.insertMedicalExpense(medicalExpense)

        then: "DuplicateMedicalExpenseException is thrown"
        thrown(DuplicateMedicalExpenseException)
    }
}
