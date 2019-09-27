package finance.pojos

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ResultMessage {
    var message = ""
    var guid = ""
    var resultCode: Int? = 0
    private var date = ZonedDateTime.now()

    fun getDate(): String {
        return DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a").format(this.date).toString()
    }

    fun setDate(date: ZonedDateTime) {
        this.date = date
    }
}
