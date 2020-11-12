package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class TransactionState(val state: String) {
    @JsonProperty("cleared")
    Cleared("cleared"),

    @JsonProperty("outstanding")
    Outstanding("outstanding"),

    @JsonProperty("future")
    Future("future"),

    @JsonProperty("undefined")
    Undefined("undefined");

    fun value() : String = state
    override fun toString(): String = name.toLowerCase()

    companion object {
        private val VALUES = values();
        fun getByValue(state: String) = VALUES.firstOrNull { it.state == state }
    }
}