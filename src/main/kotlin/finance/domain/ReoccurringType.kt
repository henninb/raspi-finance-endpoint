package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class ReoccurringType(
    override val label: String,
) : LabeledEnum {
    Monthly("monthly"),
    Annually("annually"),
    BiAnnually("biannually"),
    FortNightly("fortnightly"),
    Quarterly("quarterly"),
    Onetime("onetime"),
    Undefined("undefined"),
    ;

    override fun toString(): String = label

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): ReoccurringType = fromLabelOrThrow(value)
    }
}
