package finance.utils

import finance.domain.AccountType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class AccountTypeConverter : AttributeConverter<AccountType, String> {

    override fun convertToDatabaseColumn(attribute: AccountType): String {
        return when (attribute) {
            AccountType.Credit -> "credit"
            AccountType.Debit -> "debit"
            AccountType.Undefined -> "undefined"
        }
    }

    override fun convertToEntityAttribute(attribute: String): AccountType {
        return when (attribute.trim().lowercase()) {
            "credit" -> AccountType.Credit
            "debit" -> AccountType.Debit
            "undefined" -> AccountType.Undefined
            else -> throw RuntimeException("Unknown attribute: $attribute")
        }
    }
}
