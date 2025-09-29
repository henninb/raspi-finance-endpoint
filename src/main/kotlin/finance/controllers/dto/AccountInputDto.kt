package finance.controllers.dto

import finance.domain.AccountType
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_FOUR_DIGITS_MESSAGE
import finance.utils.Constants.FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.sql.Timestamp

data class AccountInputDto(
    val accountId: Long? = null,
    @field:NotBlank
    @field:Size(min = 3, max = 40, message = FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE)
    @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    val accountNameOwner: String,
    @field:NotNull
    val accountType: AccountType,
    val activeStatus: Boolean? = null,
    @field:Pattern(regexp = "^[0-9]{4}$", message = FIELD_MUST_BE_FOUR_DIGITS_MESSAGE)
    val moniker: String? = null,
    @field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    val outstanding: BigDecimal? = null,
    @field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    val cleared: BigDecimal? = null,
    @field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    val future: BigDecimal? = null,
    val dateClosed: Timestamp? = null,
    val validationDate: Timestamp? = null
)