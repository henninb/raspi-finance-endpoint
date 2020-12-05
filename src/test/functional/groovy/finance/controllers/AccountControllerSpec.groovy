package finance.controllers

import finance.Application
import finance.domain.Account
import finance.helpers.AccountBuilder
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    protected TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    protected HttpHeaders headers

    @Autowired
    protected TransactionService transactionService

    @Autowired
    protected AccountService accountService

    @Autowired
    protected CategoryService categoryService

    @Shared
    protected Account account

    void setup() {
        headers = new HttpHeaders()
        account = AccountBuilder.builder().build()
    }

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri
    }

    void 'test findAccount endpoint accountNameOwner found'() {
        given:
        account.accountNameOwner = 'found_test'
        accountService.insertAccount(account)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/account/select/" + account.accountNameOwner), HttpMethod.GET,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _

        cleanup:
        accountService.deleteByAccountNameOwner(account.accountNameOwner)
    }

    void 'test findAccount endpoint accountNameOwner not found'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/account/select/" + UUID.randomUUID().toString()), HttpMethod.GET,
                entity, String)
        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test deleteAccount endpoint'() {
        given:
        account.accountNameOwner = 'random_test'
        accountService.insertAccount(account)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/account/delete/" + account.accountNameOwner), HttpMethod.DELETE,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _

        cleanup:
        accountService.deleteByAccountNameOwner(account.accountNameOwner)
    }

    //TODO: build failed started to fail noticed on 11/8/2020
    void 'test insertAccount endpoint bad data'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>('accountBadData', headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/account/insert/'), HttpMethod.POST,
                entity, String)
        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    //TODO: build failed started to fail noticed on 11/8/2020
    void 'test insertAccount endpoint - irrelevant payload'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>('{"test":1}', headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/account/insert/'), HttpMethod.POST,
                entity, String)
        then:
        //def ex = thrown(JsonParseException)
        //ex.getMessage().contains('Unrecognized token')
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test findAccount endpoint empty accountNameOwner'() {
        given:
        account.accountNameOwner = ''
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(account, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/account/insert/'), HttpMethod.POST,
                entity, String)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }
}
