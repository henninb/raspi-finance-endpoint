package finance.utils

import org.apache.logging.log4j.LogManager
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class LowerCaseConverter : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(attribute: String?): String {
        if (attribute == null) {
            return ""
        }
        logger.debug("convertToDatabaseColumn - converted to lowercase")
        return attribute.toLowerCase()
    }

    override fun convertToEntityAttribute(attribute: String?): String {
        if (attribute == null) {
            return ""
        }

        logger.debug("convertToEntityAttribute - converted to lowercase")
        return attribute.toLowerCase()
    }

    companion object {
        private val logger = LogManager.getLogger()
    }
}
