package finance.utils

import finance.domain.TransactionType
import jakarta.persistence.Converter

@Converter
class TransactionTypeConverter : LabeledEnumConverter<TransactionType>(TransactionType::class.java)
