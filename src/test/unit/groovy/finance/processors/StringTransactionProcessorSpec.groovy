package finance.processors

import finance.domain.Transaction
import finance.helpers.TransactionBuilder

@SuppressWarnings("GroovyAccessibility")
class StringTransactionProcessorSpec extends BaseProcessor {

    void 'test -- StringTransactionProcessor'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        stringTransactionProcessor.process(mockExchange)

        then:
        1 * mockExchange.in >> mockMessage
        //TODO: 12/4/2020 - check the details
        1 * mockMessage.setBody(_)
        //1 * mockMessage.setBody(transaction)
        1 * mockMessage.getBody(Transaction) >> transaction
        1 * meterRegistryMock.counter(_) >> counter
        1 * counter.increment()
        1 * _
    }
}
