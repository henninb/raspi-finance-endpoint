package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class AccountType(val label: String) {
    @JsonProperty("credit")
    Credit("credit"),

    @JsonProperty("debit")
    Debit("debit"),

    @JsonProperty("undefined")
    Undefined("undefined");

    override fun toString(): String = name.lowercase()

    companion object
}