package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal

data class Totals(
    val totalsFuture: BigDecimal,
    val totalsCleared: BigDecimal,
    val totals: BigDecimal,
    val totalsOutstanding: BigDecimal,
) {
    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = JsonMapper.builderWithJackson2Defaults().findAndAddModules().build()
    }
}
