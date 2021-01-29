package finance.routes

import finance.configurations.CamelProperties
import finance.processors.ExceptionProcessor
import org.apache.camel.Exchange
import org.apache.camel.LoggingLevel
import org.apache.camel.builder.RouteBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@ConditionalOnProperty(name = ["camel.enabled"], havingValue = "true", matchIfMissing = true)
@Component
class JsonFileWriterRouteBuilder (
    private var camelProperties: CamelProperties, private var exceptionProcessor: ExceptionProcessor
) : RouteBuilder() {

    @Throws(Exception::class)
    override fun configure() {

        from(camelProperties.jsonFileWriterRoute)
            .autoStartup(camelProperties.autoStartRoute)
            .routeId(camelProperties.jsonFileWriterRouteId)
            .setHeader(Exchange.FILE_NAME, header("guid"))
            .choice()
            .`when`(header("CamelFileName").isNotNull)
            .log(LoggingLevel.INFO, "wrote processed data to file.")
            .to(camelProperties.savedFileEndpoint)
            .log(LoggingLevel.INFO, "message saved to file.")
            .otherwise()
            .throwException(RuntimeException("filename is not set."))
            .endChoice()
            .end()
    }
}