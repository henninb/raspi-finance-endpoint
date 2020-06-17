package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class AccountType {
    @JsonProperty("credit")
    Credit,

    @JsonProperty("debit")
    Debit,
    Undefined;

    override fun toString(): String {
        println("toString - name: ${name.toLowerCase()}")
        return name.toLowerCase()
    }
}