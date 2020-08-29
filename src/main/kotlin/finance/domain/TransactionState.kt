package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class TransactionState {
    @JsonProperty("cleared")
    Cleared,

    @JsonProperty("outstanding")
    Outstanding,

    @JsonProperty("future")
    Future,
        
    @JsonProperty("undefined")
    Undefined;

    override fun toString(): String {
        return name.toLowerCase()
    }
}