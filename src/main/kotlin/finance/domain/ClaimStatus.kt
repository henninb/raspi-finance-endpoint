package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class ClaimStatus(val label: String) {
    Submitted("submitted"),
    Processing("processing"),
    Approved("approved"),
    Denied("denied"),
    Paid("paid"),
    Closed("closed"),
    ;

    @JsonValue
    fun toValue(): String = label

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): ClaimStatus {
            if (value.isNullOrBlank()) {
                throw IllegalArgumentException("Unknown claim status: $value")
            }
            return values().find { it.label.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown claim status: $value")
        }

        @JvmStatic
        fun getValidStatuses(): List<String> = values().map { it.label }
    }

    override fun toString(): String = label
}
