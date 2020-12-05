package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Shared
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import spock.lang.Specification

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

    private ObjectMapper mapper = new ObjectMapper()

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

    //TODO: bh 11/8/2020 - com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'badTransactionJsonPayload'
    //TODO: build fails in intellij
    void 'test -- insertTransaction endpoint bad data - not json in the payload'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>('badTransactionJsonPayload', headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/insert/'), HttpMethod.POST, entity, String)

        then:
        //thrown(JsonParseException)
        response.statusCode.is(HttpStatus.BAD_REQUEST)
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

    void 'test -- insertTransaction endpoint - bad guid'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        transaction.guid = '123'
        HttpEntity entity = new HttpEntity<>(transaction, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/insert/'), HttpMethod.POST,
                entity, String)
        then:
        response.statusCode.is(HttpStatus.BAD_REQUEST)
        0 * _
    }

    void 'test -- insertTransaction endpoint - bad category'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)

        when:
        transaction.category = "123451234512345123451234512345123451234512345123451234512345"
        String badCategory = mapper.writeValueAsString(transaction)
        HttpEntity entity = new HttpEntity<>(badCategory, headers)
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/insert/'), HttpMethod.POST,
                entity, String)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test -- insertTransaction endpoint - old transaction date'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        transaction.transactionDate = new java.sql.Date(100000)
        HttpEntity entity = new HttpEntity<>(transaction, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/insert/'), HttpMethod.POST,
                entity, String)
        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
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
