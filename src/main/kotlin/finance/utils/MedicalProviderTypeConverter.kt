package finance.utils

import finance.domain.MedicalProviderType
import jakarta.persistence.Converter

@Converter
class MedicalProviderTypeConverter : LabeledEnumConverter<MedicalProviderType>(MedicalProviderType::class.java)
