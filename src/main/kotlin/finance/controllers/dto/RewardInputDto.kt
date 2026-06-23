package finance.controllers.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class RewardInputDto(
    val rewardId: Long? = null,
    @field:NotNull val accountId: Long,
    @field:DecimalMin("1.0") val multiplier: BigDecimal,
    @field:NotBlank @field:Size(min = 1, max = 50) val category: String,
    @field:DecimalMin("0.0001") val cpp: BigDecimal = BigDecimal("0.01"),
    val activeStatus: Boolean? = null,
)
