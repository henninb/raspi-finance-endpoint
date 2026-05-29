package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class TransactionType(
    override val label: String,
) : LabeledEnum {
    Expense("expense"),
    Income("income"),
    Transfer("transfer"),
    Undefined("undefined"),
    ;

    override fun toString(): String = label

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): TransactionType = fromLabelOrThrow(value)
    }
}
