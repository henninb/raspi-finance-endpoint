package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
//@JsonDeserialize(as = AccountType.Undefined)
//@JsonFormat(shape = JsonFormat.Shape.OBJECT)
//@JsonFormat(default = AccountType.Undefined)
//@SerializedName(defaultValue = AccountType.Undefined)
enum class AccountType(val label: String) {
    @JsonProperty("credit")
    Credit("credit"),

    @JsonProperty("debit")
    Debit("debit"),

    @JsonProperty("undefined")
    Undefined("undefined");

    //fun value() : String = type
    override fun toString(): String = name.lowercase()

//    fun parseAccountType(str: String): AccountType {
//        return when(str) {
//            "undefined" -> Undefined
//            else -> valueOf(str)
//        }
//    }

//    override fun valueof(): AccountType {
//
//        return Undefined
//    }

    companion object
}