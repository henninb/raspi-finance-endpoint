package finance.utils

import finance.domain.NetworkStatus
import jakarta.persistence.Converter

@Converter
class NetworkStatusConverter : LabeledEnumConverter<NetworkStatus>(NetworkStatus::class.java)
