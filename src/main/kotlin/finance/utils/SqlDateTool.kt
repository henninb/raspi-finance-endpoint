package finance.utils

import java.text.SimpleDateFormat
import java.sql.Date
import java.util.*

class SqlDateTool {
    fun stringToSqlDate(stringDate: String): Date {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.isLenient = false
//        simpleDateFormat.timeZone = TimeZone.getDefault()
//        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return Date(simpleDateFormat.parse(stringDate).time)
    }

    fun sqlDateToString(sqlDate: Date): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.isLenient = false
        // simpleDateFormat.timeZone = TimeZone.getDefault()
        // simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return simpleDateFormat.format(sqlDate)
    }

    fun currentSqlDate(): Date {
        return Date(Calendar.getInstance().timeInMillis)
    }
}
