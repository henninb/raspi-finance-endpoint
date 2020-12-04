package finance.processors

import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import finance.services.MeterService
import org.apache.camel.Exchange
import org.apache.camel.Message
import spock.lang.Specification

class StringTransactionProcessorSpec extends Specification {
    Exchange mockExchange = GroovyMock(Exchange)
    Message mockMessage = GroovyMock(Message)
    MeterService mockMeterService = GroovyMock(MeterService)
    StringTransactionProcessor processor = new StringTransactionProcessor(mockMeterService)

    def "test -- StringTransactionProcessor"() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        processor.process(mockExchange)

        then:
        1 * mockExchange.in >> mockMessage
        1 * mockMessage.body(Transaction) >> transaction
        1 * mockMessage.body(transaction.toString())
        1 * _
    }
}
