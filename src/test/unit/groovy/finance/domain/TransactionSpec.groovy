package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.TransactionBuilder
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation
import java.time.LocalDate

import static finance.utils.Constants.*

class TransactionSpec extends BaseDomainSpec {
    protected String jsonPayload = '''
{
"accountId":1,
"accountType":"credit",
"transactionType":"expense",
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

    void 'test Transaction to JSON'() {
        given:
        Transaction transactionFromString = mapper.readValue(jsonPayload, Transaction)

        when:
        String json = mapper.writeValueAsString(transactionFromString)
        def jsonNode = mapper.readTree(json)
        def expectedDateArray = "[${transactionFromString.transactionDate.year},${transactionFromString.transactionDate.monthValue},${transactionFromString.transactionDate.dayOfMonth}]"

        then:
        noExceptionThrown()
        json.contains(transactionFromString.guid)
        json.contains(transactionFromString.description)
        json.contains(transactionFromString.notes)
        json.contains(transactionFromString.transactionState.toString())
        jsonNode.get("transactionDate").toString() == expectedDateArray
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
        transaction.transactionType == TransactionType.Expense
        transaction.guid == '4ea3be58-3993-46de-88a2-4ffc7f1d73bd'
        transaction.transactionId == 0
        0 * _
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
        '{"transactionDate":"1/20/2020"}'  | JsonMappingException
        '{"transactionDate":"2020-04-31"}' | JsonMappingException
        '{"accountType":"notValid"}'       | JsonMappingException
        '{"accountType":"notValid"}'       | JsonMappingException
        '{"transactionType":"notValid"}'   | JsonMappingException
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
                .withTransactionType(transactionType)
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
        invalidField       | guid                                   | accountId | accountType        | transactionType         | accountNameOwner   | transactionDate               | description      | category | amount  | transactionState         | reoccurringType           | notes      | expectedError                                       | errorCount
        'guid'             | '11ea3be58-3993-46de-88a2-4ffc7f1d73b' | 1004      | AccountType.Credit | TransactionType.Expense | 'chase_brian'      | LocalDate.now()                | 'aliexpress.com' | 'online' | -94.74G | TransactionState.Future  | ReoccurringType.Undefined | 'no notes' | FIELD_MUST_BE_UUID_MESSAGE                          | 1
        'accountId'        | UUID.randomUUID()                      | -1L       | AccountType.Credit | TransactionType.Expense | 'chase_brian'      | LocalDate.now()                | 'aliexpress.com' | 'online' | 43.16G  | TransactionState.Future  | ReoccurringType.Undefined | 'no notes' | FILED_MUST_BE_GREATER_THAN_ZERO_MESSAGE             | 1
        'description'      | UUID.randomUUID()                      | 1003      | AccountType.Credit | TransactionType.Expense | 'one-chase_brian'  | LocalDate.now()                | 'Café Roale'     | 'online' | -3.14G  | TransactionState.Cleared | ReoccurringType.Undefined | 'no notes' | FIELD_MUST_BE_ASCII_MESSAGE                         | 1
        'description'      | UUID.randomUUID()                      | 1003      | AccountType.Credit | TransactionType.Expense | 'one-chase_brian'  | LocalDate.now()                | ''               | 'online' | -3.11G  | TransactionState.Cleared | ReoccurringType.Undefined | 'no notes' | FILED_MUST_BE_BETWEEN_ONE_AND_SEVENTY_FIVE_MESSAGE  | 1
        'accountNameOwner' | UUID.randomUUID()                      | 1003      | AccountType.Credit | TransactionType.Expense | 'one.chase_brian'  | LocalDate.now()                | 'target.com'     | 'online' | 13.14G  | TransactionState.Cleared | ReoccurringType.Undefined | ''         | FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE | 1
        'accountNameOwner' | UUID.randomUUID()                      | 1003      | AccountType.Credit | TransactionType.Expense | 'one -chase_brian' | LocalDate.now()                | 'target.com'     | 'online' | 13.14G  | TransactionState.Cleared | ReoccurringType.Undefined | ''         | FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE | 1
        'accountNameOwner' | UUID.randomUUID()                      | 1003      | AccountType.Credit | TransactionType.Expense | 'brian'            | LocalDate.now()                | 'target.com'     | 'online' | 13.14G  | TransactionState.Cleared | ReoccurringType.Undefined | ''         | FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE | 1
        'category'         | UUID.randomUUID()                      | 1003      | AccountType.Credit | TransactionType.Expense | 'one-chase_brian'  | LocalDate.now()                | 'Cafe Roale'     | 'onliné' | 3.14G   | TransactionState.Cleared | ReoccurringType.Undefined | 'no notes' | FILED_MUST_BE_ALPHA_NUMERIC_NO_SPACE_MESSAGE        | 1
        'category'         | UUID.randomUUID()                      | 1003      | AccountType.Credit | TransactionType.Expense | 'one-chase_brian'  | LocalDate.now()                | 'Cafe Roale'     | 'Online' | 3.14G   | TransactionState.Cleared | ReoccurringType.Undefined | 'no notes' | FILED_MUST_BE_ALPHA_NUMERIC_NO_SPACE_MESSAGE        | 1
        'amount'           | UUID.randomUUID()                      | 1003      | AccountType.Credit | TransactionType.Expense | 'one-chase_brian'  | LocalDate.now()                | 'Cafe Roale'     | 'online' | 3.1412G | TransactionState.Cleared | ReoccurringType.Undefined | 'no notes' | FIELD_MUST_BE_A_CURRENCY_MESSAGE                    | 1
        'transactionDate'  | UUID.randomUUID()                      | 1003      | AccountType.Credit | TransactionType.Expense | 'one-chase_brian'  | LocalDate.of(1999, 12, 31)     | 'Cafe Roale'     | 'online' | 3.14G   | TransactionState.Cleared | ReoccurringType.Undefined | 'no notes' | FILED_MUST_BE_DATE_GREATER_THAN_MESSAGE             | 1
    }
}
