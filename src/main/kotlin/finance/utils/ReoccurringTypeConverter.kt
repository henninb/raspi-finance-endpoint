package finance.utils

import finance.domain.ReoccurringType
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class ReoccurringTypeConverter : AttributeConverter<ReoccurringType, String> {

    override fun convertToDatabaseColumn(attribute: ReoccurringType): String {
        return when (attribute) {
            ReoccurringType.Annually -> "annually"
            ReoccurringType.BiAnnually -> "bi_annually"
            ReoccurringType.FortNightly -> "fort_nightly"
            ReoccurringType.Quarterly -> "quarterly"
            ReoccurringType.Monthly -> "monthly"
            ReoccurringType.Undefined -> "undefined"
        }
    }

    override fun convertToEntityAttribute(attribute: String): ReoccurringType {
        return when (attribute.trim().toLowerCase()) {
            "annually" -> ReoccurringType.Annually
            "bi_annually" -> ReoccurringType.BiAnnually
            "fort_nightly" -> ReoccurringType.FortNightly
            "quarterly" -> ReoccurringType.Quarterly
            "monthly" -> ReoccurringType.Monthly
            "undefined" -> ReoccurringType.Undefined
            else -> throw RuntimeException("Unknown attribute: $attribute")
        }
    }
}
