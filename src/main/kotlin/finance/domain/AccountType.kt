package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class AccountType {
    @JsonProperty("credit")
    Credit,

    @JsonProperty("debit")
    Debit,

    @JsonProperty("undefined")
    Undefined;

    override fun toString(): String {
        return name.toLowerCase()
    }

//    fun parseEAccountType(str: String): AccountType {
//        return when(str) {
//            "EXAMPLE5" -> AccountType.Undefined
//            else -> AccountType.valueOf(str)
//        }
//    }
}