package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
//@JsonDeserialize(as = AccountType.Undefined)
//@JsonFormat(shape = JsonFormat.Shape.OBJECT)
//@JsonFormat(default = AccountType.Undefined)
//@SerializedName(defaultValue = AccountType.Undefined)
enum class AccountType(val type: String) {
    @JsonProperty("credit")
    Credit("credit"),

    @JsonProperty("debit")
    Debit("debit"),

    @JsonProperty("undefined")
    Undefined("undefined");

    fun value() : String = type
    override fun toString(): String = name.toLowerCase()

    companion object {
        //private val VALUES = values();
        //fun getByValue(type: String) = VALUES.firstOrNull { it.type == type }
        fun from(type: String?): AccountType = values().find { it.name == type } ?: Undefined
        //fun valueOf(type: String): AccountType? = values().find { it.type == type }
        //private val map = AccountType.values().associateBy(AccountType::type)
    }
}