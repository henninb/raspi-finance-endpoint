package finance.utils

import finance.domain.ReoccurringType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class ReoccurringTypeConverter : AttributeConverter<ReoccurringType, String> {

    override fun convertToDatabaseColumn(attribute: ReoccurringType): String {
        return when (attribute) {
            ReoccurringType.Annually -> "annually"
            ReoccurringType.BiAnnually -> "biannually"
            ReoccurringType.FortNightly -> "fortnightly"
            ReoccurringType.Quarterly -> "quarterly"
            ReoccurringType.Monthly -> "monthly"
            ReoccurringType.Onetime -> "onetime"
            ReoccurringType.Undefined -> "undefined"
        }
    }

    override fun convertToEntityAttribute(attribute: String): ReoccurringType {
        return when (attribute.trim().lowercase()) {
            "annually" -> ReoccurringType.Annually
            "biannually" -> ReoccurringType.BiAnnually
            "fortnightly" -> ReoccurringType.FortNightly
            "quarterly" -> ReoccurringType.Quarterly
            "monthly" -> ReoccurringType.Monthly
            "onetime" -> ReoccurringType.Onetime
            "undefined" -> ReoccurringType.Undefined
            else -> throw RuntimeException("Unknown attribute: $attribute")
        }
    }
}
