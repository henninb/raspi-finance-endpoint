
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

    def "should return a list of medical expenses"() {
        given: "a medical expense"
        def medicalExpense = new MedicalExpense(medicalExpenseId: 1L, transactionId: 1L, serviceDate: Date.valueOf("2024-01-15"), billedAmount: BigDecimal.TEN, insurancePaid: BigDecimal.ONE, patientResponsibility: BigDecimal.ZERO, claimStatus: ClaimStatus.Submitted, activeStatus: true, isOutOfNetwork: false, dateAdded: new Timestamp(System.currentTimeMillis()), dateUpdated: new Timestamp(System.currentTimeMillis()))
        medicalExpenseRepository.findByActiveStatusTrueOrderByServiceDateDesc() >> [medicalExpense]

        when: "the find all medical expenses is called"
        def result = medicalExpenseService.findAllMedicalExpenses()

        then: "the medical expense is returned"
        result.size() == 1
        result[0].medicalExpenseId == 1L
    }

    def "should update a medical expense"() {
        given: "a medical expense"
        def medicalExpense = new MedicalExpense(medicalExpenseId: 1L, transactionId: 1L, serviceDate: Date.valueOf("2024-01-15"), billedAmount: BigDecimal.TEN, insurancePaid: BigDecimal.ONE, patientResponsibility: BigDecimal.ZERO, claimStatus: ClaimStatus.Submitted, activeStatus: true, isOutOfNetwork: false, dateAdded: new Timestamp(System.currentTimeMillis()), dateUpdated: new Timestamp(System.currentTimeMillis()))
        medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(1L) >> medicalExpense
        medicalExpenseRepository.save(_ as MedicalExpense) >> medicalExpense

        when: "the update medical expense is called"
        def result = medicalExpenseService.updateMedicalExpense(medicalExpense)

        then: "the medical expense is updated"
        result.medicalExpenseId == 1L
    }

    def "should throw exception when updating non-existent medical expense"() {
        given: "a medical expense"
        def medicalExpense = new MedicalExpense(medicalExpenseId: 1L)
        medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(1L) >> null

        when: "updating a non-existent medical expense"
        medicalExpenseService.updateMedicalExpense(medicalExpense)

        then: "IllegalArgumentException is thrown"
        thrown(IllegalArgumentException)
    }

    def "should find a medical expense by id"() {
        given: "a medical expense"
        def medicalExpense = new MedicalExpense(medicalExpenseId: 1L, transactionId: 1L, serviceDate: Date.valueOf("2024-01-15"), billedAmount: BigDecimal.TEN, insurancePaid: BigDecimal.ONE, patientResponsibility: BigDecimal.ZERO, claimStatus: ClaimStatus.Submitted, activeStatus: true, isOutOfNetwork: false, dateAdded: new Timestamp(System.currentTimeMillis()), dateUpdated: new Timestamp(System.currentTimeMillis()))
        medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(1L) >> medicalExpense

        when: "the find medical expense by id is called"
        def result = medicalExpenseService.findMedicalExpenseById(1L)

        then: "the medical expense is returned"
        result.medicalExpenseId == 1L
    }

    def "should find medical expense by transaction id"() {
        given: "a medical expense"
        def medicalExpense = new MedicalExpense(transactionId: 123L)
        medicalExpenseRepository.findByTransactionId(123L) >> medicalExpense

        when: "finding by transaction id"
        def result = medicalExpenseService.findMedicalExpenseByTransactionId(123L)

        then: "the correct expense is returned"
        result.transactionId == 123L
    }

    def "should soft delete a medical expense"() {
        when: "soft deleting a medical expense"
        def result = medicalExpenseService.softDeleteMedicalExpense(1L)

        then: "the repository is called and returns true"
        1 * medicalExpenseRepository.softDeleteByMedicalExpenseId(1L) >> 1
        result == true
    }

    def "should update claim status"() {
        when: "updating claim status"
        def result = medicalExpenseService.updateClaimStatus(1L, ClaimStatus.Paid)

        then: "the repository is called and returns true"
        1 * medicalExpenseRepository.updateClaimStatus(1L, ClaimStatus.Paid) >> 1
        result == true
    }

    def "should link payment transaction"() {
        given: "a medical expense"
        def medicalExpense = new MedicalExpense(medicalExpenseId: 1L)
        medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(1L) >> medicalExpense
        medicalExpenseRepository.findByTransactionId(456L) >> null
        medicalExpenseRepository.save(_ as MedicalExpense) >> medicalExpense

        when: "linking a payment transaction"
        def result = medicalExpenseService.linkPaymentTransaction(1L, 456L)

        then: "the transactionId is set and saved"
        result.transactionId == 456L
    }

    def "should throw exception when linking a transaction already linked"() {
        given: "a medical expense and a transaction already linked to another expense"
        def medicalExpense = new MedicalExpense(medicalExpenseId: 1L)
        def otherExpense = new MedicalExpense(medicalExpenseId: 2L, transactionId: 456L)
        medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(1L) >> medicalExpense
        medicalExpenseRepository.findByTransactionId(456L) >> otherExpense

        when: "linking the payment transaction"
        medicalExpenseService.linkPaymentTransaction(1L, 456L)

        then: "DuplicateMedicalExpenseException is thrown"
        thrown(DuplicateMedicalExpenseException)
    }

    def "should unlink payment transaction"() {
        given: "a medical expense with a linked transaction"
        def medicalExpense = new MedicalExpense(medicalExpenseId: 1L, transactionId: 456L, paidAmount: new BigDecimal("50.00"))
        medicalExpenseRepository.findByMedicalExpenseIdAndActiveStatusTrue(1L) >> medicalExpense
        medicalExpenseRepository.save(_ as MedicalExpense) >> medicalExpense

        when: "unlinking the payment transaction"
        def result = medicalExpenseService.unlinkPaymentTransaction(1L)

        then: "the transactionId and paidAmount are cleared"
        result.transactionId == null
        result.paidAmount == BigDecimal.ZERO
    }

    def "should get total billed amount by year"() {
        given: "a total billed amount"
        medicalExpenseRepository.getTotalBilledAmountByYear(2024) >> new BigDecimal("1000.00")

        when: "getting total billed amount by year"
        def result = medicalExpenseService.getTotalBilledAmountByYear(2024)

        then: "the correct total is returned"
        result == new BigDecimal("1000.00")
    }

    def "should return zero for total billed amount if repository returns null"() {
        given: "repository returns null"
        medicalExpenseRepository.getTotalBilledAmountByYear(2024) >> null

        when: "getting total billed amount by year"
        def result = medicalExpenseService.getTotalBilledAmountByYear(2024)

        then: "zero is returned"
        result == BigDecimal.ZERO
    }
}
