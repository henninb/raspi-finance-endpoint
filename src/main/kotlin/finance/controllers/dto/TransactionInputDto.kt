package finance.controllers.dto

import finance.domain.AccountType
import finance.domain.ReoccurringType
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.utils.Constants.ALPHA_NUMERIC_NO_SPACE_PATTERN
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.ASCII_PATTERN
import finance.utils.Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_ASCII_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_NUMERIC_NO_SPACE_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_UUID_MESSAGE
import finance.utils.Constants.FILED_MUST_BE_BETWEEN_ONE_AND_SEVENTY_FIVE_MESSAGE
import finance.utils.Constants.FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE
import finance.utils.Constants.FILED_MUST_BE_BETWEEN_ZERO_AND_FIFTY_MESSAGE
import finance.utils.Constants.UUID_PATTERN
import finance.utils.ValidDate
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class TransactionInputDto(
    val transactionId: Long? = null,
    @field:Pattern(regexp = UUID_PATTERN, message = FIELD_MUST_BE_UUID_MESSAGE)
    val guid: String? = null,
    @field:Min(value = 0L)
    val accountId: Long? = null,
    @field:NotNull
    val accountType: AccountType,
    @field:NotNull
    val transactionType: TransactionType,
    @field:NotBlank
    @field:Size(min = 3, max = 40, message = FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE)
    @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    val accountNameOwner: String,
    @field:NotNull
    @field:ValidDate
    val transactionDate: LocalDate,
    @field:NotBlank
    @field:Size(min = 1, max = 75, message = FILED_MUST_BE_BETWEEN_ONE_AND_SEVENTY_FIVE_MESSAGE)
    @field:Pattern(regexp = ASCII_PATTERN, message = FIELD_MUST_BE_ASCII_MESSAGE)
    val description: String,
    @field:NotBlank
    @field:Size(max = 50, message = FILED_MUST_BE_BETWEEN_ZERO_AND_FIFTY_MESSAGE)
    @field:Pattern(regexp = ALPHA_NUMERIC_NO_SPACE_PATTERN, message = FIELD_MUST_BE_NUMERIC_NO_SPACE_MESSAGE)
    val category: String,
    @field:NotNull
    @field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    val amount: BigDecimal,
    @field:NotNull
    val transactionState: TransactionState,
    val activeStatus: Boolean? = null,
    val reoccurringType: ReoccurringType? = null,
    @field:Size(max = 100)
    @field:Pattern(regexp = ASCII_PATTERN, message = FIELD_MUST_BE_ASCII_MESSAGE)
    val notes: String? = null,
    @field:ValidDate
    val dueDate: LocalDate? = null,
    @field:Min(value = 0L)
    val receiptImageId: Long? = null,
)
