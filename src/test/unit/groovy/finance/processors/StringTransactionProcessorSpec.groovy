package finance.processors

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import org.apache.camel.Exchange
import org.apache.camel.Message
import spock.lang.Specification

class StringTransactionProcessorSpec extends Specification {
    Exchange mockExchange = Mock(Exchange)
    Message mockMessage = Mock(Message)
    ObjectMapper mapper = new ObjectMapper()

    void "test StringTransactionProcessor"() {
        given:
        StringTransactionProcessor processor = new StringTransactionProcessor()
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        processor.process(mockExchange)

        then:
        1 * mockExchange.getIn() >> mockMessage
        1 * mockMessage.getBody(Transaction.class) >> transaction
        1 * mockExchange.setProperty('guid', transaction.guid)
        //TODO: this should be 1
        //0 * mockMessage.setBody(transaction.toString())
        1 * _
    }
}