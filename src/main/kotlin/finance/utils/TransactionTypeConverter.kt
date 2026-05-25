package finance.utils

import finance.domain.TransactionType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class TransactionTypeConverter : AttributeConverter<TransactionType, String> {

    override fun convertToDatabaseColumn(attribute: TransactionType): String {
        return when (attribute) {
            TransactionType.Expense -> "expense"
            TransactionType.Income -> "income"
            TransactionType.Transfer -> "transfer"
            TransactionType.Undefined -> "undefined"
        }
    }

    override fun convertToEntityAttribute(attribute: String): TransactionType {
        return when (attribute.trim().lowercase()) {
            "expense" -> TransactionType.Expense
            "income" ->  TransactionType.Income
            "transfer" -> TransactionType.Transfer
            "undefined" -> TransactionType.Undefined
            else -> throw RuntimeException("Unknown attribute: $attribute")
        }
    }
}
