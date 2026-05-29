package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class NetworkStatus(
    override val label: String,
) : LabeledEnum {
    InNetwork("in_network"),
    OutOfNetwork("out_of_network"),
    Unknown("unknown"),
    ;

    override fun toString(): String = label

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): NetworkStatus = fromLabelOrThrow(value)
    }
}
