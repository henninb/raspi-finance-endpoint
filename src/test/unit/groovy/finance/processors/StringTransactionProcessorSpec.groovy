package finance.processors

import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import finance.utils.Constants

@SuppressWarnings("GroovyAccessibility")
class StringTransactionProcessorSpec extends BaseProcessor {

    void 'test - StringTransactionProcessor valid payload'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        exchange.in.setBody(transaction)

        when:
        stringTransactionProcessor.process(exchange)

        then:
        1 * meterRegistryMock.counter(setMeterId(Constants.CAMEL_STRING_PROCESSOR_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test - StringTransactionProcessor invalid object'() {
        given:
        exchange.in.setBody("{}")

        when:
        stringTransactionProcessor.process(exchange)

        then:
        thrown(RuntimeException)
        0 * _
    }
}
