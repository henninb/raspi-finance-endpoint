package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class MedicalProviderType(
    override val label: String,
) : LabeledEnum {
    General("general"),
    Specialist("specialist"),
    Hospital("hospital"),
    Pharmacy("pharmacy"),
    Laboratory("laboratory"),
    Imaging("imaging"),
    UrgentCare("urgent_care"),
    Emergency("emergency"),
    MentalHealth("mental_health"),
    Dental("dental"),
    Vision("vision"),
    PhysicalTherapy("physical_therapy"),
    Other("other"),
    ;

    override fun toString(): String = label

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): MedicalProviderType = fromLabelOrThrow(value)
    }
}
