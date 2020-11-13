package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import finance.helpers.TransactionBuilder
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory
import java.math.RoundingMode
import java.sql.Date

class TransactionSpec extends Specification {

    ValidatorFactory validatorFactory
    Validator validator
    private ObjectMapper mapper = new ObjectMapper()

    def jsonPayload = '''
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

    def setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    def cleanup() {
        validatorFactory.close()
    }

    def "test Transaction to JSON"() {
        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction.class)

        when:
        def json = mapper.writeValueAsString(transactionFromString)

        then:
        json.contains("4ea3be58-3993-46de-88a2-4ffc7f1d73bd")
        0 * _
    }

    def "test JSON deserialize to Transaction domain object"() {
        when:
        Transaction transaction = mapper.readValue(jsonPayload, Transaction.class)

        then:
        transaction.accountType == AccountType.Credit
        transaction.guid == "4ea3be58-3993-46de-88a2-4ffc7f1d73bd"
        transaction.transactionId == 0
        0 * _
    }

    def "test JSON deserialize to Transaction domain object - bad"() {
        when:
        mapper.readValue('trash-payload', Transaction.class)

        then:
        JsonParseException ex = thrown()
        ex.getMessage().contains('Unrecognized token')
        0 * _
    }


    def "test validation valid transaction"() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction)

        then:
        violations.isEmpty()
        0 * _
    }

    @Unroll
    def "test validation invalid #invalidField has error expectedError"() {
        given:
        Transaction transaction = new TransactionBuilder()
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
        violations.iterator().next().getInvalidValue() == transaction.getProperties()[invalidField]

        where:
        invalidField       | transactionId | guid                                   | accountId | accountType        | accountNameOwner   | transactionDate      | description      | category | amount                                                   | transactionState         | reoccurring | reoccurringType           | notes      | expectedError                              | errorCount
        'guid'             | 1001L         | '11ea3be58-3993-46de-88a2-4ffc7f1d73b' | 1004      | AccountType.Credit | 'chase_brian'      | new Date(1553645394) | 'aliexpress.com' | 'online' | new BigDecimal(-94.74).setScale(2, RoundingMode.HALF_UP) | TransactionState.Future  | false       | ReoccurringType.Undefined | 'no notes' | 'must be uuid formatted'                   | 1
        'accountId'        | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | -1L       | AccountType.Credit | 'chase_brian'      | new Date(1553645394) | 'aliexpress.com' | 'online' | new BigDecimal(43.16).setScale(2, RoundingMode.HALF_UP)  | TransactionState.Future  | false       | ReoccurringType.Undefined | 'no notes' | 'must be greater than or equal to 0'       | 1
        'description'      | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(1553645394) | 'Café Roale'     | 'online' | new BigDecimal(-3.14).setScale(2, RoundingMode.HALF_UP)  | TransactionState.Cleared | false       | ReoccurringType.Undefined | 'no notes' | 'must be ascii character set'              | 1
        'description'      | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(1553645394) | ''               | 'online' | new BigDecimal(-3.11).setScale(2, RoundingMode.HALF_UP)  | TransactionState.Cleared | false       | ReoccurringType.Undefined | 'no notes' | 'size must be between 1 and 75'            | 1
        'accountNameOwner' | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one.chase_brian'  | new Date(1553645394) | 'target.com'     | 'online' | new BigDecimal(13.14).setScale(2, RoundingMode.HALF_UP)  | TransactionState.Cleared | false       | ReoccurringType.Undefined | ''         | 'must be alpha separated by an underscore' | 1
        'accountNameOwner' | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one -chase_brian' | new Date(1553645394) | 'target.com'     | 'online' | new BigDecimal(13.14).setScale(2, RoundingMode.HALF_UP)  | TransactionState.Cleared | false       | ReoccurringType.Undefined | ''         | 'must be alpha separated by an underscore' | 1
        'accountNameOwner' | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'brian'            | new Date(1553645394) | 'target.com'     | 'online' | new BigDecimal(13.14).setScale(2, RoundingMode.HALF_UP)  | TransactionState.Cleared | false       | ReoccurringType.Undefined | ''         | 'must be alpha separated by an underscore' | 1
        'category'         | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(1553645394) | 'Cafe Roale'     | 'onliné' | new BigDecimal(3.14).setScale(2, RoundingMode.HALF_UP)   | TransactionState.Cleared | false       | ReoccurringType.Undefined | 'no notes' | 'must be alphanumeric no space'            | 1
        'category'         | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(1553645394) | 'Cafe Roale'     | 'Online' | new BigDecimal(3.14).setScale(2, RoundingMode.HALF_UP)   | TransactionState.Cleared | false       | ReoccurringType.Undefined | 'no notes' | 'must be alphanumeric no space'            | 1
        'amount'           | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(1553645394) | 'Cafe Roale'     | 'online' | new BigDecimal(3.1412)                                   | TransactionState.Cleared | false       | ReoccurringType.Undefined | 'no notes' | 'must be dollar precision'                 | 1
        'transactionDate'  | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian'  | new Date(946684700)  | 'Cafe Roale'     | 'online' | new BigDecimal(3.14).setScale(2, RoundingMode.HALF_UP)   | TransactionState.Cleared | false       | ReoccurringType.Undefined | 'no notes' | 'date must be greater than 1/1/2000.'      | 1
    }
}
