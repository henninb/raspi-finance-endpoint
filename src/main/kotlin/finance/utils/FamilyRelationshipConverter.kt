package finance.utils

import finance.domain.FamilyRelationship
import jakarta.persistence.Converter

@Converter(autoApply = true)
class FamilyRelationshipConverter : LabeledEnumConverter<FamilyRelationship>(FamilyRelationship::class.java)
