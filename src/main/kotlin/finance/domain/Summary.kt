package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal

data class Summary(
    @param:JsonProperty
    var totals: BigDecimal,
    @param:JsonProperty
    var totalsCleared: BigDecimal,
    @param:JsonProperty
    var totalsOutstanding: BigDecimal,
    @param:JsonProperty
    var totalsFuture: BigDecimal,
) {
    constructor() : this(BigDecimal(0.00), BigDecimal(0.00), BigDecimal(0.00), BigDecimal(0.00))

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
