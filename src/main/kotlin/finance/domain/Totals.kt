package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal

data class Totals(
    val totalsFuture: BigDecimal,
    val totalsCleared: BigDecimal,
    val totals: BigDecimal,
    val totalsOutstanding: BigDecimal
) {

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }

}
