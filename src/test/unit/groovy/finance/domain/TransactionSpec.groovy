package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
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
import java.text.ParseException

class TransactionSpec extends Specification {

    protected ValidatorFactory validatorFactory
    protected Validator validator
    protected ObjectMapper mapper = new ObjectMapper()
    protected String jsonPayload = '''
{
"accountId":1,
"accountType":"credit",
"transactionDate":"2020-10-05",
"dueDate":"2020-10-15",
"guid":"4ea3be58-3993-46de-88a2-4ffc7f1d73bd",
"accountNameOwner":"chase_brian",
"description":"aliexpress.com",
"category":"online",
"amount":3.14,
"transactionState":"cleared",
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
        noExceptionThrown()
        json.contains(transactionFromString.guid)
        json.contains(transactionFromString.description)
        json.contains(transactionFromString.notes)
        json.contains(transactionFromString.transactionState.toString())
        json.contains(transactionFromString.transactionDate.toString())
        0 * _
    }

    void 'test Transaction to JSON - date'() {
        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction)

        when:
        String json = mapper.writeValueAsString(transactionFromString)

        and:
        Transaction transactionDeserialized = mapper.readValue(json, Transaction)

        then:
        noExceptionThrown()
        transactionFromString.transactionDate == transactionDeserialized.transactionDate
        0 * _
    }

    void 'test -- JSON deserialize to Transaction with valid payload'() {
        when:
        Transaction transaction = mapper.readValue(jsonPayload, Transaction)

        then:
        noExceptionThrown()
        transaction.accountType == AccountType.Credit
        transaction.guid == '4ea3be58-3993-46de-88a2-4ffc7f1d73bd'
        transaction.transactionId == 0
        0 * _
    }

    @Unroll
    void 'test - transaction bad date payload'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        when:
        transaction.jsonSetterTransactionDate(payload)

        then:
        thrown(thownExecption)
        0 * _

        where:
        payload      | thownExecption
        '2020-11-31' | ParseException
        '2021-02-29' | ParseException
        'invalid'    | ParseException
    }

    @Unroll
    void 'test - transaction bad data in json payload'() {
        when:
        mapper.readValue(payload, Transaction)

        then:
        thrown(thownExecption)
        0 * _

        where:
        payload                            | thownExecption
        '{"transactionDate":1234}'         | JsonMappingException
        '{"transactionDate":"1/20/2020"}'  | JsonMappingException
        '{"transactionDate":"2020-04-31"}' | JsonMappingException
        '{"accountType":"notValid"}'       | JsonMappingException
        '{"accountType":"notValid"}'       | JsonMappingException
        '{"reoccurringType":"notValid"}'   | JsonMappingException
        '{"amount":"1.222a"}'              | JsonMappingException
        'invalid'                          | JsonParseException
        '[]'                               | MismatchedInputException
        '{description: "test"}'            | JsonParseException
        '{"amount": "abc"}'                | InvalidFormatException
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
                .withGuid(guid.toString())
                .withAccountId(accountId)
                .withAccountType(accountType)
                .withAccountNameOwner(accountNameOwner)
                .withTransactionDate(transactionDate)
                .withDescription(description)
                .withCategory(category)
                .withAmount(amount)
                .withTransactionState(transactionState)
                .withReoccurringType(reoccurringType)
                .withNotes(notes)
                .build()

        when:
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().invalidValue == transaction.properties[invalidField]

        where:
        invalidField       | guid                                   | accountId | accountType        | accountNameOwner   | transactionDate                          | description      | category | amount  | transactionState         | reoccurringType           | notes      | expectedError                              | errorCount
        'guid'             | '11ea3be58-3993-46de-88a2-4ffc7f1d73b' | 1004      | AccountType.Credit | 'chase_brian'      | new Date(Calendar.instance.timeInMillis) | 'aliexpress.com' | 'online' | -94.74G | TransactionState.Future  | ReoccurringType.Undefined | 'no notes' | 'must be uuid formatted'                   | 1
        'accountId'        | UUID.randomUUID()                      | -1L       | AccountType.Credit | 'chase_brian'      | new Date(Calendar.instance.timeInMillis) | 'aliexpress.com' | 'online' | 43.16G  | TransactionState.Future  | ReoccurringType.Undefined | 'no notes' | 'must be greater than or equal to 0'       | 1
        'description'      | UUID.randomUUID()                      | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(Calendar.instance.timeInMillis) | 'Café Roale'     | 'online' | -3.14G  | TransactionState.Cleared | ReoccurringType.Undefined | 'no notes' | 'must be ascii character set'              | 1
        'description'      | UUID.randomUUID()                      | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(Calendar.instance.timeInMillis) | ''               | 'online' | -3.11G  | TransactionState.Cleared | ReoccurringType.Undefined | 'no notes' | 'size must be between 1 and 75'            | 1
        'accountNameOwner' | UUID.randomUUID()                      | 1003      | AccountType.Credit | 'one.chase_brian'  | new Date(Calendar.instance.timeInMillis) | 'target.com'     | 'online' | 13.14G  | TransactionState.Cleared | ReoccurringType.Undefined | ''         | 'must be alpha separated by an underscore' | 1
        'accountNameOwner' | UUID.randomUUID()                      | 1003      | AccountType.Credit | 'one -chase_brian' | new Date(Calendar.instance.timeInMillis) | 'target.com'     | 'online' | 13.14G  | TransactionState.Cleared | ReoccurringType.Undefined | ''         | 'must be alpha separated by an underscore' | 1
        'accountNameOwner' | UUID.randomUUID()                      | 1003      | AccountType.Credit | 'brian'            | new Date(Calendar.instance.timeInMillis) | 'target.com'     | 'online' | 13.14G  | TransactionState.Cleared | ReoccurringType.Undefined | ''         | 'must be alpha separated by an underscore' | 1
        'category'         | UUID.randomUUID()                      | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(Calendar.instance.timeInMillis) | 'Cafe Roale'     | 'onliné' | 3.14G   | TransactionState.Cleared | ReoccurringType.Undefined | 'no notes' | 'must be alphanumeric no space'            | 1
        'category'         | UUID.randomUUID()                      | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(Calendar.instance.timeInMillis) | 'Cafe Roale'     | 'Online' | 3.14G   | TransactionState.Cleared | ReoccurringType.Undefined | 'no notes' | 'must be alphanumeric no space'            | 1
        'amount'           | UUID.randomUUID()                      | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(Calendar.instance.timeInMillis) | 'Cafe Roale'     | 'online' | 3.1412G | TransactionState.Cleared | ReoccurringType.Undefined | 'no notes' | 'must be dollar precision'                 | 1
        'transactionDate'  | UUID.randomUUID()                      | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(946684700)                      | 'Cafe Roale'     | 'online' | 3.14G   | TransactionState.Cleared | ReoccurringType.Undefined | 'no notes' | 'date must be greater than 1/1/2000.'      | 1
    }
}
