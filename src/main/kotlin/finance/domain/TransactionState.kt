package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class TransactionState {
    @JsonProperty("future")
    Future,

    @JsonProperty("cleared")
    Cleared,

    @JsonProperty("outstanding")
    Outstanding,

    @JsonProperty("undefined")
    Undefined;

    override fun toString(): String {
        return name.toLowerCase()
    }
}