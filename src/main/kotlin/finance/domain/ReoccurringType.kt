package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class ReoccurringType(val type: String) {
    @JsonProperty("monthly")
    Monthly("monthly"),

    @JsonProperty("annually")
    Annually("annually"),

    @JsonProperty("bi_annually")
    BiAnnually("bi_annually"),

    @JsonProperty("every_two_weeks")
    EveryTwoWeeks("every_two_weeks"),

    @JsonProperty("undefined")
    Undefined("undefined");

    fun value(): String = type
    override fun toString(): String = name.toLowerCase()

    companion object {
        //private val VALUES = values();
        //fun getByValue(type: String) = VALUES.firstOrNull { it.type == type }
    }
}