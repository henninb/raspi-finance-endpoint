package finance.routes

import finance.configurations.CamelProperties
import finance.helpers.TransactionBuilder
import finance.processors.ExceptionProcessor
import finance.processors.JsonTransactionProcessor
import org.apache.camel.Exchange
import org.apache.camel.ProducerTemplate
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.ModelCamelContext
import spock.lang.Ignore
import spock.lang.Specification

class JsonFileReaderRouteBuilderSpec extends Specification {
    protected ModelCamelContext camelContext
    protected JsonTransactionProcessor mockJsonTransactionProcessor = GroovyMock(JsonTransactionProcessor)
    protected ExceptionProcessor mockExceptionProcessor = GroovyMock(ExceptionProcessor)
    protected String payload = TransactionBuilder.builder().build()

    protected CamelProperties camelProperties = new CamelProperties(
            "true",
            "jsonFileReaderRoute",
            "direct:routeFromLocal",
            "fileWriterRoute",
            "direct:routeFromLocal",
            "transactionToDatabaseRoute",
            "mock:toTransactionToDatabaseRoute",
            "mock:toSavedFileEndpoint",
            "mock:toFailedJsonFileEndpoint",
            "mock:toFailedJsonParserEndpoint")

    void setup() {
        camelContext = new DefaultCamelContext()
        JsonFileReaderRouteBuilder router = new JsonFileReaderRouteBuilder(camelProperties, mockJsonTransactionProcessor, mockExceptionProcessor)
        camelContext.addRoutes(router)
        camelContext.start()
    }

    void cleanup() {
        camelContext.stop()
    }

    void 'test -- with invalid file name'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.failedJsonFileEndpoint)
        mockTestOutputEndpoint.expectedCount = 1
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')

        when:
        producer.sendBodyAndHeader(payload, Exchange.FILE_NAME, 'foo_brian.bad')

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 1
        mockTestOutputEndpoint.assertIsSatisfied()
        0 * _
    }

    void 'test -- valid payload and valid fileName'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.transactionToDatabaseRoute)
        mockTestOutputEndpoint.expectedCount = 1
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')

        when:
        producer.sendBodyAndHeader(payload, Exchange.FILE_NAME, 'foo_brian.json')

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 1
        mockTestOutputEndpoint.assertIsSatisfied()
        1 * mockJsonTransactionProcessor.process(_ as Exchange)
        0 * _
    }

    void 'test -- invalid json payload and valid fileName'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.failedJsonParserEndpoint)
        mockTestOutputEndpoint.expectedCount = 0
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')

        when:
        producer.sendBodyAndHeader('invalidJsonPayload', Exchange.FILE_NAME, 'foo_brian.json')

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 0
        mockTestOutputEndpoint.assertIsSatisfied()
        1 * mockJsonTransactionProcessor.process(_ as Exchange)
        0 * _
    }

    void 'test -- wrong json payload and valid fileName'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.transactionToDatabaseRoute)
        mockTestOutputEndpoint.expectedCount = 1
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')
        String myPayload = '[{"test":1}]'

        when:
        producer.sendBodyAndHeader(myPayload, Exchange.FILE_NAME, 'foo_brian.json')

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 1
        mockTestOutputEndpoint.assertIsSatisfied()
        1 * mockJsonTransactionProcessor.process(_ as Exchange)
        0 * _
    }
}
