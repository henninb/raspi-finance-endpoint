package finance.processors

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import javax.validation.ConstraintViolation

@SuppressWarnings("GroovyAccessibility")
class JsonTransactionProcessorSpec extends BaseProcessor {

    protected String payloadInvalid = '''

[{"transactionId":1,"guid":"4ea3be58-3993-abcd-88a2-4ffc7f1d73ba","accountId":0,"accountType":"credit","accountNameOwner":"chase_brian","transactionDate":"2020-12-30","description":"aliexpress.com","category":"online","amount":3.14,"transactionState":"cleared","activeStatus":true,"reoccurring":false,"reoccurringType":"undefined","notes":"my note to you"},
{"transactionId":2,"guid":"4ea3be58-3993-abcd-88a2-4ffc7f1d73bb","accountId":0,"accountType":"credit","accountNameOwner":"","transactionDate":"2020-12-31","description":"aliexpress.com","category":"online","amount":3.15,"transactionState":"cleared","activeStatus":true,"reoccurring":false,"reoccurringType":"undefined","notes":"my note to you"}]
'''

    void 'test JsonTransactionProcessor - process - valid records'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        List<Transaction> transactions = [transaction]
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transactions[0])
        exchange.in.setBody(transactions.toString())

        when:
        jsonTransactionProcessor.process(exchange)

        then:
        1 * mockValidator.validate(transactions[0]) >> constraintViolations
        0 * _
    }

    void 'test JsonTransactionProcessor - process - invalid records'() {
        given:
        List<Transaction> transactions =  mapper.readValue(payloadInvalid, Transaction[])
        Set<ConstraintViolation<Transaction>> constraintViolations1 = validator.validate(transactions[0])
        Set<ConstraintViolation<Transaction>> constraintViolations2 = validator.validate(transactions[1])
        exchange.in.setBody(payloadInvalid)

        when:
        jsonTransactionProcessor.process(exchange)

        then:
        constraintViolations1.size() == 0
        constraintViolations2.size() == 2
        thrown(RuntimeException)
        1 * mockValidator.validate(transactions[0]) >> constraintViolations1
        1 * mockValidator.validate(transactions[1]) >> constraintViolations2
        0 * _
    }

    void 'test JsonTransactionProcessor - process - invalid exchange'() {
        given:
        exchange.in.setBody('totalTrash')

        when:
        jsonTransactionProcessor.process(exchange)

        then:
        thrown(JsonParseException)
        0 * _
    }

    void 'test JsonTransactionProcessor - process - invalid json'() {
        given:
        exchange.in.setBody('{}')

        when:
        jsonTransactionProcessor.process(exchange)

        then:
        thrown(MismatchedInputException)
        0 * _
    }

    void 'test JsonTransactionProcessor - process - empty payload'() {
        given:
        exchange.in.setBody('[]')

        when:
        jsonTransactionProcessor.process(exchange)

        then:
        0 * _
    }
}
