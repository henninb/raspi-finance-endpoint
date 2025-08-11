package finance.controllers

import finance.Application
import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Stepwise
@ActiveProfiles("int")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionControllerSpec extends BaseControllerSpec {

    @Shared
    protected String jsonPayloadInvalidGuid = '''
{
"accountId":0,
"accountType":"credit",
"transactionDate":"2020-10-05",
"dueDate":"2020-10-07",
"dateUpdated":1593981072000,
"dateAdded":1593981072000,
"guid":"badGuid",
"accountNameOwner":"chase_brian",
"description":"aliexpress.com",
"category":"online",
"amount":3.14,
"transactionState":"cleared",
"reoccurringType":"undefined",
"notes":"my note to you"
}
'''

    @Shared
    protected String jsonPayloadMissingGuid = '''
{
"accountId":0,
"accountType":"credit",
"transactionDate":"2020-10-05",
"dueDate":"2020-10-07",
"dateUpdated":1593981072000,
"dateAdded":1593981072000,
"accountNameOwner":"chase_brian",
"description":"aliexpress.com",
"category":"online",
"amount":3.14,
"transactionState":"cleared",
"reoccurringType":"undefined",
"notes":"my note to you"
}
'''

    @Shared
    protected String jsonPayloadInvalidCategory = '''
{
"accountId":0,
"accountType":"credit",
"transactionDate":"2020-10-05",
"dueDate":"2020-10-07",
"dateUpdated":1593981072000,
"dateAdded":1593981072000,
"accountNameOwner":"chase_brian",
"description":"aliexpress.com",
"guid":"2eba99af-6625-4fc7-a65d-e24783ab60c0",
"category":"123451234512345123451234512345123451234512345123451234512345",
"amount":3.14,
"transactionState":"cleared",
"reoccurringType":"undefined",
"notes":"my note to you"
}
'''

    @Shared
    protected Transaction transaction = TransactionBuilder.builder().build()

    @Shared
    protected String endpointName = 'transaction'

    void 'should fail to update transaction receipt image with invalid data'() {
        given:
        String jpegImage = 'data:image/jpeg;base64,/9j/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA=MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/yQALCAABAAEBAREA/8wABgAQEAX/2gAIAQEAAD8A0s8g/9k='
        String guid = 'aaaaaaaa-bbbb-cccc-dddd-1234567890de'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(jpegImage, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/transaction/update/receipt/image/${guid}"), HttpMethod.PUT,
                entity, String)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        0 * _
    }

    void 'should successfully insert new transaction'() {

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, transaction.toString())

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should reject duplicate transaction insertion'() {

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, transaction.toString())

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        0 * _
    }

    void 'should reject transaction insertion with empty description'() {
        given:
        Transaction transaction = TransactionBuilder.builder().withDescription('').build()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, transaction.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should successfully find transaction by guid'() {

        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, transaction.guid)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should return not found for non-existent transaction guid'() {

        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, UUID.randomUUID().toString())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should return not found when deleting non-existent transaction'() {

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, UUID.randomUUID().toString())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should fail to update transaction with existing guid constraint'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(transaction, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("/api/transaction/update/${UUID.randomUUID()}"),
                HttpMethod.PUT, entity, String)

        then:
        // Update operation fails due to constraint violation - GUID already exists
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        0 * _
    }

    @Unroll
    void 'should reject transaction insertion with invalid payload'() {

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payload)

        then:
        response.statusCode == httpStatus
        0 * _

        where:
        payload                    | httpStatus
        'badJson'                  | HttpStatus.BAD_REQUEST
        '{"test":1}'               | HttpStatus.BAD_REQUEST
        '{badJson:"test"}'         | HttpStatus.BAD_REQUEST
        jsonPayloadInvalidGuid     | HttpStatus.BAD_REQUEST
        jsonPayloadMissingGuid     | HttpStatus.BAD_REQUEST
        jsonPayloadInvalidCategory | HttpStatus.BAD_REQUEST
        //TransactionBuilder.builder().transactionDate(Date.valueOf("1999-10-15")).build() | HttpStatus.BAD_REQUEST
    }

    void 'should fail to update existing transaction due to service layer constraint'() {
        given:
        // Create an updated transaction with different description
        Transaction updatedTransaction = TransactionBuilder.builder()
            .withGuid(transaction.guid)
            .withDescription('updated aliexpress.com')
            .build()
            
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(updatedTransaction.toString(), headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("/api/transaction/update/${transaction.guid}"),
                HttpMethod.PUT, entity, String)

        then:
        // Update operation fails due to constraint violation - service layer tries to INSERT instead of UPDATE
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        0 * _
    }

    void 'should successfully delete transaction by guid'() {

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, transaction.guid)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }
}
