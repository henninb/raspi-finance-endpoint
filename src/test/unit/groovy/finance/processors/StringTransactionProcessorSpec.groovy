package finance.processors

import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import finance.utils.Constants
import org.apache.camel.Exchange
import spock.lang.Ignore

@SuppressWarnings("GroovyAccessibility")
class StringTransactionProcessorSpec extends BaseProcessor {

    void 'test - StringTransactionProcessor valid payload'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        stringTransactionProcessor.process(mockExchange)

        then:
        1 * mockExchange.in >> mockMessage
        1 * mockMessage.setBody(mapper.writeValueAsString(transaction))
        1 * mockMessage.getBody(Transaction) >> transaction
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_STRING_PROCESSOR_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        1 * _
    }

    @Ignore
    void 'test - StringTransactionProcessor invalid object'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        stringTransactionProcessor.process(mockExchange)

        then:
       // thrown(NullPointerException)
        1 * mockExchange.in >> mockMessage
        1 * mockMessage.setBody(mapper.writeValueAsString(transaction))
        1 * mockMessage.getBody(Transaction) >> null
        //1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_STRING_PROCESSOR_COUNTER, transaction.accountNameOwner)) >> counter
        //1 * counter.increment()
        1 * _
    }
}
