package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class FamilyRelationship(
    override val label: String,
) : LabeledEnum {
    Self("self"),
    Spouse("spouse"),
    Child("child"),
    Dependent("dependent"),
    Other("other"),
    ;

    override fun toString(): String = label

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): FamilyRelationship = fromLabelOrThrow(value)
    }
}
