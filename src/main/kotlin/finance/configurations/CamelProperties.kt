package finance.configurations

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

//@ConditionalOnProperty(name = ["camel.enabled"], havingValue = "true", matchIfMissing = true)
@Configuration
@ConfigurationProperties(prefix = "custom.project.camel-route", ignoreUnknownFields = false)
open class CamelProperties(
    var autoStartRoute: String = "",
    var jsonFileReaderRouteId: String = "",
    var jsonFileReaderRoute: String = "",
    var jsonFileWriterRouteId: String = "",
    var jsonFileWriterRoute: String = "",
    var transactionToDatabaseRouteId: String = "",
    var transactionToDatabaseRoute: String = "",
    var savedFileEndpoint: String = "",
    var failedJsonFileEndpoint: String = "",
    var failedJsonParserEndpoint: String = ""
) {
    constructor() : this(savedFileEndpoint = "")
}