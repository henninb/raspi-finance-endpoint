package finance.routes


import org.apache.camel.ProducerTemplate
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext

class JsonFileWriterRouteBuilderSpec extends BaseRouteBuilderSpec {

//    protected CamelProperties camelProperties = new CamelProperties(
//            "true",
//            "n/a",
//            "n/a",
//            "fileWriterRoute",
//            "direct:routeFromLocal",
//            "transactionToDatabaseRoute",
//            "direct:routeFromLocal",
//            "mock:toSavedFileEndpoint",
//            "mock:toFailedJsonFileEndpoint",
//            "mock:toFailedJsonParserEndpoint")

    void setup() {
        camelProperties.jsonFileWriterRoute = 'direct:routeFromLocal'
        camelContext = new DefaultCamelContext()
        JsonFileWriterRouteBuilder router = new JsonFileWriterRouteBuilder(camelProperties, mockExceptionProcessor)
        camelContext.addRoutes(router)
        camelContext.start()
    }

    void 'test -- valid payload - 1 messages'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.savedFileEndpoint)
        mockTestOutputEndpoint.expectedCount = 1
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')

        when:
        producer.sendBodyAndHeader('theData', 'guid', '123')

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 1
        mockTestOutputEndpoint.assertIsSatisfied()
        0 * _
    }

    void 'test -- invalid payload - 1 messages'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.savedFileEndpoint)
        mockTestOutputEndpoint.expectedCount = 0
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')

        when:
        producer.sendBody('theDataWithoutHeader')

        then:
        thrown(RuntimeException)
        mockTestOutputEndpoint.receivedExchanges.size() == 0
        0 * _
    }
}
