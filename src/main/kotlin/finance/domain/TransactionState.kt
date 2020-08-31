package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.lang.Enum

@JsonFormat
enum class TransactionState {
    @JsonProperty("cleared")
    Cleared,

    @JsonProperty("outstanding")
    Outstanding,

    @JsonProperty("future")
    Future,
        
    @JsonProperty("undefined")
    Undefined;


//    override fun fromString(param: String): TransactionState {
//        val toUpper = param.capitalize()
//        return try {
//            valueOf(toUpper)
//        } catch (e: Exception) {
//            Undefined
//        }
//    }

//    override fun valueOf(parm: String) :TransactionState {
//
//    }

    override fun toString(): String {
        return name.toLowerCase()
    }
}