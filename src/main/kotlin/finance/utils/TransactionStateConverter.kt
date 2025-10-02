package finance.utils

import finance.domain.TransactionState
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class TransactionStateConverter : AttributeConverter<TransactionState, String> {
    override fun convertToDatabaseColumn(attribute: TransactionState): String =
        when (attribute) {
            TransactionState.Outstanding -> "outstanding"
            TransactionState.Future -> "future"
            TransactionState.Cleared -> "cleared"
            TransactionState.Undefined -> "undefined"
        }

    override fun convertToEntityAttribute(attribute: String): TransactionState =
        when (attribute.trim().lowercase()) {
            "outstanding" -> TransactionState.Outstanding
            "future" -> TransactionState.Future
            "cleared" -> TransactionState.Cleared
            "undefined" -> TransactionState.Undefined
            else -> throw RuntimeException("Unknown attribute: $attribute")
        }
}
