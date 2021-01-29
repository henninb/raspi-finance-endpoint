package finance.processors

import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import javax.validation.ConstraintViolation

@SuppressWarnings("GroovyAccessibility")
class JsonTransactionProcessorSpec extends BaseProcessor {

    protected String payloadInvalid = '''

[{"transactionId":1,"guid":"4ea3be58-3993-abcd-88a2-4ffc7f1d73ba","accountId":0,"accountType":"credit","accountNameOwner":"chase_brian","transactionDate":"2020-12-30","description":"aliexpress.com","category":"online","amount":3.14,"transactionState":"cleared","activeStatus":true,"reoccurring":false,"reoccurringType":"undefined","notes":"my note to you"},
{"transactionId":2,"guid":"4ea3be58-3993-abcd-88a2-4ffc7f1d73bb","accountId":0,"accountType":"credit","accountNameOwner":"","transactionDate":"2020-12-31","description":"aliexpress.com","category":"online","amount":3.15,"transactionState":"cleared","activeStatus":true,"reoccurring":false,"reoccurringType":"undefined","notes":"my note to you"}]
'''

            //"[{\"guid\":\"0a23fec3-18c8-4b89-a5af-68fab8db8620\",\"accountId\":0,\"accountType\":\"credit\",\"accountNameOwner\":\"invalid.invalid\",\"transactionDate\":1475647200000,\"description\":\"target.com\",\"category\":\"online\",\"amount\":33.08,\"cleared\":1,\"reoccurring\":false,\"notes\":\"my note\"}," +
            //"{\"transactionId\":0,\"guid\":\"0a23fec3-18c8-4b89-a5af-68fab8db8620\",\"accountId\":0,\"accountType\":\"credit\",\"accountNameOwner\":\"amex_brian\",\"transactionDate\":1475647200000,\"description\":\"Cafe Roale\",\"category\":\"restaurant\",\"amount\":3.08,\"cleared\":1,\"reoccurring\":false,\"notes\":\"\",\"dateUpdated\":1475588992000,\"dateAdded\":1475588992000,\"sha256\":\"\"}]"

    //TODO: not sure why the validate is not matching
    void 'test JsonTransactionProcessor - process - valid records'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        List<Transaction> transactions = [transaction]
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transactions[0])
        when:
        jsonTransactionProcessor.process(mockExchange)

        then:
        1 * mockExchange.in >> mockMessage
        1 * mockMessage.getBody(String) >> transactions.toString()
        1 * mockValidator.validate(transactions[0]) >> constraintViolations
        //1 * mockValidator.validate(_ as Transaction) >> constraintViolations
        1 * mockMessage.setBody({
            it -> return it.size() == 1
        })
        0 * _
    }

    void 'test JsonTransactionProcessor - process - invalid records'() {
        given:
//        Transaction transaction1 = TransactionBuilder.builder().build()
//        Transaction transaction2 = TransactionBuilder.builder().accountNameOwner('').build()
//        List<Transaction> transactions1 = [transaction1, transaction2]
        List<Transaction> transactions =  mapper.readValue(payloadInvalid, Transaction[])
        Set<ConstraintViolation<Transaction>> constraintViolations1 = validator.validate(transactions[0])
        Set<ConstraintViolation<Transaction>> constraintViolations2 = validator.validate(transactions[1])

        when:
        jsonTransactionProcessor.process(mockExchange)

        then:
        constraintViolations1.size() == 0
        constraintViolations2.size() == 2
        thrown(RuntimeException)
        1 * mockExchange.in >> mockMessage
        1 * mockMessage.getBody(String) >> payloadInvalid
        1 * mockValidator.validate(transactions[0]) >> constraintViolations1
        1 * mockValidator.validate(transactions[1]) >> constraintViolations2
        0 * _
    }
}
