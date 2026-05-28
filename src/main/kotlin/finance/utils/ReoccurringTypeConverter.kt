package finance.utils

import finance.domain.ReoccurringType
import jakarta.persistence.Converter

@Converter
class ReoccurringTypeConverter : LabeledEnumConverter<ReoccurringType>(ReoccurringType::class.java)
