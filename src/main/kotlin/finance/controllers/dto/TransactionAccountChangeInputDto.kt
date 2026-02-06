package finance.controllers.dto

import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_UUID_MESSAGE
import finance.utils.Constants.FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE
import finance.utils.Constants.UUID_PATTERN
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class TransactionAccountChangeInputDto(
    @field:NotBlank
    @field:Pattern(regexp = UUID_PATTERN, message = FIELD_MUST_BE_UUID_MESSAGE)
    val guid: String,
    @field:NotBlank
    @field:Size(min = 3, max = 40, message = FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE)
    @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    val accountNameOwner: String,
)
