package finance.routes

import finance.domain.Transaction
import finance.helpers.CategoryBuilder
import finance.helpers.TransactionBuilder
import finance.utils.Constants
import org.apache.camel.CamelExecutionException
import org.apache.camel.ProducerTemplate
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext

import javax.validation.ConstraintViolation

@SuppressWarnings("GroovyAccessibility")
class TransactionToDatabaseRouteBuilderSpec extends BaseRouteBuilderSpec {

    void setup() {
        camelProperties.transactionToDatabaseRoute = 'direct:routeFromLocal'
        camelProperties.jsonFileWriterRoute = 'mock:toEnd'
        camelContext = new DefaultCamelContext()
        TransactionToDatabaseRouteBuilder router = new TransactionToDatabaseRouteBuilder(camelProperties, stringTransactionProcessorMock, insertTransactionProcessorMock, mockExceptionProcessor)
        camelContext.addRoutes(router)
        camelContext.start()
    }

    void 'test -- valid payload - 1 messages'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.jsonFileWriterRoute)
        mockTestOutputEndpoint.expectedCount = 1
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri(camelProperties.transactionToDatabaseRoute)
        //ConsumerTemplate consumer = camelContext.createConsumerTemplate()
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
        1 * validatorMock.validate(transaction) >> constraintViolations
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_STRING_PROCESSOR_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        1 * categoryRepositoryMock.findByCategory(transaction.category) >> Optional.of(CategoryBuilder.builder().build())
        1 * transactionRepositoryMock.saveAndFlush(transaction)
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test -- valid payload - 2 messages'() {
        given:
        MockEndpoint mockTestOutputEndpoint = MockEndpoint.resolve(camelContext, camelProperties.jsonFileWriterRoute)
        mockTestOutputEndpoint.expectedCount = 2
        ProducerTemplate producer = camelContext.createProducerTemplate()
        producer.setDefaultEndpointUri(camelProperties.transactionToDatabaseRoute)
        //ConsumerTemplate consumer = camelContext.createConsumerTemplate()
        Transaction transaction1 = TransactionBuilder.builder().withGuid(UUID.randomUUID().toString()).build()
        Transaction transaction2 = TransactionBuilder.builder().withGuid(UUID.randomUUID().toString()).build()
        List<Transaction> transactions = [transaction1, transaction2]
        Set<ConstraintViolation<Transaction>> constraintViolations1 = validator.validate(transaction1)
        Set<ConstraintViolation<Transaction>> constraintViolations2 = validator.validate(transaction2)
        when:
        producer.sendBody(transactions)

        then:
        mockTestOutputEndpoint.receivedExchanges.size() == 2
        mockTestOutputEndpoint.assertIsSatisfied()
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, transaction1.accountNameOwner)) >> counter
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, transaction2.accountNameOwner)) >> counter
        2 * counter.increment()
        1 * validatorMock.validate(transaction1) >> constraintViolations1
        1 * validatorMock.validate(transaction2) >> constraintViolations2
        1 * transactionRepositoryMock.findByGuid(transaction1.guid) >> Optional.of(transaction1)
        1 * transactionRepositoryMock.findByGuid(transaction2.guid) >> Optional.of(transaction2)
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_STRING_PROCESSOR_COUNTER, transaction1.accountNameOwner)) >> counter
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_STRING_PROCESSOR_COUNTER, transaction2.accountNameOwner)) >> counter
        2 * counter.increment()
        1 * categoryRepositoryMock.findByCategory(transaction1.category) >> Optional.of(CategoryBuilder.builder().build())
        1 * categoryRepositoryMock.findByCategory(transaction2.category) >> Optional.of(CategoryBuilder.builder().build())
        1 * transactionRepositoryMock.saveAndFlush(transaction1)
        1 * transactionRepositoryMock.saveAndFlush(transaction2)
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER, transaction1.accountNameOwner)) >> counter
        1 * meterRegistryMock.counter(setMeterId(Constants.TRANSACTION_ALREADY_EXISTS_COUNTER, transaction2.accountNameOwner)) >> counter
        2 * counter.increment()
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
        0 * _
    }
}
