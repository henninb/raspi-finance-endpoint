package finance.utils

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class LowerCaseConverter : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(attribute: String): String {
        return attribute.toLowerCase()
    }

    override fun convertToEntityAttribute(attribute: String): String {
        return attribute.toLowerCase()
    }
}
