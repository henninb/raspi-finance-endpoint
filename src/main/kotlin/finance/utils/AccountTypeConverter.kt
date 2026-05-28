package finance.utils

import finance.domain.AccountType
import jakarta.persistence.Converter

@Converter
class AccountTypeConverter : LabeledEnumConverter<AccountType>(AccountType::class.java)
