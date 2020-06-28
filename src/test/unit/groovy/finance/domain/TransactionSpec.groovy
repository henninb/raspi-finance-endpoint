package finance.domain

import com.fasterxml.jackson.databind.ObjectMapper
import finance.helpers.TransactionBuilder
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory
import java.math.RoundingMode
import java.sql.Date
import java.sql.Timestamp

class TransactionSpec extends Specification {

    ValidatorFactory validatorFactory
    Validator validator
    private ObjectMapper mapper = new ObjectMapper()

    //"transactionId":1001,
    def jsonPayload = '''
{"accountId":0,
"accountType":"credit",
"transactionDate":1553645394,
"dateUpdated":1553645394,
"dateAdded":1553645394,"guid":"4ea3be58-3993-46de-88a2-4ffc7f1d73bd",
"accountNameOwner":"chase_brian","description":"aliexpress.com",
"category":"online","amount":3.14,"cleared":1,"reoccurring":false,
"notes":"my note to you",
"sha256":"963e35c37ea59f3f6fa35d72fb0ba47e1e1523fae867eeeb7ead64b55ff22b77"}
'''


    void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup() {
        validatorFactory.close()
    }

    def "test Transaction to JSON"() {

        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction.class)

        when:
        def json = mapper.writeValueAsString(transactionFromString)
        println json

        then:
        json.contains("4ea3be58-3993-46de-88a2-4ffc7f1d73bd")
        0 * _
    }

    def "test JSON deserialize to Transaction domain object"() {

        given:
        Transaction transaction = mapper.readValue(jsonPayload, Transaction.class)
        when:
        println("transactionId='${transaction.transactionId}'")
        println("guid='${transaction.guid}'")
        println("accountType='${transaction.accountType}'")

        then:
        transaction.accountType == AccountType.Credit
        transaction.guid == "4ea3be58-3993-46de-88a2-4ffc7f1d73bd"
        transaction.transactionId == 0
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
    def "test validation invalid #invalidField has error #expectedError"() {
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
                .cleared(cleared)
                .reoccurring(reoccurring)
                .notes(notes)
                .dateAdded(dateAdded)
                .dateUpdated(dateUpdated)
                .sha256(sha256)
                .build()

        when:
        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction)

        then:
        println("message='${violations.message}'")
        println("size='${violations.size()}'")

        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().getInvalidValue() == transaction.getProperties()[invalidField]

        where:
        invalidField       | transactionId | guid                                   | accountId | accountType        | accountNameOwner  | transactionDate      | description      | category | amount                                                   | cleared | reoccurring | notes      | dateAdded                    | dateUpdated                  | sha256   | expectedError                              | errorCount
        //'transactionId'    | -1L           | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1004      | 'Credit'    | 'blah.chase_brian' | new Date(1553645394) | 'aliexpress.com' | 'online' | new BigDecimal(-73.14).setScale(2, RoundingMode.HALF_UP) | 1       | false       | ''         | new Timestamp(1553645394000) | new Timestamp(1553645394000) | 'sha256' | 'must be greater than or equal to 0'                   | 1
        'guid'             | 1001L         | '11ea3be58-3993-46de-88a2-4ffc7f1d73b' | 1004      | AccountType.Credit | 'chase_brian'     | new Date(1553645394) | 'aliexpress.com' | 'online' | new BigDecimal(-94.74).setScale(2, RoundingMode.HALF_UP) | -1      | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | 'sha256' | 'must be uuid formatted'                   | 1
        //  75eb28-ebd-4cd0-bc-b26721a0b787

        //'accountId'        | 1001L         |   '737fb2-1dd-bc01-4d8-2d88c5cca558' | -1L       | AccountType.Credit | 'chase_brian'     | new Date(1553645394)   | 'aliexpress.com' | 'online' | new BigDecimal(43.16).setScale(2, RoundingMode.HALF_UP)  | -2      | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | ''       | 'must be greater than or equal to 0'       | 1
        'accountId'        | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | -1L       | AccountType.Credit | 'chase_brian'     | new Date(1553645394) | 'aliexpress.com' | 'online' | new BigDecimal(43.16).setScale(2, RoundingMode.HALF_UP)  | -2      | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | ''       | 'must be greater than or equal to 0'       | 1
        'description'      | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian' | new Date(1553645394) | 'Café Roale'     | 'online' | new BigDecimal(-3.14).setScale(2, RoundingMode.HALF_UP)  | 1       | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | 'sha256' | 'must be ascii character set'              | 1
        'description'      | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian' | new Date(1553645394) | ''               | 'online' | new BigDecimal(-3.11).setScale(2, RoundingMode.HALF_UP)  | 1       | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | 'sha256' | 'size must be between 1 and 75'            | 1
        //'accountType'      | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Undefined | 'chase_brian'     | new Date(1553645394) | 'Cafe Roale'     | 'online' | new BigDecimal(23.84).setScale(2, RoundingMode.HALF_UP)  | 1       | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | 'sha256' | ''                                         | 0
        'accountNameOwner' | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one.chase_brian' | new Date(1553645394) | 'target.com'     | 'online' | new BigDecimal(13.14).setScale(2, RoundingMode.HALF_UP)  | 1       | false       | ''         | new Timestamp(1553645394000) | new Timestamp(1553645394000) | ''       | 'must be alpha separated by an underscore' | 1
        'cleared'          | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian' | new Date(1553645394) | 'Cafe Roale'     | ''       | new BigDecimal(3.14).setScale(2, RoundingMode.HALF_UP)   | 2       | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | 'sha256' | 'must be less than or equal to 1'          | 1
        'cleared'          | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Debit  | 'one-chase_brian' | new Date(1553645394) | 'Cafe Roale'     | 'online' | new BigDecimal(3.14).setScale(2, RoundingMode.HALF_UP)   | -5      | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | 'sha256' | 'must be greater than or equal to -3'      | 1
        'category'         | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian' | new Date(1553645394) | 'Cafe Roale'     | 'onliné' | new BigDecimal(3.14).setScale(2, RoundingMode.HALF_UP)   | 1       | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | 'sha256' | 'must be ascii character set'              | 1
        'amount'           | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian' | new Date(1553645394) | 'Cafe Roale'     | 'online' | new BigDecimal(3.1412)                                   | 1       | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | 'sha256' | 'must be dollar precision'                 | 1
        'transactionDate'  | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian' | new Date(946684700)  | 'Cafe Roale'     | 'online' | new BigDecimal(3.14).setScale(2, RoundingMode.HALF_UP)   | 1       | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | 'sha256' | 'date must be greater than 1/1/2000.'      | 1
        //'transactionDate'  | 1001L         | '4ea3be58-3993-46de-88a2-4ffc7f1d73bd' | 1003      | AccountType.Credit | 'one-chase_brian' | new Date(939272400000) | 'Cafe Roale'     | 'online' | new BigDecimal(3.14).setScale(2, RoundingMode.HALF_UP)   | 1       | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | 'sha256' | 'date must be greater than 1/1/2000.'      | 1
    }
}
