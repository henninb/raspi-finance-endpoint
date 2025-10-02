package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class TransactionType(
    val label: String,
) {
    @JsonProperty("expense")
    Expense("expense"),

    @JsonProperty("income")
    Income("income"),

    @JsonProperty("transfer")
    Transfer("transfer"),

    @JsonProperty("undefined")
    Undefined("undefined"),
    ;

    // fun value() : String = type
    override fun toString(): String = name.lowercase()

    companion object
}
