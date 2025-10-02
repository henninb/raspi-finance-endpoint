package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class NetworkStatus(
    val label: String,
) {
    @JsonProperty("in_network")
    InNetwork("in_network"),

    @JsonProperty("out_of_network")
    OutOfNetwork("out_of_network"),

    @JsonProperty("unknown")
    Unknown("unknown"),
    ;

    override fun toString(): String = name.lowercase()

    companion object
}
