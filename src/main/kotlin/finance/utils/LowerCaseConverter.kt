package finance.utils

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class LowerCaseConverter : AttributeConverter<String, String> {
    override fun convertToDatabaseColumn(attribute: String?): String = attribute?.lowercase() ?: ""

    override fun convertToEntityAttribute(attribute: String?): String = attribute?.lowercase() ?: ""
}
