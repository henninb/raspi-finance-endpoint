package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class TransactionState(val label: String) {
    @JsonProperty("cleared")
    Cleared("cleared"),

    @JsonProperty("outstanding")
    Outstanding("outstanding"),

    @JsonProperty("future")
    Future("future"),

    @JsonProperty("undefined")
    Undefined("undefined"),
    ;

    fun value(): String = label

    override fun toString(): String = name.lowercase()

    companion object {
        private val VALUES = values()

        fun getByValue(label: String) = VALUES.firstOrNull { it.label == label }
    }
}
