package finance.utils

import org.apache.logging.log4j.LogManager
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class LowerCaseConverter : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(attribute: String?): String {
        if (attribute == null) {
            return ""
        }
        logger.debug("convertToDatabaseColumn - converted to lowercase")
        return attribute.lowercase()
    }

    override fun convertToEntityAttribute(attribute: String?): String {
        if (attribute == null) {
            return ""
        }

        logger.debug("convertToEntityAttribute - converted to lowercase")
        return attribute.lowercase()
    }

    companion object {
        private val logger = LogManager.getLogger()
    }
}
