package finance.utils

import javax.persistence.AttributeConverter
import javax.persistence.Converter
import java.sql.Date

@Converter
class DateTypeConverter : AttributeConverter<Date, Long> {

    override fun convertToDatabaseColumn(attribute: Date): Long {
        return 0L
    }

    override fun convertToEntityAttribute(attribute: Long): Date {
        return Date(0)
    }
}
