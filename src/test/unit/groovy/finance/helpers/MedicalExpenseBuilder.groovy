package finance.helpers

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense

import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.*

class MedicalExpenseBuilder {

    Long medicalExpenseId = 0L
    Long transactionId = null
    Long providerId = 1L
    Long familyMemberId = 1L
    Date serviceDate = Date.valueOf('2024-01-15')
    String serviceDescription = 'Medical service'
    String procedureCode = 'PROC123'
    String diagnosisCode = 'DIAG456'
    BigDecimal billedAmount = new BigDecimal("150.00")
    BigDecimal insuranceDiscount = new BigDecimal("25.00")
    BigDecimal insurancePaid = new BigDecimal("100.00")
    BigDecimal patientResponsibility = new BigDecimal("25.00")
    Date paidDate = null
    Boolean isOutOfNetwork = false
    String claimNumber = 'CLAIM-2024-001'
    ClaimStatus claimStatus = ClaimStatus.Submitted
    Boolean activeStatus = true
    Timestamp dateAdded = new Timestamp(Calendar.getInstance().time.time)
    Timestamp dateUpdated = new Timestamp(Calendar.getInstance().time.time)
    BigDecimal paidAmount = new BigDecimal("0.00")

    static MedicalExpenseBuilder builder() {
        return new MedicalExpenseBuilder()
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
            return it
        }
        return expense
    }

    MedicalExpenseBuilder withMedicalExpenseId(Long medicalExpenseId) {
        this.medicalExpenseId = medicalExpenseId
        return this
    }

    MedicalExpenseBuilder withTransactionId(Long transactionId) {
        this.transactionId = transactionId
        return this
    }

    MedicalExpenseBuilder withProviderId(Long providerId) {
        this.providerId = providerId
        return this
    }

    MedicalExpenseBuilder withFamilyMemberId(Long familyMemberId) {
        this.familyMemberId = familyMemberId
        return this
    }

    MedicalExpenseBuilder withServiceDate(Date serviceDate) {
        this.serviceDate = serviceDate
        return this
    }

    MedicalExpenseBuilder withServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription
        return this
    }

    MedicalExpenseBuilder withProcedureCode(String procedureCode) {
        this.procedureCode = procedureCode
        return this
    }

    MedicalExpenseBuilder withDiagnosisCode(String diagnosisCode) {
        this.diagnosisCode = diagnosisCode
        return this
    }

    MedicalExpenseBuilder withBilledAmount(BigDecimal billedAmount) {
        this.billedAmount = billedAmount
        return this
    }

    MedicalExpenseBuilder withAmount(BigDecimal amount) {
        this.billedAmount = amount
        return this
    }

    MedicalExpenseBuilder withInsuranceDiscount(BigDecimal insuranceDiscount) {
        this.insuranceDiscount = insuranceDiscount
        return this
    }

    MedicalExpenseBuilder withInsurancePaid(BigDecimal insurancePaid) {
        this.insurancePaid = insurancePaid
        return this
    }

    MedicalExpenseBuilder withPatientResponsibility(BigDecimal patientResponsibility) {
        this.patientResponsibility = patientResponsibility
        return this
    }

    MedicalExpenseBuilder withPaidDate(Date paidDate) {
        this.paidDate = paidDate
        return this
    }

    MedicalExpenseBuilder withIsOutOfNetwork(Boolean isOutOfNetwork) {
        this.isOutOfNetwork = isOutOfNetwork
        return this
    }

    MedicalExpenseBuilder withClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber
        return this
    }

    MedicalExpenseBuilder withClaimStatus(ClaimStatus claimStatus) {
        this.claimStatus = claimStatus
        return this
    }

    MedicalExpenseBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    MedicalExpenseBuilder withDateAdded(Timestamp dateAdded) {
        this.dateAdded = dateAdded
        return this
    }

    MedicalExpenseBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        return this
    }

    MedicalExpenseBuilder withPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount
        return this
    }
}