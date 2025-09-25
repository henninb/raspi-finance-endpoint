package finance.helpers

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense

import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.*

class MedicalExpenseBuilder {

    private static final String AMOUNT_25 = "25.00"

    Long medicalExpenseId = 0L
    Long transactionId = null
    Long providerId = 1L
    Long familyMemberId = 1L
    Date serviceDate = Date.valueOf('2024-01-15')
    String serviceDescription = 'Medical service'
    String procedureCode = 'PROC123'
    String diagnosisCode = 'DIAG456'
    BigDecimal billedAmount = new BigDecimal("150.00")
    BigDecimal insuranceDiscount = new BigDecimal(AMOUNT_25)
    BigDecimal insurancePaid = new BigDecimal("100.00")
    BigDecimal patientResponsibility = new BigDecimal(AMOUNT_25)
    Date paidDate = null
    Boolean isOutOfNetwork = false
    String claimNumber = 'CLAIM-2024-001'
    ClaimStatus claimStatus = ClaimStatus.Submitted
    Boolean activeStatus = true
    Timestamp dateAdded = new Timestamp(Calendar.getInstance().time.time)
    Timestamp dateUpdated = new Timestamp(Calendar.getInstance().time.time)
    BigDecimal paidAmount = new BigDecimal("0.00")

    static MedicalExpenseBuilder builder() {
        new MedicalExpenseBuilder()
    }

    MedicalExpense build() {
        MedicalExpense expense = new MedicalExpense().with {
            medicalExpenseId = this.medicalExpenseId
            transactionId = this.transactionId
            providerId = this.providerId
            familyMemberId = this.familyMemberId
            serviceDate = this.serviceDate
            serviceDescription = this.serviceDescription
            procedureCode = this.procedureCode
            diagnosisCode = this.diagnosisCode
            billedAmount = this.billedAmount
            insuranceDiscount = this.insuranceDiscount
            insurancePaid = this.insurancePaid
            patientResponsibility = this.patientResponsibility
            paidDate = this.paidDate
            isOutOfNetwork = this.isOutOfNetwork
            claimNumber = this.claimNumber
            claimStatus = this.claimStatus
            activeStatus = this.activeStatus
            dateAdded = this.dateAdded
            dateUpdated = this.dateUpdated
            paidAmount = this.paidAmount
            it
        }
        expense
    }

    MedicalExpenseBuilder withMedicalExpenseId(Long medicalExpenseId) {
        this.medicalExpenseId = medicalExpenseId
        this
    }

    MedicalExpenseBuilder withTransactionId(Long transactionId) {
        this.transactionId = transactionId
        this
    }

    MedicalExpenseBuilder withProviderId(Long providerId) {
        this.providerId = providerId
        this
    }

    MedicalExpenseBuilder withFamilyMemberId(Long familyMemberId) {
        this.familyMemberId = familyMemberId
        this
    }

    MedicalExpenseBuilder withServiceDate(Date serviceDate) {
        this.serviceDate = serviceDate
        this
    }

    MedicalExpenseBuilder withServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription
        this
    }

    MedicalExpenseBuilder withProcedureCode(String procedureCode) {
        this.procedureCode = procedureCode
        this
    }

    MedicalExpenseBuilder withDiagnosisCode(String diagnosisCode) {
        this.diagnosisCode = diagnosisCode
        this
    }

    MedicalExpenseBuilder withBilledAmount(BigDecimal billedAmount) {
        this.billedAmount = billedAmount
        this
    }

    MedicalExpenseBuilder withAmount(BigDecimal amount) {
        this.billedAmount = amount
        this
    }

    MedicalExpenseBuilder withInsuranceDiscount(BigDecimal insuranceDiscount) {
        this.insuranceDiscount = insuranceDiscount
        this
    }

    MedicalExpenseBuilder withInsurancePaid(BigDecimal insurancePaid) {
        this.insurancePaid = insurancePaid
        this
    }

    MedicalExpenseBuilder withPatientResponsibility(BigDecimal patientResponsibility) {
        this.patientResponsibility = patientResponsibility
        this
    }

    MedicalExpenseBuilder withPaidDate(Date paidDate) {
        this.paidDate = paidDate
        this
    }

    MedicalExpenseBuilder withIsOutOfNetwork(Boolean isOutOfNetwork) {
        this.isOutOfNetwork = isOutOfNetwork
        this
    }

    MedicalExpenseBuilder withClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber
        this
    }

    MedicalExpenseBuilder withClaimStatus(ClaimStatus claimStatus) {
        this.claimStatus = claimStatus
        this
    }

    MedicalExpenseBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        this
    }

    MedicalExpenseBuilder withDateAdded(Timestamp dateAdded) {
        this.dateAdded = dateAdded
        this
    }

    MedicalExpenseBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        this
    }

    MedicalExpenseBuilder withPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount
        this
    }
}
