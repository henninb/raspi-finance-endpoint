package finance.utils

import finance.domain.FamilyRelationship
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class FamilyRelationshipConverter : AttributeConverter<FamilyRelationship, String> {
    override fun convertToDatabaseColumn(attribute: FamilyRelationship): String {
        return attribute.label
    }

    override fun convertToEntityAttribute(attribute: String): FamilyRelationship {
        return when (attribute.trim().lowercase()) {
            "self" -> FamilyRelationship.Self
            "spouse" -> FamilyRelationship.Spouse
            "child" -> FamilyRelationship.Child
            "dependent" -> FamilyRelationship.Dependent
            "other" -> FamilyRelationship.Other
            else -> throw RuntimeException("Unknown family relationship attribute: $attribute")
        }
    }
}
