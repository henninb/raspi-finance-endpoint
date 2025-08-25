package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class MedicalProviderType(val label: String) {
    @JsonProperty("general")
    General("general"),

    @JsonProperty("specialist")
    Specialist("specialist"),

    @JsonProperty("hospital")
    Hospital("hospital"),

    @JsonProperty("pharmacy")
    Pharmacy("pharmacy"),

    @JsonProperty("laboratory")
    Laboratory("laboratory"),

    @JsonProperty("imaging")
    Imaging("imaging"),

    @JsonProperty("urgent_care")
    UrgentCare("urgent_care"),

    @JsonProperty("emergency")
    Emergency("emergency"),

    @JsonProperty("mental_health")
    MentalHealth("mental_health"),

    @JsonProperty("dental")
    Dental("dental"),

    @JsonProperty("vision")
    Vision("vision"),

    @JsonProperty("physical_therapy")
    PhysicalTherapy("physical_therapy"),

    @JsonProperty("other")
    Other("other");

    override fun toString(): String = name.lowercase()

    companion object
}