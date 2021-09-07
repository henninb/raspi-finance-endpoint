package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal

data class Summary(
    @JsonProperty
    var totals: BigDecimal,

    @JsonProperty
    var totalsCleared: BigDecimal,

    @JsonProperty
    var totalsOutstanding: BigDecimal,

    @JsonProperty
    var totalsFuture: BigDecimal
    ) {

    constructor() : this(BigDecimal(0.00), BigDecimal(0.00),BigDecimal(0.00),BigDecimal(0.00))

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}