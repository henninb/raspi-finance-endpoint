package finance.utils

import finance.domain.MedicalProviderType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class MedicalProviderTypeConverter : AttributeConverter<MedicalProviderType, String> {
    override fun convertToDatabaseColumn(attribute: MedicalProviderType): String {
        return attribute.label
    }

    override fun convertToEntityAttribute(attribute: String): MedicalProviderType {
        return when (attribute.trim().lowercase()) {
            "general" -> MedicalProviderType.General
            "specialist" -> MedicalProviderType.Specialist
            "hospital" -> MedicalProviderType.Hospital
            "pharmacy" -> MedicalProviderType.Pharmacy
            "laboratory" -> MedicalProviderType.Laboratory
            "imaging" -> MedicalProviderType.Imaging
            "urgent_care" -> MedicalProviderType.UrgentCare
            "emergency" -> MedicalProviderType.Emergency
            "mental_health" -> MedicalProviderType.MentalHealth
            "dental" -> MedicalProviderType.Dental
            "vision" -> MedicalProviderType.Vision
            "physical_therapy" -> MedicalProviderType.PhysicalTherapy
            "other" -> MedicalProviderType.Other
            else -> throw RuntimeException("Unknown medical provider type attribute: $attribute")
        }
    }
}
