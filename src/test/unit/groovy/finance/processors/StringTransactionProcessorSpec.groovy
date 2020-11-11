package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import org.apache.camel.Exchange
import org.apache.camel.Message
import spock.lang.Specification

class StringTransactionProcessorSpec extends Specification {
    Exchange mockExchange = GroovyMock(Exchange)
    Message mockMessage = GroovyMock(Message)

    def "test -- StringTransactionProcessor"() {
        given:
        StringTransactionProcessor processor = new StringTransactionProcessor()
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        processor.process(mockExchange)

        then:
        1 * mockExchange.getIn() >> mockMessage
        1 * mockMessage.getBody(Transaction.class) >> transaction
        1 * mockMessage.setBody(transaction.toString())
        1 * _
    }
}