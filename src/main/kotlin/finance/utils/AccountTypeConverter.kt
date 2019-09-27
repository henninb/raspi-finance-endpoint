package finance.utils

import finance.pojos.AccountType
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class AccountTypeConverter : AttributeConverter<AccountType, String> {

    override fun convertToDatabaseColumn(attribute: AccountType): String {
        when (attribute) {
            AccountType.Credit -> return "credit"
            AccountType.Debit -> return "debit"
            AccountType.Undefined -> return "undefined"
            else -> throw IllegalArgumentException("Unknown: $attribute")
        }
    }

    override fun convertToEntityAttribute(dbData: String): AccountType {
        when (dbData) {
            "credit" -> return AccountType.Credit
            "debit" -> return AccountType.Debit
            "unknown" -> return AccountType.Undefined
            else -> throw IllegalArgumentException("Unknown $dbData")
        }
    }
}
