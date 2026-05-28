package finance.utils

import finance.domain.TransactionState
import jakarta.persistence.Converter

@Converter
class TransactionStateConverter : LabeledEnumConverter<TransactionState>(TransactionState::class.java)
