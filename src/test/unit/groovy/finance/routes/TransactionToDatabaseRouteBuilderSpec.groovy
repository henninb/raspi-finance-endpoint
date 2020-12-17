package finance.routes

import finance.configurations.CamelProperties
import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import finance.processors.ExceptionProcessor
import finance.processors.InsertTransactionProcessor
import finance.processors.StringTransactionProcessor
import org.apache.camel.ProducerTemplate
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.ModelCamelContext
import spock.lang.Specification

class TransactionToDatabaseRouteBuilderSpec extends Specification {
    protected ModelCamelContext camelContext
    protected StringTransactionProcessor stringTransactionProcessorMock = GroovyMock(StringTransactionProcessor)
    protected InsertTransactionProcessor insertTransactionProcessorMock = GroovyMock(InsertTransactionProcessor)
    protected ExceptionProcessor mockExceptionProcessor = GroovyMock(ExceptionProcessor)

    protected CamelProperties camelProperties = new CamelProperties(
            "true",
            "n/a",
            "n/a",
            "fileWriterRoute",
            "mock:toEnd",
            "transactionToDatabaseRoute",
            "direct:routeFromLocal",
            "mock:toSavedFileEndpoint",
            "mock:toFailedJsonFileEndpoint",
            "mock:toFailedJsonParserEndpoint")

    void setup() {
        camelContext = new DefaultCamelContext()
        TransactionToDatabaseRouteBuilder router = new TransactionToDatabaseRouteBuilder(camelProperties, stringTransactionProcessorMock, insertTransactionProcessorMock, mockExceptionProcessor)
        camelContext.addRoutes(router)

        camelContext.start()
    }

    void cleanup() {
        camelContext.stop()
    }

    void 'test -- valid payload - 2 messages'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.jsonFileWriterRoute)
        mockTestOutputEndpoint.expectedCount = 2
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')
        List<Transaction> transactions = [TransactionBuilder.builder().build(), TransactionBuilder.builder().build()]

        when:
        producer.sendBody(transactions)

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 2
        mockTestOutputEndpoint.assertIsSatisfied()
        2 * stringTransactionProcessorMock.process(_)
        2 * insertTransactionProcessorMock.process(_)
        0 * _
    }

    void 'test -- invalid payload - 2 messages'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.jsonFileWriterRoute)
        mockTestOutputEndpoint.expectedCount = 0
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')
        List<String> transactions = ['junk1', 'junk2']

        when:
        producer.sendBody(transactions)

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 0
        mockTestOutputEndpoint.assertIsSatisfied()
        0 * stringTransactionProcessorMock.process(_)
        0 * insertTransactionProcessorMock.process(_)
        2 * mockExceptionProcessor.process(_)
        0 * _
    }
}
