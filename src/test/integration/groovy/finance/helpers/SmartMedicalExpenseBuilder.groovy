package finance.helpers

import finance.domain.MedicalExpense
import finance.domain.ClaimStatus
import groovy.util.logging.Slf4j
import java.math.BigDecimal
import java.sql.Date
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class SmartMedicalExpenseBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger(0)

    private String testOwner
    private Long transactionId = 1001L
    private Long providerId = null
    private Long familyMemberId = null
    private LocalDate serviceDate = LocalDate.parse("2024-01-15")
    private String serviceDescription
    private String procedureCode = "99396"
    private String diagnosisCode = "Z00.00"
    private BigDecimal billedAmount = new BigDecimal("350.00")
    private BigDecimal insuranceDiscount = new BigDecimal("50.00")
    private BigDecimal insurancePaid = new BigDecimal("250.00")
    private BigDecimal patientResponsibility = new BigDecimal("50.00")
    private LocalDate paidDate = null
    private Boolean isOutOfNetwork = false
    private String claimNumber
    private ClaimStatus claimStatus = ClaimStatus.Submitted
    private Boolean activeStatus = true

    private SmartMedicalExpenseBuilder(String testOwner) {
        this.testOwner = testOwner
        // Generate unique service description and claim number
        this.serviceDescription = generateUniqueServiceDescription()
        this.claimNumber = generateUniqueClaimNumber()
    }

    static SmartMedicalExpenseBuilder builderForOwner(String testOwner) {
        return new SmartMedicalExpenseBuilder(testOwner)
    }

    private String generateUniqueServiceDescription() {
        String counter = COUNTER.incrementAndGet().toString()
        String ownerPart = testOwner.replaceAll(/[^a-zA-Z0-9]/, '')

        if (ownerPart.isEmpty()) {
            ownerPart = "test"
        }

        return "Medical Service ${counter} for ${ownerPart}"
    }

    private String generateUniqueClaimNumber() {
        String counter = COUNTER.get().toString()
        return "CLM-${counter.padLeft(6, '0')}"
    }

    SmartMedicalExpenseBuilder withTransactionId(Long transactionId) {
        this.transactionId = transactionId
        return this
    }

    SmartMedicalExpenseBuilder withProviderId(Long providerId) {
        this.providerId = providerId
        return this
    }

    SmartMedicalExpenseBuilder withFamilyMemberId(Long familyMemberId) {
        this.familyMemberId = familyMemberId
        return this
    }

    SmartMedicalExpenseBuilder withServiceDate(Date serviceDate) {
        this.serviceDate = serviceDate?.toLocalDate()
        return this
    }

    SmartMedicalExpenseBuilder withServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate
        return this
    }

    SmartMedicalExpenseBuilder withServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription
        return this
    }

    SmartMedicalExpenseBuilder withBilledAmount(BigDecimal billedAmount) {
        this.billedAmount = billedAmount
        // Auto-adjust other amounts to maintain financial consistency
        // Keep the insurance discount at 0 for simplicity
        this.insuranceDiscount = BigDecimal.ZERO
        // Set insurance paid to 70% of billed amount
        this.insurancePaid = billedAmount.multiply(new BigDecimal("0.70")).setScale(2, BigDecimal.ROUND_HALF_UP)
        // Set patient responsibility to remaining amount
        this.patientResponsibility = billedAmount.subtract(this.insurancePaid).setScale(2, BigDecimal.ROUND_HALF_UP)
        return this
    }

    SmartMedicalExpenseBuilder withInsurancePaid(BigDecimal insurancePaid) {
        this.insurancePaid = insurancePaid
        return this
    }

    SmartMedicalExpenseBuilder withPatientResponsibility(BigDecimal patientResponsibility) {
        this.patientResponsibility = patientResponsibility
        return this
    }

    SmartMedicalExpenseBuilder withClaimStatus(ClaimStatus claimStatus) {
        this.claimStatus = claimStatus
        return this
    }

    SmartMedicalExpenseBuilder withOutOfNetwork(Boolean isOutOfNetwork) {
        this.isOutOfNetwork = isOutOfNetwork
        return this
    }

    MedicalExpense build() {
        MedicalExpense medicalExpense = new MedicalExpense().with {
            owner = this.testOwner
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
            return it
        }
        return medicalExpense
    }

    MedicalExpense buildAndValidate() {
        MedicalExpense medicalExpense = build()
        validateConstraints(medicalExpense)
        return medicalExpense
    }

    private void validateConstraints(MedicalExpense medicalExpense) {
        // Validate financial consistency
        BigDecimal totalAllocated = medicalExpense.insuranceDiscount +
                                   medicalExpense.insurancePaid +
                                   medicalExpense.patientResponsibility

        if (medicalExpense.billedAmount < totalAllocated) {
            throw new IllegalStateException("Billed amount (${medicalExpense.billedAmount}) must be >= total allocated (${totalAllocated})")
        }

        // Validate amounts are non-negative
        if (medicalExpense.billedAmount < BigDecimal.ZERO ||
            medicalExpense.insuranceDiscount < BigDecimal.ZERO ||
            medicalExpense.insurancePaid < BigDecimal.ZERO ||
            medicalExpense.patientResponsibility < BigDecimal.ZERO) {
            throw new IllegalStateException("All financial amounts must be non-negative")
        }

        // Validate service date is not in the future
        if (medicalExpense.serviceDate != null && medicalExpense.serviceDate.isAfter(LocalDate.now())) {
            throw new IllegalStateException("Service date cannot be in the future")
        }

        log.debug("Generated medical expense for transaction: ${medicalExpense.transactionId}, owner: ${testOwner}")
    }
}
