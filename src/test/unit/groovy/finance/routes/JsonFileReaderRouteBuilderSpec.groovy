package finance.routes

import finance.configurations.CamelProperties
import finance.processors.ExceptionProcessor
import finance.processors.JsonTransactionProcessor
import org.apache.camel.Exchange
import org.apache.camel.ProducerTemplate
import org.apache.camel.builder.AdviceWithRouteBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.ModelCamelContext
import org.apache.camel.model.RouteDefinition
import org.apache.camel.reifier.RouteReifier
import spock.lang.Ignore
import spock.lang.Specification

class JsonFileReaderRouteBuilderSpec extends Specification {
    protected ModelCamelContext camelContext
    protected JsonTransactionProcessor mockJsonTransactionProcessor = GroovyMock(JsonTransactionProcessor)
    protected ExceptionProcessor mockExceptionProcessor = GroovyMock(ExceptionProcessor)

    protected String payload = '''
    [
    {"guid":"aa08f2bb-29a6-4f71-b866-ff8f625e1b04","accountNameOwner":"foo_brian",
    "description":"Bullseye cafe","category":"restaurant","amount":4.42,"cleared":1,
    "reoccurring":false,"notes":"","sha256":"","transactionId":0,"accountId":0,
    "accountType":"credit",
    "transactionDate":1337058000000,"dateUpdated":1487301459000,"dateAdded":1487301459000},
    {"guid":"bb08f2bb-29a6-4f71-b866-ff8f625e1b04","accountNameOwner":"foo_brian",
    "description":"Bullseye cafe","category":"restaurant","amount":4.42,"cleared":1,
    "reoccurring":false,"notes":"","sha256":"","transactionId":0,"accountId":0,
    "accountType":"credit",
    "transactionDate":1337058000000,"dateUpdated":1487301459000,"dateAdded":1487301459000}
    ]
    '''

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

        ModelCamelContext mcc = camelContext.adapt(ModelCamelContext)

        camelContext.routeDefinitions.toList().each { RouteDefinition routeDefinition ->
            RouteReifier.adviceWith(mcc.getRouteDefinition(camelProperties.jsonFileReaderRouteId), mcc, new AdviceWithRouteBuilder() {
                @Override
                void configure() throws Exception {
                    mockEndpointsAndSkip('direct://toTransactionToDatabaseRoute')
                }
            })
        }
    }

    void cleanup() {
        camelContext.stop()
    }

    String invalidJsonPayload = '''
    [
    {"guid":"aa08f2bb-29a6-4f71-b866-ff8f625e1b04","accountNameOwner":"foo_brian",
    "description":"Bullseye cafe","category":"restaurant",
    "amount":4.42,"cleared":1,
    "reoccurring":false,"notes":"",
    "sha256":"","transactionId":0,
    "accountId":0,
    "accountType":"credit",
    "transactionDate":1337058000000,"dateUpdated":1487301459000,"dateAdded":1487301459000},
    {"guid":"bb08f2bb-29a6-4f71-b866-ff8f625e1b04",
    "accountNameOwner":"foo_brian",
    "description":"Bullseye cafe",
    "category":"restaurant","amount":4.42,"cleared":1,
    "reoccurring":false,"notes":"","sha256":"","transactionId":0,"accountId":0,
    "accountType":"creditNotValid",
    "transactionDate":1337058000000,
    "dateUpdated":1487301459000,
    "dateAdded":1487301459000},
    {"guid":"cc08f2bb-29a6-4f71-b866-ff8f625e1b04","accountNameOwner":"foo_brian",
    "description":"Bullseye cafe","category":"restaurant","amount":4.42,"cleared":1,
    "reoccurring":false,"notes":"","sha256":"","transactionId":0,"accountId":0,
    "accountType":"credit",
    "transactionDate":1337058000000,"dateUpdated":1487301459000,"dateAdded":1487301459000}
    ]
    '''

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
        1 * mockJsonTransactionProcessor.process(_)
        0 * _
    }

    @Ignore
    //TODO: should be integration tests
    void 'test -- invalid field in payload and valid fileName'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.failedJsonParserEndpoint)
        mockTestOutputEndpoint.expectedCount = 1
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')

        when:
        producer.sendBodyAndHeader(invalidJsonPayload, Exchange.FILE_NAME, 'foo_brian.json')

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 1
        mockTestOutputEndpoint.assertIsSatisfied()
        1 * mockJsonTransactionProcessor.process(_) //>> {throw new JsonParseException()}
        0 * _
    }

    @Ignore
    //TODO: should be integration tests
    void 'test -- invalid json payload and valid fileName'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.failedJsonParserEndpoint)
        mockTestOutputEndpoint.expectedCount = 1
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')

        when:
        producer.sendBodyAndHeader('invalidJsonPayload', Exchange.FILE_NAME, 'foo_brian.json')

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 1
        mockTestOutputEndpoint.assertIsSatisfied()
        0 * _
    }

    @Ignore
    //TODO: should be integration tests
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
        0 * _
    }
}
