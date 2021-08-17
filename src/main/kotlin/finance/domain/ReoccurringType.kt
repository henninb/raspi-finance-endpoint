package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class ReoccurringType(val label: String) {
    @JsonProperty("monthly")
    Monthly("monthly"),

    @JsonProperty("annually")
    Annually("annually"),

    @JsonProperty("biannually")
    BiAnnually("biannually"),

    @JsonProperty("fortnightly")
    FortNightly("fortnightly"),

    @JsonProperty("quarterly")
    Quarterly("quarterly"),

    @JsonProperty("onetime")
    Onetime("onetime"),

    @JsonProperty("undefined")
    Undefined("undefined");

    fun value(): String = label
    override fun toString(): String = name.lowercase()

    companion object {
    }
}