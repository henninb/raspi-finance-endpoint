package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate

data class BonusProgress(
    val accountNameOwner: String,
    val spent: BigDecimal,
    val spentPending: BigDecimal,
    val target: BigDecimal,
    val remaining: BigDecimal,
    val percentComplete: Double,
    val bonusAmount: BigDecimal,
    val bonusEarned: Boolean,
    val windowStartDate: LocalDate,
    val windowEndDate: LocalDate,
    val daysRemaining: Long,
) {
    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper().findAndRegisterModules()
    }
}
