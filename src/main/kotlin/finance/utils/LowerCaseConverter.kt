package finance.utils

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class LowerCaseConverter : AttributeConverter<String, String> {
    override fun convertToDatabaseColumn(attribute: String?): String = requireNotNull(attribute) { "Cannot persist null string field" }.lowercase()

    override fun convertToEntityAttribute(attribute: String?): String = requireNotNull(attribute) { "Null string read from non-nullable column" }.lowercase()
}
