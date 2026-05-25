package finance.configurations

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "spring.datasource", ignoreUnknownFields = true)
@Configuration
open class DataSourceProperties(
    var url: String = "",
    var username: String = "",
    var password: String = ""
)
