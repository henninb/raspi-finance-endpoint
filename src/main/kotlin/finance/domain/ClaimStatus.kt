package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class ClaimStatus(
    override val label: String,
) : LabeledEnum {
    Submitted("submitted"),
    Processing("processing"),
    Approved("approved"),
    Denied("denied"),
    Paid("paid"),
    Closed("closed"),
    ;

    override fun toString(): String = label

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): ClaimStatus = fromLabelOrThrow(value)

        @JvmStatic
        fun getValidStatuses(): List<String> = values().map { it.label }
    }
}
