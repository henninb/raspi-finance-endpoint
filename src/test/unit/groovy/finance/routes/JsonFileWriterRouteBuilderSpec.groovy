package finance.routes

import finance.configurations.CamelProperties
import finance.processors.ExceptionProcessor
import org.apache.camel.builder.AdviceWithRouteBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.model.ModelCamelContext
import org.apache.camel.model.RouteDefinition
import org.apache.camel.reifier.RouteReifier
import spock.lang.Specification

class JsonFileWriterRouteBuilderSpec extends Specification {

    ModelCamelContext camelContext
    ExceptionProcessor mockExceptionProcessor = Mock(ExceptionProcessor)

    CamelProperties camelProperties = new CamelProperties(
            "true",
            "n/a",
            "n/a",
            "fileWriterRoute",
            "direct:routeFromLocal",
            "transactionToDatabaseRoute",
            "direct:routeFromLocal",
            "mock:toSavedFileEndpoint",
            "mock:toFailedJsonFileEndpoint",
            "mock:toFailedJsonParserEndpoint")

    def setup() {
        camelContext = new DefaultCamelContext()
        def router = new JsonFileWriterRouteBuilder(camelProperties, mockExceptionProcessor)
        camelContext.addRoutes(router)

        camelContext.start()

        ModelCamelContext mcc = camelContext.adapt(ModelCamelContext.class)

        camelContext.routeDefinitions.toList().each { RouteDefinition routeDefinition ->
            RouteReifier.adviceWith(mcc.getRouteDefinition(camelProperties.jsonFileWriterRouteId), mcc, new AdviceWithRouteBuilder() {
                @Override
                void configure() throws Exception {
                }
            })
        }
    }

    def cleanup() {
        camelContext.stop()
    }


    def 'test -- valid payload - 1 messages'() {
        given:
        def mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.savedFileEndpoint)
        mockTestOutputEndpoint.expectedCount = 1
        def producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')

        when:
        producer.sendBodyAndHeader('theData', 'guid','123')

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 1
        mockTestOutputEndpoint.assertIsSatisfied()
        0 * _
    }

    def 'test -- invalid payload - 1 messages'() {
        given:
        def mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.savedFileEndpoint)
        mockTestOutputEndpoint.expectedCount = 0
        def producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri('direct:routeFromLocal')

        when:
        producer.sendBody('theDataWithoutHeader')

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 0
        1 * mockExceptionProcessor.process(_)
        mockTestOutputEndpoint.assertIsSatisfied()
        0 * _
    }

}
