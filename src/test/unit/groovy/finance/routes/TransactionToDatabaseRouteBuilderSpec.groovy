package finance.routes

import finance.configurations.CamelProperties
import finance.helpers.TransactionBuilder
import finance.processors.ExceptionProcessor
import finance.processors.InsertTransactionProcessor
import finance.processors.JsonTransactionProcessor
import finance.processors.StringTransactionProcessor
import org.apache.camel.EndpointInject
import org.apache.camel.Exchange
import org.apache.camel.builder.AdviceWithRouteBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.ModelCamelContext
import org.apache.camel.model.RouteDefinition
import org.apache.camel.reifier.RouteReifier
import spock.lang.Ignore
import spock.lang.Specification

class TransactionToDatabaseRouteBuilderSpec extends Specification {
    ModelCamelContext camelContext
    StringTransactionProcessor stringTransactionProcessorMock = Mock(StringTransactionProcessor)
    InsertTransactionProcessor insertTransactionProcessorMock = Mock(InsertTransactionProcessor)
    ExceptionProcessor mockExceptionProcessor = Mock(ExceptionProcessor)

    CamelProperties camelProperties = new CamelProperties(
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

    def setup() {
        camelContext = new DefaultCamelContext()
        def router = new TransactionToDatabaseRouteBuilder(camelProperties, stringTransactionProcessorMock, insertTransactionProcessorMock, mockExceptionProcessor)
        camelContext.addRoutes(router)

        camelContext.start()

        ModelCamelContext mcc = camelContext.adapt(ModelCamelContext.class)

        camelContext.routeDefinitions.toList().each { RouteDefinition routeDefinition ->
            RouteReifier.adviceWith(mcc.getRouteDefinition(camelProperties.transactionToDatabaseRouteId), mcc, new AdviceWithRouteBuilder() {
                @Override
                void configure() throws Exception {
                }
            })
        }
    }

    def cleanup() {
        camelContext.stop()
    }

    def 'test -- valid payload - 2 messages'() {
        given:
        def mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.jsonFileWriterRoute)
        mockTestOutputEndpoint.expectedCount = 2
        def producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')
        def transactions = [TransactionBuilder.builder().build(), TransactionBuilder.builder().build()]

        when:
        producer.sendBody(transactions)

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 2
        mockTestOutputEndpoint.assertIsSatisfied()
        2 * stringTransactionProcessorMock.process(_)
        2 * insertTransactionProcessorMock.process(_)
        0 * _
    }

    def 'test -- invalid payload - 2 messages'() {
        given:
        def mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.jsonFileWriterRoute)
        mockTestOutputEndpoint.expectedCount = 0
        def producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')
        def transactions = ['junk1', 'junk2']

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
