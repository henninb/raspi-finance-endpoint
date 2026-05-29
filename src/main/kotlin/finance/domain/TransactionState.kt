package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class TransactionState(
    override val label: String,
) : LabeledEnum {
    Cleared("cleared"),
    Outstanding("outstanding"),
    Future("future"),
    Undefined("undefined"),
    ;

    override fun toString(): String = label

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): TransactionState = fromLabelOrThrow(value)
    }
}
