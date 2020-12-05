package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.TransactionBuilder
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory
import java.sql.Date

class TransactionSpec extends Specification {

    protected ValidatorFactory validatorFactory
    protected Validator validator
    protected ObjectMapper mapper = new ObjectMapper()
    protected String jsonPayload = '''
{
"accountId":0,
"accountType":"credit",
"transactionDate":"2020-10-05",
"dateUpdated":1593981072000,
"dateAdded":1593981072000,
"guid":"4ea3be58-3993-46de-88a2-4ffc7f1d73bd",
"accountNameOwner":"chase_brian",
"description":"aliexpress.com",
"category":"online",
"amount":3.14,
"transactionState":"cleared",
"reoccurring":false,
"reoccurringType":"undefined",
"notes":"my note to you"
}
'''

    void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup() {
        validatorFactory.close()
    }

    void 'test Transaction to JSON'() {
        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction)

        when:
        String json = mapper.writeValueAsString(transactionFromString)

        then:
        json.contains(transactionFromString.guid)
        json.contains(transactionFromString.description)
        json.contains(transactionFromString.notes)
        json.contains(transactionFromString.transactionState.toString())
        0 * _
    }

    void 'test -- JSON deserialize to Transaction with valid payload'() {
        when:
        Transaction transaction = mapper.readValue(jsonPayload, Transaction)

        then:
        transaction.accountType == AccountType.Credit
        transaction.guid == '4ea3be58-3993-46de-88a2-4ffc7f1d73bd'
        transaction.transactionId == 0
        0 * _
    }

    @Unroll
    void 'test -- JSON deserialize to Transaction with invalid payload'() {
        when:
        mapper.readValue(payload, Transaction)

        then:
        Exception ex = thrown(exceptionThrown)
        ex.message.contains(message)
        0 * _

        where:
        payload                 | exceptionThrown          | message
        'non-jsonPayload'       | JsonParseException       | 'Unrecognized token'
        '[]'                    | MismatchedInputException | 'Cannot deserialize value of type'
        '{description: "test"}' | JsonParseException       | 'was expecting double-quote to start field name'
        '{"amount": "abc"}'     | InvalidFormatException   | 'Cannot deserialize value of type'
        //'{"amount": 1.5555}'
        //'{}'
        //'{"description": "test"}'
    }

    void 'test validation valid transaction'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction)

        then:
        violations.empty
        0 * _
    }

    @Unroll
    void 'test validation invalid #invalidField has error expectedError'() {
        given:
        Transaction transaction = new TransactionBuilder().builder()
                .guid(guid)
                .accountId(accountId)
                .accountType(accountType)
                .accountNameOwner(accountNameOwner)
                .transactionDate(transactionDate)
                .description(description)
                .category(category)
                .amount(amount)
                .transactionState(transactionState)
                .reoccurring(reoccurring)
                .reoccurringType(reoccurringType)
                .notes(notes)
                .build()

        when:
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().invalidValue == transaction.properties[invalidField]

        where:
        invalidField       | guid                                   | accountId | accountType        | accountNameOwner   | transactionDate      | description      | category | amount  | transactionState         | reoccurring | reoccurringType           | notes      | expectedError                              | errorCount
        'guid'             | '11ea3be58-3993-46de-88a2-4ffc7f1d73b' | 1004      | AccountType.Credit | 'chase_brian'      | new Date(1553645394) | 'aliexpress.com' | 'online' | -94.74G | TransactionState.Future  | false       | ReoccurringType.Undefined | 'no notes' | 'must be uuid formatted'                   | 1
        'accountId'        | UUID.randomUUID().toString()           | -1L       | AccountType.Credit | 'chase_brian'      | new Date(1553645394) | 'aliexpress.com' | 'online' | 43.16G  | TransactionState.Future  | false       | ReoccurringType.Undefined | 'no notes' | 'must be greater than or equal to 0'       | 1
        'description'      | UUID.randomUUID().toString()           | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(1553645394) | 'Café Roale'     | 'online' | -3.14G  | TransactionState.Cleared | false       | ReoccurringType.Undefined | 'no notes' | 'must be ascii character set'              | 1
        'description'      | UUID.randomUUID().toString()           | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(1553645394) | ''               | 'online' | -3.11G  | TransactionState.Cleared | false       | ReoccurringType.Undefined | 'no notes' | 'size must be between 1 and 75'            | 1
        'accountNameOwner' | UUID.randomUUID().toString()           | 1003      | AccountType.Credit | 'one.chase_brian'  | new Date(1553645394) | 'target.com'     | 'online' | 13.14G  | TransactionState.Cleared | false       | ReoccurringType.Undefined | ''         | 'must be alpha separated by an underscore' | 1
        'accountNameOwner' | UUID.randomUUID().toString()           | 1003      | AccountType.Credit | 'one -chase_brian' | new Date(1553645394) | 'target.com'     | 'online' | 13.14G  | TransactionState.Cleared | false       | ReoccurringType.Undefined | ''         | 'must be alpha separated by an underscore' | 1
        'accountNameOwner' | UUID.randomUUID().toString()           | 1003      | AccountType.Credit | 'brian'            | new Date(1553645394) | 'target.com'     | 'online' | 13.14G  | TransactionState.Cleared | false       | ReoccurringType.Undefined | ''         | 'must be alpha separated by an underscore' | 1
        'category'         | UUID.randomUUID().toString()           | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(1553645394) | 'Cafe Roale'     | 'onliné' | 3.14G   | TransactionState.Cleared | false       | ReoccurringType.Undefined | 'no notes' | 'must be alphanumeric no space'            | 1
        'category'         | UUID.randomUUID().toString()           | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(1553645394) | 'Cafe Roale'     | 'Online' | 3.14G   | TransactionState.Cleared | false       | ReoccurringType.Undefined | 'no notes' | 'must be alphanumeric no space'            | 1
        'amount'           | UUID.randomUUID().toString()           | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(1553645394) | 'Cafe Roale'     | 'online' | 3.1412G | TransactionState.Cleared | false       | ReoccurringType.Undefined | 'no notes' | 'must be dollar precision'                 | 1
        'transactionDate'  | UUID.randomUUID().toString()           | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(946684700)  | 'Cafe Roale'     | 'online' | 3.14G   | TransactionState.Cleared | false       | ReoccurringType.Undefined | 'no notes' | 'date must be greater than 1/1/2000.'      | 1
    }
}
