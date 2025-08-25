package finance.utils

import finance.domain.NetworkStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class NetworkStatusConverter : AttributeConverter<NetworkStatus, String> {

    override fun convertToDatabaseColumn(attribute: NetworkStatus): String {
        return attribute.label
    }

    override fun convertToEntityAttribute(attribute: String): NetworkStatus {
        return when (attribute.trim().lowercase()) {
            "in_network" -> NetworkStatus.InNetwork
            "out_of_network" -> NetworkStatus.OutOfNetwork
            "unknown" -> NetworkStatus.Unknown
            else -> throw RuntimeException("Unknown network status attribute: $attribute")
        }
    }
}