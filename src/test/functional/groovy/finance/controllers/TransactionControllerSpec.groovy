package finance.controllers

import finance.Application
import finance.domain.Account
import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import finance.helpers.AccountBuilder
import finance.services.AccountService
import finance.services.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
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

    //@Autowired
    //TransactionDAO transactionDAO

    @Shared
    Transaction transaction

    @Shared
    Account account

    @Shared
    String guid

    @Shared
    String accountNameOwner


    def setup() {

    }

    def setupSpec() {
        headers = new HttpHeaders()
        account = AccountBuilder.builder().build()
        transaction = TransactionBuilder.builder().build()
        guid = transaction.getGuid()
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
                createURLWithPort("/select/" + guid), HttpMethod.GET,
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
                createURLWithPort("/select/" + UUID.randomUUID().toString()), HttpMethod.GET,
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
                createURLWithPort("/delete/" + guid), HttpMethod.DELETE,
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
                createURLWithPort("/delete/" + UUID.randomUUID().toString()), HttpMethod.DELETE,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.NOT_FOUND

        cleanup:
        transactionService.deleteByGuid(guid)
        accountService.deleteByAccountNameOwner(accountNameOwner)
    }

}
