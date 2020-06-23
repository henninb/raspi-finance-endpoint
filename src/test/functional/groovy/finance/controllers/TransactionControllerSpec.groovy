package finance.controllers

import finance.Application
import finance.domain.Account
import finance.domain.Category
import finance.domain.Transaction
import finance.helpers.CategoryBuilder
import finance.helpers.TransactionBuilder
import finance.helpers.AccountBuilder
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification

@ActiveProfiles("stage")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    HttpHeaders headers

    @Autowired
    TransactionService transactionService

    @Autowired
    AccountService accountService

    @Autowired
    CategoryService categoryService

    //@Autowired
    //TransactionDAO transactionDAO

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


    String json =
     """
{
"guid":"4ea3be58-3993-46de-88a2-4ffc7f1d73bd",
"accountNameOwner":"foo_brian",
"description":"Deposit",
"category":"none",
"amount":12.10,"cleared":1,"reoccurring":false,
"notes":"my notes","sha256":"",
"transactionId":0,"accountId":0,
"accountType":"debit",
"transactionDate":1435467600000,
"dateUpdated":1435502109000,
"dateAdded":1435502109000
}
     """


    String jsonUpdateDescription =
            """
{
"guid":"4ea3be58-3993-46de-88a2-4ffc7f1d73bd",
"accountNameOwner":"foo_brian",
"description":"Deposit - updated",
"category":"none",
"amount":12.10,"cleared":1,"reoccurring":false,
"notes":"my notes","sha256":"",
"transactionId":0,"accountId":0,
"accountType":"debit",
"transactionDate":1435467600000,
"dateUpdated":1435502109000,
"dateAdded":1435502109000
}
     """

    def setupSpec() {
        headers = new HttpHeaders()
        account = AccountBuilder.builder().build()
        category = CategoryBuilder.builder().build()
        transaction = TransactionBuilder.builder().build()
        guid = transaction.getGuid()
        categoryName = transaction.getCategory()
        accountNameOwner = transaction.getAccountNameOwner()
    }

    private String createURLWithPort(String uri) {
        println "port = ${port}"

        return "http://localhost:" + port + uri
    }

    def "test findTransaction endpoint found"() {
        given:
        accountService.insertAccount(account)
        transactionService.insertTransaction(transaction)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when: "rest call is initiated"
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/select/" + guid), HttpMethod.GET,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.OK

        cleanup:
        transactionService.deleteByGuid(guid)
        accountService.deleteByAccountNameOwner(accountNameOwner)
    }

    def "test findTransaction endpoint not found"() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)
        accountService.insertAccount(account)
        transactionService.insertTransaction(transaction)

        when: "rest call is initiated"
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/select/" + UUID.randomUUID().toString()), HttpMethod.GET,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.NOT_FOUND

        cleanup:
        transactionService.deleteByGuid(guid)
        accountService.deleteByAccountNameOwner(accountNameOwner)
    }

    def "test deleteTransaction endpoint guid found"() {
        given:
        accountService.insertAccount(account)
        transactionService.insertTransaction(transaction)

        HttpEntity entity = new HttpEntity<>(null, headers)

        when: "rest call is initiated"
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/delete/" + guid), HttpMethod.DELETE,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.OK

        cleanup:
        transactionService.deleteByGuid(guid)
        accountService.deleteByAccountNameOwner(accountNameOwner)
    }

    def "test deleteTransaction endpoint guid not found"() {
        given:
        accountService.insertAccount(account)
        transactionService.insertTransaction(transaction)

        HttpEntity entity = new HttpEntity<>(null, headers)

        when: "rest call is initiated"
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/delete/" + UUID.randomUUID().toString()), HttpMethod.DELETE,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.NOT_FOUND

        cleanup:
        transactionService.deleteByGuid(guid)
        accountService.deleteByAccountNameOwner(accountNameOwner)
    }

    def "test insertTransaction endpoint bad data"() {
        given:
        accountService.insertAccount(account)
        categoryService.insertCategory(category)
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>("foo", headers)

        when: "rest call is initiated"
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/insert/"), HttpMethod.POST,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.BAD_REQUEST

        cleanup:
        transactionService.deleteByGuid(guid)
        accountService.deleteByAccountNameOwner(accountNameOwner)
        categoryService.deleteByCategory(categoryName)
    }

    def "test insertTransaction endpoint"() {
        given:
        accountService.insertAccount(account)
        categoryService.insertCategory(category)
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(json, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/insert/"), HttpMethod.POST,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.OK

        cleanup:
        transactionService.deleteByGuid(guid)
        accountService.deleteByAccountNameOwner(accountNameOwner)
        categoryService.deleteByCategory(categoryName)
    }

    def "test updateTransaction endpoint"() {
        given:
        transactionService.insertTransaction(transaction)
        headers.setContentType(new MediaType("application", "json-patch+json"))
        HttpEntity entity = new HttpEntity<>(jsonUpdateDescription, headers)

        when:
        String response = restTemplate.patchForObject(
                createURLWithPort("/transaction/update/" + guid), entity, String.class)
        then:
        assert response == "transaction patched"

        cleanup:
        transactionService.deleteByGuid(guid)
        accountService.deleteByAccountNameOwner(accountNameOwner)
        categoryService.deleteByCategory(categoryName)
    }
}
