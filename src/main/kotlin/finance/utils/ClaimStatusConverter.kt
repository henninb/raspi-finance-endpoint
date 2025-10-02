package finance.utils

import finance.domain.ClaimStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class ClaimStatusConverter : AttributeConverter<ClaimStatus, String> {
    override fun convertToDatabaseColumn(attribute: ClaimStatus): String = attribute.label

    override fun convertToEntityAttribute(dbData: String): ClaimStatus =
        when (dbData.trim().lowercase()) {
            "submitted" -> ClaimStatus.Submitted
            "processing" -> ClaimStatus.Processing
            "approved" -> ClaimStatus.Approved
            "denied" -> ClaimStatus.Denied
            "paid" -> ClaimStatus.Paid
            "closed" -> ClaimStatus.Closed
            else -> throw RuntimeException("Unknown claim status attribute: $dbData")
        }
}
