package finance.utils

import finance.pojos.AccountType
import javax.persistence.AttributeConverter
import javax.persistence.Converter

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
        return  when (attribute.trim()) {
            "credit" -> AccountType.Credit
            "debit" -> AccountType.Debit
            "unknown" -> AccountType.Undefined
            //else -> throw IllegalArgumentException("Unknown attribute: $attribute")
            else -> throw RuntimeException("Unknown attribute: $attribute")
        }
    }
}
