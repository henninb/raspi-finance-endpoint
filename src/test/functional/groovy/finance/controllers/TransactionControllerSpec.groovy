package finance.controllers


import finance.Application
import finance.domain.Account
import finance.domain.Category
import finance.domain.Transaction
import finance.helpers.AccountBuilder
import finance.helpers.CategoryBuilder
import finance.helpers.TransactionBuilder
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    @Autowired
    protected TransactionService transactionService

    @Autowired
    protected AccountService accountService

    @Autowired
    protected CategoryService categoryService

    protected TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    protected HttpHeaders headers

    @Shared
    protected Transaction transaction

    @Shared
    protected Account account

    @Shared
    protected Category category

    @Shared
    protected String guid

    @Shared
    protected String categoryName

    @Shared
    protected String accountNameOwner

    @Shared
    protected String jsonPayloadInvalidGuid = '''
{
"accountId":0,
"accountType":"credit",
"transactionDate":"2020-10-05",
"dateUpdated":1593981072000,
"dateAdded":1593981072000,
"guid":"badGuid",
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

    @Shared
    protected String jsonPayloadMissingGuid = '''
{
"accountId":0,
"accountType":"credit",
"transactionDate":"2020-10-05",
"dateUpdated":1593981072000,
"dateAdded":1593981072000,
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

    @Shared
    protected String jsonPayloadInvalidCategory = '''
{
"accountId":0,
"accountType":"credit",
"transactionDate":"2020-10-05",
"dateUpdated":1593981072000,
"dateAdded":1593981072000,
"accountNameOwner":"chase_brian",
"description":"aliexpress.com",
"guid":"2eba99af-6625-4fc7-a65d-e24783ab60c0",
"category":"123451234512345123451234512345123451234512345123451234512345",
"amount":3.14,
"transactionState":"cleared",
"reoccurring":false,
"reoccurringType":"undefined",
"notes":"my note to you"
}
'''

    @Shared
    protected Transaction transactionOldTransactionDate = TransactionBuilder.builder().build()

    void setupSpec() {
        headers = new HttpHeaders()
        account = AccountBuilder.builder().build()
        category = CategoryBuilder.builder().build()
        transaction = TransactionBuilder.builder().build()
        transaction.guid = UUID.randomUUID()
        guid = transaction.guid
        transaction.category = UUID.randomUUID()
        categoryName = transaction.category
        accountNameOwner = transaction.accountNameOwner
        transactionOldTransactionDate.transactionDate = new java.sql.Date(100000)
    }

    private String createURLWithPort(String uri) {
        return 'http://localhost:' + port + uri
    }

    void 'test -- findTransaction endpoint insert - find - delete'() {

        given:
        transactionService.insertTransaction(transaction)
        HttpEntity entity = new HttpEntity<>(null, headers)
        Optional<Transaction> insertedValue = transactionService.findTransactionByGuid(guid)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/select/' + transaction.guid), HttpMethod.GET,
                entity, String)

        then:
        response.statusCode.is(HttpStatus.OK)
        insertedValue.get().guid == transaction.guid
        0 * _

        cleanup:
        transactionService.deleteTransactionByGuid(transaction.guid)
    }

    void 'test -- findTransaction endpoint transaction insert guid is not found'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/select/" + UUID.randomUUID().toString()), HttpMethod.GET,
                entity, String)
        then:
        response.statusCode.is(HttpStatus.NOT_FOUND)
        0 * _
    }

    void 'test -- deleteTransaction endpoint insert delete guid found'() {
        given:
        transactionService.insertTransaction(transaction)

        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/delete/' + transaction.guid), HttpMethod.DELETE,
                entity, String)
        then:
        response.statusCode.is(HttpStatus.OK)
        0 * _
    }

    //TODO: bh fix the multiple category delete issue
    @Ignore
    void 'test -- deleteTransaction endpoint insert delete guid found - multiple categories associated'() {
        given:
        Category categoryNew = new Category()
        categoryNew.category = "new_cat"
        categoryService.insertCategory(categoryNew)
        transaction.categories.add(categoryNew)
        transactionService.insertTransaction(transaction)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/delete/' + transaction.guid), HttpMethod.DELETE,
                entity, String)
        then:
        response.statusCode.is(HttpStatus.OK)
        0 * _
    }

    void 'test -- deleteTransaction endpoint guid not found'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/delete/" + UUID.randomUUID().toString()), HttpMethod.DELETE,
                entity, String)
        then:
        response.statusCode.is(HttpStatus.NOT_FOUND)
        0 * _
    }

    void 'test -- insertTransaction endpoint'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(transaction.toString(), headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/insert/'), HttpMethod.POST,
                entity, String)
        then:
        response.statusCode.is(HttpStatus.OK)
        0 * _

        cleanup:
        transactionService.deleteTransactionByGuid(guid)
    }

    @Unroll
    void 'test insertTransaction endpoint - failure for irregular payload'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/insert/'), HttpMethod.POST,
                entity, String)
        then:
        response.statusCode.is(httpStatus)
        response.body.contains(responseBody)
        0 * _

        where:
        payload                                  | httpStatus             | responseBody
        'badJson'                                | HttpStatus.BAD_REQUEST | 'Unrecognized token'
        '{"test":1}'                             | HttpStatus.BAD_REQUEST | 'value failed for JSON property guid due to missing'
        '{badJson:"test"}'                       | HttpStatus.BAD_REQUEST | 'was expecting double-quote to start field'
        jsonPayloadInvalidGuid                   | HttpStatus.BAD_REQUEST | 'Cannot insert transaction as there is a constraint violation on the data.'
        jsonPayloadMissingGuid                   | HttpStatus.BAD_REQUEST | 'value for creator parameter guid which is a non-nullable type'
        jsonPayloadInvalidCategory               | HttpStatus.BAD_REQUEST | 'Cannot insert transaction as there is a constraint violation on the data'
        transactionOldTransactionDate.toString() | HttpStatus.BAD_REQUEST | 'Cannot insert transaction as there is a constraint violation on the data.'
    }

    //TODO: should this fail as a bad request?
    //TODO: build fails and duplicate constraint violation
    @Ignore
    void 'test -- updateTransaction transaction endpoint'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        transaction.guid = UUID.randomUUID()
        guid = transaction.guid
        transaction.description = 'firstDescription'
        transactionService.insertTransaction(transaction)
        transaction.description = 'updateToDescription'
        HttpEntity entity = new HttpEntity<>(transaction, headers)

        when:
        def response = restTemplate.exchange(createURLWithPort('/transaction/update/' + guid),
                HttpMethod.PUT, entity, String)

        then:
        response.getStatusCode() == HttpStatus.BAD_REQUEST
        0 * _

        cleanup:
        transactionService.deleteTransactionByGuid(guid)
    }
}
