package finance.utils

import finance.domain.TransactionState
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class TransactionStateConverter : AttributeConverter<TransactionState, String> {

    override fun convertToDatabaseColumn(attribute: TransactionState): String {
        return when (attribute) {
            TransactionState.Outstanding -> "outstanding"
            TransactionState.Future -> "future"
            TransactionState.Cleared -> "cleared"
            TransactionState.Undefined -> "undefined"
        }
    }

    override fun convertToEntityAttribute(attribute: String): TransactionState {
        return when (attribute.trim().lowercase()) {
            "outstanding" -> TransactionState.Outstanding
            "future" -> TransactionState.Future
            "cleared" -> TransactionState.Cleared
            "undefined" -> TransactionState.Undefined
            else -> throw RuntimeException("Unknown attribute: $attribute")
        }
    }
}
