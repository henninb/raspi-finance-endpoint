package finance.controllers

import finance.Application
import finance.domain.Account
import finance.helpers.AccountBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerSpec extends BaseControllerSpec {

    @Shared
    protected Account account = AccountBuilder.builder().withAccountNameOwner('unique_brian').build()

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

    void 'test insert Account'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(account, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/account/insert/'), HttpMethod.POST,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    @Ignore('should duplicate Accounts return 200? probably not')
    void 'test insert Account - duplicate'() {
        given:
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

    void 'test insert Account - empty'() {
        given:
        Account account = AccountBuilder.builder().withAccountNameOwner('').build()
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

    void 'test insert Account - not active'() {
        given:
        Account account = AccountBuilder.builder()
                .withAccountNameOwner('non-active_brian')
                .withActiveStatus(false)
                .build()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(account, headers)
        restTemplate.exchange(
                createURLWithPort('/account/insert/'), HttpMethod.POST,
                entity, String)
        when:
        entity = new HttpEntity<>(null, headers)
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/account/select/${account.accountNameOwner}"), HttpMethod.GET,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test find Account found'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        entity = new HttpEntity<>(null, headers)
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/account/select/${account.accountNameOwner}"), HttpMethod.GET,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test find Account - not found'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/account/select/${UUID.randomUUID()}"), HttpMethod.GET,
                entity, String)
        then:
        response.statusCode.is(HttpStatus.NOT_FOUND)
        0 * _
    }

    void 'test delete Account'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/account/delete/${account.accountNameOwner}"), HttpMethod.DELETE,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test delete Account - referenced by a transaction from a payment'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)
        String referencedByTransaction = 'referenced_brian'

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/account/delete/${referencedByTransaction}"), HttpMethod.DELETE,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
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
        0 * _

        where:
        payload                          | httpStatus
        jsonPayloadMissingAccountType    | HttpStatus.BAD_REQUEST
        '{"test":1}'                     | HttpStatus.BAD_REQUEST
        'badJson'                        | HttpStatus.BAD_REQUEST
        '{malformedJson:"test"}'         | HttpStatus.BAD_REQUEST
        jsonPayloadInvalidActiveStatus   | HttpStatus.BAD_REQUEST
        jsonPayloadEmptyAccountNameOwner | HttpStatus.BAD_REQUEST
        jsonPayloadInvalidAccountType    | HttpStatus.BAD_REQUEST
        jsonPayloadInvalidTotals         | HttpStatus.BAD_REQUEST
    }
}
