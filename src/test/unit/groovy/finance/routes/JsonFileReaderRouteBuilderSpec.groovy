package finance.routes


import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import org.apache.camel.Exchange
import org.apache.camel.ProducerTemplate
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext

import javax.validation.ConstraintViolation

class JsonFileReaderRouteBuilderSpec extends BaseRouteBuilderSpec {

    void setup() {
        jsonTransactionProcessor.meterService = meterService
        jsonTransactionProcessor.validator = validatorMock

        camelProperties.jsonFileReaderRoute = 'direct:routeFromLocal'
        camelContext = new DefaultCamelContext()
        JsonFileReaderRouteBuilder router = new JsonFileReaderRouteBuilder(camelProperties, jsonTransactionProcessor, exceptionProcessorMock)
        camelContext.addRoutes(router)
        camelContext.start()
    }

    void 'test - with invalid file name'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.failedJsonFileEndpoint)
        mockTestOutputEndpoint.expectedCount = 1
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri(camelProperties.jsonFileReaderRoute)

        when:
        producer.sendBodyAndHeader('payload', Exchange.FILE_NAME, 'foo_brian.bad')

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 1
        mockTestOutputEndpoint.assertIsSatisfied()
        0 * _
    }

    void 'test - valid payload and valid fileName'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        String payload = [transaction]
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.transactionToDatabaseRoute)
        mockTestOutputEndpoint.expectedCount = 1
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri(camelProperties.jsonFileReaderRoute)
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        producer.sendBodyAndHeader(payload, Exchange.FILE_NAME, 'foo_brian.json')

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 1
        mockTestOutputEndpoint.assertIsSatisfied()
        1 * validatorMock.validate(transaction) >> constraintViolations
        0 * _
    }

    void 'test -- invalid json payload and valid fileName'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.failedJsonParserEndpoint)
        mockTestOutputEndpoint.expectedCount = 1
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri(camelProperties.jsonFileReaderRoute)

        when:
        producer.sendBodyAndHeader('invalidJsonPayload', Exchange.FILE_NAME, 'foo_brian.json')

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 1
        mockTestOutputEndpoint.assertIsSatisfied()
        1 * exceptionProcessorMock.process(_)
        //1 * mockJsonTransactionProcessor.process(_ as Exchange)
        0 * _
    }

    void 'test -- wrong json payload and valid fileName'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.transactionToDatabaseRoute)
        mockTestOutputEndpoint.expectedCount = 1
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri(camelProperties.jsonFileReaderRoute)
        String myPayload = '[{"test":1}]'
        Transaction transaction = TransactionBuilder.builder().build()
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        producer.sendBodyAndHeader(myPayload, Exchange.FILE_NAME, 'foo_brian.json')

        then:
        //mockTestOutputEndpoint.receivedExchanges.size() == 1
        //mockTestOutputEndpoint.assertIsSatisfied()
        1 * validatorMock.validate(_) >> constraintViolations
        0 * _
    }
}
