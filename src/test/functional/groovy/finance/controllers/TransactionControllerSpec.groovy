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

    def setup() {
        println("***** setup started")
        headers = new HttpHeaders()
        account = AccountBuilder.builder().build()
        category = CategoryBuilder.builder().build()
        transaction = TransactionBuilder.builder().build()
        transaction.guid = UUID.randomUUID()
        guid = transaction.getGuid()
        categoryName = transaction.getCategory()
        accountNameOwner = transaction.getAccountNameOwner()
    }

    private String createURLWithPort(String uri) {
        println "port = ${port}"

        return "http://localhost:" + port + uri
    }

    def "test findTransaction endpoint insert - find - delete"() {

        given:
        transactionService.insertTransaction(transaction)
        HttpEntity entity = new HttpEntity<>(null, headers)
        def insertedValue = transactionService.findTransactionByGuid(guid)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/select/" + transaction.guid), HttpMethod.GET,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.OK
        insertedValue.get().guid == transaction.guid

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
        assert response.statusCode == HttpStatus.NOT_FOUND
    }

    def "test deleteTransaction endpoint insert delete guid found"() {
        given:
        transactionService.insertTransaction(transaction)

        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/delete/" + transaction.guid), HttpMethod.DELETE,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.OK
    }

    //TODO: fix the multiple category delete issue
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
                createURLWithPort("/transaction/delete/" + transaction.guid), HttpMethod.DELETE,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.OK
    }

    def "test deleteTransaction endpoint guid not found"() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/delete/" + UUID.randomUUID().toString()), HttpMethod.DELETE,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.NOT_FOUND
    }

    def "test insertTransaction endpoint bad data - not json"() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>("foo", headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/insert/"), HttpMethod.POST,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "test insertTransaction endpoint"() {
        given:
        def transaction = TransactionBuilder.builder().build()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(transaction.toString(), headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/insert/"), HttpMethod.POST,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.OK

        cleanup:
        transactionService.deleteTransactionByGuid(guid)
    }

    def "test insertTransaction endpoint - bad guid"() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        transaction.guid = "123"
        HttpEntity entity = new HttpEntity<>(transaction, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/insert/"), HttpMethod.POST,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "test insertTransaction endpoint - bad category"() {

        given:
        headers.setContentType(MediaType.APPLICATION_JSON)

        when:
        transaction.category = "123451234512345123451234512345123451234512345123451234512345"
        def badCategory = mapper.writeValueAsString(transaction)
        HttpEntity entity = new HttpEntity<>(badCategory, headers)
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/insert/"), HttpMethod.POST,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "test insertTransaction endpoint - old transaction date"() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)

        when:
        transaction.transactionDate = new java.sql.Date(100000)

        HttpEntity entity = new HttpEntity<>(transaction, headers)
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/insert/"), HttpMethod.POST,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.BAD_REQUEST
    }

    //@Ignore
    //TODO: fix Invalid HTTP method: PATCH; nested exception is java.net.ProtocolException: Invalid HTTP method: PATCH
    def "test updateTransaction endpoint"() {

        given:

        //headers.setContentType(new MediaType("application", "json-patch+json"))
        headers.setContentType(MediaType.APPLICATION_JSON)
        //headers.setContentType(HttpMethod.PATCH)

        transaction.guid = UUID.randomUUID()
        guid = transaction.guid
        transactionService.insertTransaction(transaction)
        transaction.description = "updateToDescription"
        //def updateToDescription = mapper.writeValueAsString(transaction)
        HttpEntity entity = new HttpEntity<>(transaction, headers)
        //println entity.headers
        println transaction


        //headers.set(CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())

        //RequestSpecification.contentType(String value)

        //ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<EmailPatch>(patch),
        //                    String.class);
        //HttpClient httpClient = HttpClients.createDefault();
        //restTemplate new TestRestTemplate(Collections.<HttpMessageConverter<?>> singletonList(converter));
        //restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

        when:
        def response = restTemplate.exchange(createURLWithPort("/transaction/update/" + guid),
                HttpMethod.PUT, new HttpEntity<Transaction>(transaction), String.class)
        then:
        assert response.getStatusCode() == HttpStatus.OK

        cleanup:
        transactionService.deleteTransactionByGuid(guid)
    }
}
