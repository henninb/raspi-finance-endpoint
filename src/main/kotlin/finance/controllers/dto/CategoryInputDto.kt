package finance.controllers.dto

import finance.utils.Constants.ALPHA_NUMERIC_NO_SPACE_PATTERN
import finance.utils.Constants.FIELD_MUST_BE_NUMERIC_NO_SPACE_MESSAGE
import finance.utils.Constants.FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CategoryInputDto(
    val categoryId: Long? = null,
    @field:Size(min = 1, max = 50, message = FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE)
    @field:Pattern(regexp = ALPHA_NUMERIC_NO_SPACE_PATTERN, message = FIELD_MUST_BE_NUMERIC_NO_SPACE_MESSAGE)
    val categoryName: String,
    val activeStatus: Boolean? = null,
)
