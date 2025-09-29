package finance.configurations

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "custom.project", ignoreUnknownFields = true)
@Configuration
open class CustomProperties(
    var excludedAccounts: MutableList<String> = mutableListOf(),
)
