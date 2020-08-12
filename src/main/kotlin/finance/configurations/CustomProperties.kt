package finance.configurations

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "custom.project", ignoreUnknownFields = true)
@Configuration
open class CustomProperties(
        var creditAccounts: MutableList<String> = mutableListOf(),
        var excludedAccounts: MutableList<String> = mutableListOf(),
        var timeZone: String = "",
        var excelPassword: String = "",
        var excelInputFilePath: String = ""
)