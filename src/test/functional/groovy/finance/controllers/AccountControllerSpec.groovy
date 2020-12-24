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
import spock.lang.Unroll

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

    @Shared
    protected String jsonPayloadInvalidActiveStatus = '''
{"accountNameOwner":"test_brian","accountType":"credit","activeStatus":"invalid","moniker":"1234","totals":0.01,"totalsBalanced":0.02,"dateClosed":0}
'''
    @Shared
    protected String jsonPayloadInvalidTotals = '''
{"accountNameOwner":"test_brian","accountType":"credit","activeStatus":true,"moniker":"1234","totals":0.0155,"totalsBalanced":0.02,"dateClosed":0}
'''
    @Shared
    protected String jsonPayloadMissingAccountType = '''
{"accountNameOwner":"test_brian","activeStatus":true,"moniker":"1234","totals":0.01,"totalsBalanced":0.02,"dateClosed":0}
'''
    @Shared
    protected String jsonPayloadEmptyAccountNameOwner = '''
{"accountNameOwner":"","accountType":"credit","activeStatus":true,"moniker":"1234","totals":0.01,"totalsBalanced":0.02,"dateClosed":0}
'''
    @Shared
    protected String jsonPayloadInvalidAccountType = '''
{"accountNameOwner":"test_brian","accountType":"invalid","activeStatus":true,"moniker":"1234","totals":0.01,"totalsBalanced":0.02,"dateClosed":0}
'''

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
        response.statusCode.is(HttpStatus.NOT_FOUND)
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

    @Unroll
    void 'test deleteAccount endpoint - failure for irregular payload'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/account/delete/${accountNameOwner}"), HttpMethod.DELETE,
                entity, String)

        then:
        response.statusCode.is(httpStatus)
        response.body.contains(responseBody)
        0 * _

        where:
        accountNameOwner | httpStatus           | responseBody
        '1'              | HttpStatus.NOT_FOUND | 'could not delete this account'
        null             | HttpStatus.NOT_FOUND | 'could not delete this account'
        'adding/junk'    | HttpStatus.NOT_FOUND | 'Not Found'
    }

    @Unroll
    void 'test insertAccount endpoint - failure for irregular payload'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/account/insert/'), HttpMethod.POST,
                entity, String)
        then:
        response.statusCode.is(httpStatus)
        response.body.contains(responseBody)
        0 * _

        where:
        payload                          | httpStatus             | responseBody
        jsonPayloadMissingAccountType    | HttpStatus.BAD_REQUEST | 'value failed for JSON property accountType due to missing'
        '{"test":1}'                     | HttpStatus.BAD_REQUEST | 'value failed for JSON property accountNameOwner due to missing'
        'badJson'                        | HttpStatus.BAD_REQUEST | 'Unrecognized token'
        '{malformedJson:"test"}'         | HttpStatus.BAD_REQUEST | 'was expecting double-quote to start field'
        jsonPayloadInvalidActiveStatus   | HttpStatus.BAD_REQUEST | 'Cannot deserialize value of type'
        jsonPayloadEmptyAccountNameOwner | HttpStatus.BAD_REQUEST | 'Cannot insert account as there is a constraint violation on the data'
        jsonPayloadInvalidAccountType    | HttpStatus.BAD_REQUEST | 'Cannot deserialize value of type `finance.domain.AccountType'
        jsonPayloadInvalidTotals         | HttpStatus.BAD_REQUEST | 'Cannot insert account as there is a constraint violation on the data.'

    }
}
