package finance.configurations

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "custom.project.camel-route", ignoreUnknownFields = false)
open class CamelProperties(
    var autoStartRoute: String = "",
    var jsonFileReaderRoute: String = "",
    var jsonFileWriterRoute: String = "",
    var transactionToDatabaseRoute: String = "",
    var savedFileEndpoint: String = "",
    var failedJsonFileEndpoint: String = "",
    var failedJsonParserEndpoint: String = ""
) {
    constructor() : this(savedFileEndpoint = "")
}