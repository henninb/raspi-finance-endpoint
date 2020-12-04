package finance.processors

import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import finance.services.MeterService
import org.apache.camel.Exchange
import org.apache.camel.Message
import spock.lang.Specification

import javax.validation.Validator

class JsonTransactionProcessorSpec extends Specification {
    Validator mockValidator = GroovyMock(Validator)
    MeterService mockMeterService = GroovyMock(MeterService)
    JsonTransactionProcessor processor = new JsonTransactionProcessor(mockValidator, mockMeterService)
    Exchange mockExchange = GroovyMock(Exchange)
    Message mockMessage = GroovyMock(Message)

    String payload = "[{\"guid\":\"0a23fec3-18c8-4b89-a5af-68fab8db8620\",\"accountId\":0,\"accountType\":\"credit\",\"accountNameOwner\":\"amex_brian\",\"transactionDate\":1475647200000,\"description\":\"target.com\",\"category\":\"online\",\"amount\":33.08,\"cleared\":1,\"reoccurring\":false,\"notes\":\"\",\"dateUpdated\":1475588992000,\"dateAdded\":1475588992000,\"sha256\":\"\"}," +
            "{\"transactionId\":0,\"guid\":\"0a23fec3-18c8-4b89-a5af-68fab8db8620\",\"accountId\":0,\"accountType\":\"credit\",\"accountNameOwner\":\"amex_brian\",\"transactionDate\":1475647200000,\"description\":\"Cafe Roale\",\"category\":\"restaurant\",\"amount\":3.08,\"cleared\":1,\"reoccurring\":false,\"notes\":\"\",\"dateUpdated\":1475588992000,\"dateAdded\":1475588992000,\"sha256\":\"\"}]"

    String payloadInvalid = "[{\"guid\":\"0a23fec3-18c8-4b89-a5af-68fab8db8620\",\"accountId\":0,\"accountType\":\"credit\",\"accountNameOwner\":\"invalid.invalid\",\"transactionDate\":1475647200000,\"description\":\"target.com\",\"category\":\"online\",\"amount\":33.08,\"cleared\":1,\"reoccurring\":false,\"notes\":\"\",\"dateUpdated\":1475588992000,\"dateAdded\":1475588992000,\"sha256\":\"\"}," +
            "{\"transactionId\":0,\"guid\":\"0a23fec3-18c8-4b89-a5af-68fab8db8620\",\"accountId\":0,\"accountType\":\"credit\",\"accountNameOwner\":\"amex_brian\",\"transactionDate\":1475647200000,\"description\":\"Cafe Roale\",\"category\":\"restaurant\",\"amount\":3.08,\"cleared\":1,\"reoccurring\":false,\"notes\":\"\",\"dateUpdated\":1475588992000,\"dateAdded\":1475588992000,\"sha256\":\"\"}]"

    void 'test JsonTransactionProcessor - process - valid records'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        List<Transaction> transactions = [transaction]

        when:
        processor.process(mockExchange)

        then:
        1 * mockExchange.in >> mockMessage
        1 * mockMessage.body(String) >> transactions.toString()
        1 * mockValidator.validate(_) >> new HashSet()
        1 * mockMessage.setBody({
            it ->
                return it.size() == 1
        }
        )
        0 * _
    }

    void 'test JsonTransactionProcessor - process - invalid records'() {
        when:
        processor.process(mockExchange)

        then:
        1 * mockExchange.in >> mockMessage
        1 * mockMessage.body(String) >> payloadInvalid
        //TODO: fix this to reflect the exact message
        2 * mockValidator.validate(_) >> new HashSet()
        1 * mockMessage.setBody({
            it ->
                return it.size() == 1
        })
        0 * _
    }
}
