package finance.routes

import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import finance.utils.Constants
import org.apache.camel.CamelExecutionException
import org.apache.camel.ProducerTemplate
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext
import jakarta.validation.ConstraintViolation

@SuppressWarnings("GroovyAccessibility")
class TransactionToDatabaseRouteBuilderSpec extends BaseRouteBuilderSpec {

    void setup() {
        stringTransactionProcessor.validator = validatorMock
        stringTransactionProcessor.meterService = meterService
        insertTransactionProcessor.validator = validatorMock
        insertTransactionProcessor.meterService = meterService
        camelProperties.transactionToDatabaseRoute = 'direct:routeFromLocal'
        camelProperties.jsonFileWriterRoute = 'mock:toEnd'
        camelContext = new DefaultCamelContext()
        TransactionToDatabaseRouteBuilder router = new TransactionToDatabaseRouteBuilder(camelProperties, stringTransactionProcessor, insertTransactionProcessor, exceptionProcessorMock)
        camelContext.addRoutes(router)
        camelContext.start()
    }

    void 'test -- valid payload - 1 messages'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.jsonFileWriterRoute)
        mockTestOutputEndpoint.expectedCount = 1
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri(camelProperties.transactionToDatabaseRoute)
        Transaction transaction = TransactionBuilder.builder().build()
        List<Transaction> transactions = [transaction]
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        producer.sendBody(transactions)

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 1
        mockTestOutputEndpoint.assertIsSatisfied()
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_STRING_PROCESSOR_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        1 * transactionServiceMock.insertTransaction(transaction) >> transaction
        0 * _
    }

    void 'test -- valid payload - 2 messages'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.jsonFileWriterRoute)
        mockTestOutputEndpoint.expectedCount = 2
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri(camelProperties.transactionToDatabaseRoute)
        Transaction transaction1 = TransactionBuilder.builder().withGuid(UUID.randomUUID().toString()).build()
        Transaction transaction2 = TransactionBuilder.builder().withGuid(UUID.randomUUID().toString()).build()
        List<Transaction> transactions = [transaction1, transaction2]
        when:
        producer.sendBody(transactions)

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 2
        mockTestOutputEndpoint.assertIsSatisfied()
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, transaction1.accountNameOwner)) >> counter
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, transaction2.accountNameOwner)) >> counter
        2 * counter.increment()
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_STRING_PROCESSOR_COUNTER, transaction1.accountNameOwner)) >> counter
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_STRING_PROCESSOR_COUNTER, transaction2.accountNameOwner)) >> counter
        2 * counter.increment()
        1 * transactionServiceMock.insertTransaction(transaction1) >> transaction1
        1 * transactionServiceMock.insertTransaction(transaction2) >> transaction2
        0 * _
    }

    void 'test -- invalid payload - 2 messages'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.jsonFileWriterRoute)
        mockTestOutputEndpoint.expectedCount = 0
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri(camelProperties.transactionToDatabaseRoute)
        List<String> transactions = ['junk1', 'junk2']

        when:
        producer.sendBody(transactions)

        then:
        thrown(CamelExecutionException)
        mockTestOutputEndpoint.receivedExchanges.size() == 0
        mockTestOutputEndpoint.assertIsSatisfied()
        2 * exceptionProcessorMock.process(_)
        0 * _
    }
}
