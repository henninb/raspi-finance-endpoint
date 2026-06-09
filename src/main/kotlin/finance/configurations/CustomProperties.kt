package finance.configurations

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "custom.project", ignoreUnknownFields = true)
@Configuration
open class CustomProperties(
    var excludedAccounts: MutableList<String> = mutableListOf(),
    private var _adminUsers: MutableList<String> = mutableListOf(),
    var allowed: AllowedConfig = AllowedConfig(),
) {
    var adminUsers: MutableList<String>
        get() = _adminUsers
        set(value) {
            _adminUsers = value.filter { it.isNotBlank() }.toMutableList()
        }

    data class AllowedConfig(
        var origins: MutableList<String> = mutableListOf(),
    )
}
