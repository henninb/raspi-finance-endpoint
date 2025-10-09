package finance.controllers.dto

import finance.domain.ClaimStatus
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.sql.Date

data class MedicalExpenseInputDto(
    val medicalExpenseId: Long? = null,
    @field:Min(value = 1, message = "Transaction ID must be positive when specified")
    val transactionId: Long? = null,
    @field:Min(value = 1, message = "Provider ID must be positive when specified")
    val providerId: Long? = null,
    @field:Min(value = 1, message = "Family member ID must be positive when specified")
    val familyMemberId: Long? = null,
    @field:NotNull(message = "Service date cannot be null")
    val serviceDate: Date?,
    @field:Size(max = 500, message = "Service description cannot exceed 500 characters")
    val serviceDescription: String? = null,
    @field:Size(max = 20, message = "Procedure code cannot exceed 20 characters")
    @field:Pattern(
        regexp = "^[A-Z0-9-]*$",
        message = "Procedure code can only contain uppercase letters, numbers, and hyphens",
    )
    val procedureCode: String? = null,
    @field:Size(max = 20, message = "Diagnosis code cannot exceed 20 characters")
    @field:Pattern(
        regexp = "^[A-Z0-9.-]*$",
        message = "Diagnosis code can only contain uppercase letters, numbers, periods, and hyphens",
    )
    val diagnosisCode: String? = null,
    @field:NotNull(message = "Billed amount cannot be null")
    @field:DecimalMin(value = "0.00", message = "Billed amount must be non-negative")
    @field:DecimalMax(value = "999999999.99", message = "Billed amount cannot exceed 999,999,999.99")
    @field:Digits(integer = 10, fraction = 2, message = "Billed amount must have at most 10 integer digits and 2 decimal places")
    val billedAmount: BigDecimal?,
    @field:NotNull(message = "Insurance discount cannot be null")
    @field:DecimalMin(value = "0.00", message = "Insurance discount must be non-negative")
    @field:DecimalMax(value = "999999999.99", message = "Insurance discount cannot exceed 999,999,999.99")
    @field:Digits(integer = 10, fraction = 2, message = "Insurance discount must have at most 10 integer digits and 2 decimal places")
    val insuranceDiscount: BigDecimal?,
    @field:NotNull(message = "Insurance paid amount cannot be null")
    @field:DecimalMin(value = "0.00", message = "Insurance paid amount must be non-negative")
    @field:DecimalMax(value = "999999999.99", message = "Insurance paid amount cannot exceed 999,999,999.99")
    @field:Digits(integer = 10, fraction = 2, message = "Insurance paid amount must have at most 10 integer digits and 2 decimal places")
    val insurancePaid: BigDecimal?,
    @field:NotNull(message = "Patient responsibility cannot be null")
    @field:DecimalMin(value = "0.00", message = "Patient responsibility must be non-negative")
    @field:DecimalMax(value = "999999999.99", message = "Patient responsibility cannot exceed 999,999,999.99")
    @field:Digits(integer = 10, fraction = 2, message = "Patient responsibility must have at most 10 integer digits and 2 decimal places")
    val patientResponsibility: BigDecimal?,
    val paidDate: Date? = null,
    @field:NotNull(message = "Out of network status cannot be null")
    val isOutOfNetwork: Boolean?,
    @field:NotNull(message = "Claim number cannot be null")
    @field:Size(max = 50, message = "Claim number cannot exceed 50 characters")
    @field:Pattern(
        regexp = "^[A-Z0-9-]*$",
        message = "Claim number can only contain uppercase letters, numbers, and hyphens",
    )
    val claimNumber: String?,
    @field:NotNull(message = "Claim status cannot be null")
    val claimStatus: ClaimStatus?,
    val activeStatus: Boolean? = null,
    @field:NotNull(message = "Paid amount cannot be null")
    @field:DecimalMin(value = "0.00", message = "Paid amount must be non-negative")
    @field:DecimalMax(value = "999999999.99", message = "Paid amount cannot exceed 999,999,999.99")
    @field:Digits(integer = 10, fraction = 2, message = "Paid amount must have at most 10 integer digits and 2 decimal places")
    val paidAmount: BigDecimal?,
)
