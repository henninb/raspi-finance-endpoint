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
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    @Autowired
    TransactionService transactionService

    @Autowired
    AccountService accountService

    @Autowired
    CategoryService categoryService

    TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    HttpHeaders headers

    @Shared
    Transaction transaction

    @Shared
    Account account

    @Shared
    Category category

    @Shared
    String guid

    @Shared
    String categoryName

    @Shared
    String accountNameOwner

    private ObjectMapper mapper = new ObjectMapper()

    def setupSpec() {
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

    def "test findTransaction endpoint insert - find - delete"() {

        given:
        transactionService.insertTransaction(transaction)
        HttpEntity entity = new HttpEntity<>(null, headers)
        def insertedValue = transactionService.findTransactionByGuid(guid)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/select/' + transaction.guid), HttpMethod.GET,
                entity, String.class)

        then:
        response.statusCode == HttpStatus.OK
        insertedValue.get().guid == transaction.guid
        0 * _

        cleanup:
        transactionService.deleteTransactionByGuid(transaction.guid)
    }

    def "test findTransaction endpoint transaction insert guid is not found"() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/select/" + UUID.randomUUID().toString()), HttpMethod.GET,
                entity, String.class)
        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    def "test deleteTransaction endpoint insert delete guid found"() {
        given:
        transactionService.insertTransaction(transaction)

        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/delete/' + transaction.guid), HttpMethod.DELETE,
                entity, String.class)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    //TODO: bh fix the multiple category delete issue
    @Ignore
    def "test deleteTransaction endpoint insert delete guid found - multiple categories associated"() {
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
                entity, String.class)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    def "test deleteTransaction endpoint guid not found"() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/delete/" + UUID.randomUUID().toString()), HttpMethod.DELETE,
                entity, String.class)
        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    @Ignore
    //TODO: bh 11/8/2020 - com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'badTransactionJsonPayload'
    def "test insertTransaction endpoint bad data - not json"() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>('badTransactionJsonPayload', headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/insert/'), HttpMethod.POST,
                entity, String.class)
        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    def "test insertTransaction endpoint"() {
        given:
        def transaction = TransactionBuilder.builder().build()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(transaction.toString(), headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/insert/'), HttpMethod.POST,
                entity, String.class)
        then:
        response.statusCode == HttpStatus.OK
        0 * _

        cleanup:
        transactionService.deleteTransactionByGuid(guid)
    }

    def "test insertTransaction endpoint - bad guid"() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        transaction.guid = '123'
        HttpEntity entity = new HttpEntity<>(transaction, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/insert/'), HttpMethod.POST,
                entity, String.class)
        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    def "test insertTransaction endpoint - bad category"() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)

        when:
        transaction.category = "123451234512345123451234512345123451234512345123451234512345"
        def badCategory = mapper.writeValueAsString(transaction)
        HttpEntity entity = new HttpEntity<>(badCategory, headers)
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/insert/'), HttpMethod.POST,
                entity, String.class)
        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    def "test insertTransaction endpoint - old transaction date"() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        transaction.transactionDate = new java.sql.Date(100000)
        HttpEntity entity = new HttpEntity<>(transaction, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/transaction/insert/'), HttpMethod.POST,
                entity, String.class)
        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    //TODO: should this fail as a bad request?
    // and duplicate constraint violation
    @Ignore
    def "test updateTransaction transaction endpoint"() {
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
                HttpMethod.PUT, entity, String.class)

        then:
        response.getStatusCode() == HttpStatus.BAD_REQUEST
        0 * _

        cleanup:
        transactionService.deleteTransactionByGuid(guid)
    }
}
