package finance.processors

import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import finance.services.MeterService
import org.apache.camel.Exchange
import org.apache.camel.Message
import spock.lang.IgnoreRest
import spock.lang.Specification

class StringTransactionProcessorSpec extends Specification {
    protected Exchange mockExchange = GroovyMock(Exchange)
    protected Message mockMessage = GroovyMock(Message)
    protected MeterService mockMeterService = GroovyMock(MeterService)
    protected StringTransactionProcessor processor = new StringTransactionProcessor(mockMeterService)

    @IgnoreRest
    void 'test -- StringTransactionProcessor'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        processor.process(mockExchange)

        then:
        1 * mockExchange.in >> mockMessage
        //TODO: 12/4/2020 - check the details
        1 * mockMessage.setBody(_)
        //1 * mockMessage.setBody(transaction)
        1 * mockMessage.getBody(Transaction) >> transaction
        1 * _
    }
}
