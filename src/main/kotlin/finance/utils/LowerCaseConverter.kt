package finance.utils

import org.apache.logging.log4j.LogManager
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class LowerCaseConverter : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(attribute: String): String {
        return try {
            attribute.toLowerCase()
        } catch( e: NullPointerException) {
            logger.info(e.message)
            ""
        }
    }

    override fun convertToEntityAttribute(attribute: String): String {
        return try {
            attribute.toLowerCase()
        } catch( e: NullPointerException) {
            logger.info(e.message)
            ""
        }
    }

    companion object {
        private val logger = LogManager.getLogger()
    }
}
