package finance.utils

import finance.domain.ClaimStatus
import jakarta.persistence.Converter

@Converter
class ClaimStatusConverter : LabeledEnumConverter<ClaimStatus>(ClaimStatus::class.java)
