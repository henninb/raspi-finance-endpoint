package finance.controllers.dto

import finance.utils.Constants.FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class DescriptionInputDto(
    val descriptionId: Long? = null,
    @field:NotBlank
    @field:Size(min = 1, max = 50, message = FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE)
    val descriptionName: String,
    val activeStatus: Boolean? = null,
)
