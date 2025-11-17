package finance.controllers.dto

import finance.utils.Constants.FIELD_MUST_BE_UUID_MESSAGE
import finance.utils.Constants.UUID_PATTERN
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal
import java.time.LocalDate

data class TransferInputDto(
    val transferId: Long? = null,
    @field:NotBlank
    val sourceAccount: String,
    @field:NotBlank
    val destinationAccount: String,
    @field:NotNull
    val transactionDate: LocalDate,
    @field:NotNull
    @field:DecimalMin("0.01")
    val amount: BigDecimal,
    @field:Pattern(regexp = UUID_PATTERN, message = FIELD_MUST_BE_UUID_MESSAGE)
    val guidSource: String? = null,
    @field:Pattern(regexp = UUID_PATTERN, message = FIELD_MUST_BE_UUID_MESSAGE)
    val guidDestination: String? = null,
    val activeStatus: Boolean? = null,
)
