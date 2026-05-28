package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class FamilyRelationship(
    override val label: String,
) : LabeledEnum {
    @JsonProperty("self")
    Self("self"),

    @JsonProperty("spouse")
    Spouse("spouse"),

    @JsonProperty("child")
    Child("child"),

    @JsonProperty("dependent")
    Dependent("dependent"),

    @JsonProperty("other")
    Other("other"),
    ;

    override fun toString(): String = name.lowercase()

    companion object
}
