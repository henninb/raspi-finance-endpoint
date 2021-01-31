package finance.routes

import finance.configurations.CamelProperties
import finance.domain.Transaction
import finance.processors.ExceptionProcessor
import finance.processors.InsertTransactionProcessor
import finance.processors.StringTransactionProcessor
import org.apache.camel.InvalidPayloadException
import org.apache.camel.LoggingLevel
import org.apache.camel.builder.RouteBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@ConditionalOnProperty(name = ["camel.enabled"], havingValue = "true", matchIfMissing = true)
@Component
class TransactionToDatabaseRouteBuilder (
    private var camelProperties: CamelProperties,
    private var stringTransactionProcessor: StringTransactionProcessor,
    private var insertTransactionProcessor: InsertTransactionProcessor,
    private var exceptionProcessor: ExceptionProcessor
) : RouteBuilder() {


    @Throws(Exception::class)
    override fun configure() {

        onException(InvalidPayloadException::class.java)
            .log(LoggingLevel.INFO, "invalid payload :: \${exception.message}")
            .process(exceptionProcessor)
            .end()

        from(camelProperties.transactionToDatabaseRoute)
            .autoStartup(camelProperties.autoStartRoute)
            //.routeId(camelProperties.transactionToDatabaseRouteId)
            .routeId(TransactionToDatabaseRouteBuilder::class.java.simpleName.toString().replace("Builder", ""))
            .split(body())
            .log(LoggingLevel.INFO, "split body completed.")
            .convertBodyTo(Transaction::class.java)
            .log(LoggingLevel.INFO, "converted body to string.")
            .process(stringTransactionProcessor)
            .convertBodyTo(String::class.java)

            .process(insertTransactionProcessor)
            .to(camelProperties.jsonFileWriterRoute)
            .log(LoggingLevel.INFO, "message was processed by insertTransactionProcessor.")
            .end()
    }
}