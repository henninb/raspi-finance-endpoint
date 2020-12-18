package finance.utils

import finance.domain.ReoccurringType
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class ReoccurringTypeConverter : AttributeConverter<ReoccurringType, String> {

    override fun convertToDatabaseColumn(attribute: ReoccurringType): String {
        return when (attribute) {
            ReoccurringType.Annually -> "annually"
            ReoccurringType.BiAnnually -> "bi-annually"
            ReoccurringType.FortNightly -> "fortnightly"
            ReoccurringType.Quarterly -> "quarterly"
            ReoccurringType.Monthly -> "monthly"
            ReoccurringType.Undefined -> "undefined"
        }
    }

    override fun convertToEntityAttribute(attribute: String): ReoccurringType {
        return when (attribute.trim().toLowerCase()) {
            "annually" -> ReoccurringType.Annually
            "bi-annually" -> ReoccurringType.BiAnnually
            "fortnightly" -> ReoccurringType.FortNightly
            "quarterly" -> ReoccurringType.Quarterly
            "monthly" -> ReoccurringType.Monthly
            "undefined" -> ReoccurringType.Undefined
            else -> throw RuntimeException("Unknown attribute: $attribute")
        }
    }
}
