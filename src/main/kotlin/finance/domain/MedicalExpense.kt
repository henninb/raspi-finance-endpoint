package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import finance.utils.ClaimStatusConverter
import finance.utils.ValidDate
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

@Entity
@Table(name = "t_medical_expense")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MedicalExpense(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "medical_expense_id")
    var medicalExpenseId: Long = 0L,
    @Column(name = "transaction_id")
    @field:Min(value = 1, message = "Transaction ID must be positive when specified")
    var transactionId: Long? = null,
    @Column(name = "provider_id")
    @field:Min(value = 1, message = "Provider ID must be positive when specified")
    var providerId: Long? = null,
    @Column(name = "family_member_id")
    @field:Min(value = 1, message = "Family member ID must be positive when specified")
    var familyMemberId: Long? = null,
    @Column(name = "service_date", nullable = false)
    @field:NotNull(message = "Service date cannot be null")
    @field:ValidDate
    var serviceDate: Date = Date.valueOf("1900-01-01"),
    @Column(name = "service_description")
    @field:Size(max = 500, message = "Service description cannot exceed 500 characters")
    var serviceDescription: String? = null,
    @Column(name = "procedure_code")
    @field:Size(max = 20, message = "Procedure code cannot exceed 20 characters")
    @field:Pattern(
        regexp = "^[A-Z0-9-]*$",
        message = "Procedure code can only contain uppercase letters, numbers, and hyphens",
    )
    var procedureCode: String? = null,
    @Column(name = "diagnosis_code")
    @field:Size(max = 20, message = "Diagnosis code cannot exceed 20 characters")
    @field:Pattern(
        regexp = "^[A-Z0-9.-]*$",
        message = "Diagnosis code can only contain uppercase letters, numbers, periods, and hyphens",
    )
    var diagnosisCode: String? = null,
    @Column(name = "billed_amount", precision = 12, scale = 2, nullable = false)
    @field:NotNull(message = "Billed amount cannot be null")
    @field:DecimalMin(value = "0.00", message = "Billed amount must be non-negative")
    @field:DecimalMax(value = "999999999.99", message = "Billed amount cannot exceed 999,999,999.99")
    @field:Digits(integer = 10, fraction = 2, message = "Billed amount must have at most 10 integer digits and 2 decimal places")
    var billedAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "insurance_discount", precision = 12, scale = 2, nullable = false)
    @field:NotNull(message = "Insurance discount cannot be null")
    @field:DecimalMin(value = "0.00", message = "Insurance discount must be non-negative")
    @field:DecimalMax(value = "999999999.99", message = "Insurance discount cannot exceed 999,999,999.99")
    @field:Digits(integer = 10, fraction = 2, message = "Insurance discount must have at most 10 integer digits and 2 decimal places")
    var insuranceDiscount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "insurance_paid", precision = 12, scale = 2, nullable = false)
    @field:NotNull(message = "Insurance paid amount cannot be null")
    @field:DecimalMin(value = "0.00", message = "Insurance paid amount must be non-negative")
    @field:DecimalMax(value = "999999999.99", message = "Insurance paid amount cannot exceed 999,999,999.99")
    @field:Digits(integer = 10, fraction = 2, message = "Insurance paid amount must have at most 10 integer digits and 2 decimal places")
    var insurancePaid: BigDecimal = BigDecimal.ZERO,
    @Column(name = "patient_responsibility", precision = 12, scale = 2, nullable = false)
    @field:NotNull(message = "Patient responsibility cannot be null")
    @field:DecimalMin(value = "0.00", message = "Patient responsibility must be non-negative")
    @field:DecimalMax(value = "999999999.99", message = "Patient responsibility cannot exceed 999,999,999.99")
    @field:Digits(integer = 10, fraction = 2, message = "Patient responsibility must have at most 10 integer digits and 2 decimal places")
    var patientResponsibility: BigDecimal = BigDecimal.ZERO,
    @Column(name = "paid_date")
    @field:ValidDate
    var paidDate: Date? = null,
    @Column(name = "is_out_of_network", nullable = false)
    @field:NotNull(message = "Out of network status cannot be null")
    @get:JsonProperty("isOutOfNetwork")
    var isOutOfNetwork: Boolean = false,
    @Column(name = "claim_number")
    @field:NotNull(message = "Claim number cannot be null")
    @field:Size(max = 50, message = "Claim number cannot exceed 50 characters")
    @field:Pattern(
        regexp = "^[A-Z0-9-]*$",
        message = "Claim number can only contain uppercase letters, numbers, and hyphens",
    )
    var claimNumber: String = "",
    @Column(name = "claim_status", nullable = false)
    @field:NotNull(message = "Claim status cannot be null")
    @Convert(converter = ClaimStatusConverter::class)
    var claimStatus: ClaimStatus = ClaimStatus.Submitted,
    @Column(name = "active_status", nullable = false)
    @field:NotNull(message = "Active status cannot be null")
    var activeStatus: Boolean = true,
    @Column(name = "paid_amount", precision = 12, scale = 2, nullable = false)
    @field:NotNull(message = "Paid amount cannot be null")
    @field:DecimalMin(value = "0.00", message = "Paid amount must be non-negative")
    @field:DecimalMax(value = "999999999.99", message = "Paid amount cannot exceed 999,999,999.99")
    @field:Digits(integer = 10, fraction = 2, message = "Paid amount must have at most 10 integer digits and 2 decimal places")
    var paidAmount: BigDecimal = BigDecimal.ZERO,
) {
    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    @field:NotNull(message = "Date added cannot be null")
    var dateAdded: Timestamp = Timestamp(System.currentTimeMillis())

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    @field:NotNull(message = "Date updated cannot be null")
    var dateUpdated: Timestamp = Timestamp(System.currentTimeMillis())

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", insertable = false, updatable = false)
    @JsonIgnore
    var transaction: Transaction? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", insertable = false, updatable = false)
    @JsonIgnore
    var medicalProvider: MedicalProvider? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_member_id", insertable = false, updatable = false)
    @JsonIgnore
    var familyMember: FamilyMember? = null

    @Transient
    @JsonProperty("net_amount")
    fun getNetAmount(): BigDecimal = billedAmount - insuranceDiscount - insurancePaid

    @Transient
    @JsonProperty("total_covered")
    fun getTotalCovered(): BigDecimal = insuranceDiscount + insurancePaid

    @Transient
    @JsonProperty("is_fully_paid")
    fun isFullyPaid(): Boolean = paidAmount >= patientResponsibility

    @Transient
    @JsonProperty("unpaid_amount")
    fun getUnpaidAmount(): BigDecimal = (patientResponsibility - paidAmount).max(BigDecimal.ZERO)

    @Transient
    @JsonProperty("is_partially_paid")
    fun isPartiallyPaid(): Boolean = paidAmount > BigDecimal.ZERO && paidAmount < patientResponsibility

    @Transient
    @JsonProperty("is_unpaid")
    fun isUnpaid(): Boolean = paidAmount.compareTo(BigDecimal.ZERO) == 0 && patientResponsibility.compareTo(BigDecimal.ZERO) > 0

    @Transient
    @JsonProperty("is_overpaid")
    fun isOverpaid(): Boolean = paidAmount > patientResponsibility

    @Transient
    @JsonProperty("coverage_percentage")
    fun getCoveragePercentage(): BigDecimal =
        if (billedAmount != BigDecimal.ZERO) {
            getTotalCovered()
                .divide(billedAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
        } else {
            BigDecimal.ZERO
        }

    @PrePersist
    fun prePersist() {
        val now = Timestamp(System.currentTimeMillis())
        if (dateAdded.time == 0L) {
            dateAdded = now
        }
        dateUpdated = now
        validateFinancialConsistency()
    }

    @PreUpdate
    fun preUpdate() {
        dateUpdated = Timestamp(System.currentTimeMillis())
        validateFinancialConsistency()
    }

    private fun validateFinancialConsistency() {
        val totalAllocated = insuranceDiscount + insurancePaid + patientResponsibility
        if (totalAllocated > billedAmount) {
            throw IllegalStateException(
                "Total allocated amount ($totalAllocated) cannot exceed billed amount ($billedAmount)",
            )
        }
    }

    override fun toString(): String = mapper.writeValueAsString(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MedicalExpense) return false
        return medicalExpenseId == other.medicalExpenseId
    }

    override fun hashCode(): Int = medicalExpenseId.hashCode()

    companion object {
        @JsonIgnore
        private val mapper =
            ObjectMapper().apply {
                setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                findAndRegisterModules()
                configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
    }
}
