package finance.utils

import javax.persistence.AttributeConverter
import javax.persistence.Converter
import java.sql.Date

@Converter
class DateTypeConverter : AttributeConverter<Date, Long> {

    override fun convertToDatabaseColumn(attribute: Date): Long {
        println("convertToDatabaseColumn - attribute=${attribute}")
        return (attribute.time / 1000)
    }

    override fun convertToEntityAttribute(attribute: Long): Date {
        println("convertToEntityAttribute - attribute=${attribute}")
        return Date(attribute * 1000)
    }
}
