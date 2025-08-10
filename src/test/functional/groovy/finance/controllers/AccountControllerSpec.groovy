package finance.controllers

import finance.domain.Account
import finance.helpers.AccountBuilder
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Stepwise
@ActiveProfiles("int")
class AccountControllerSpec extends BaseControllerSpec {

    @Shared
    protected Account account = AccountBuilder.builder().withAccountNameOwner('unique_brian').build()

    @Shared
    protected String jsonPayloadInvalidActiveStatus = '''
{"accountNameOwner":"test_brian","accountType":"credit","activeStatus":"invalid","moniker":"1234","dateClosed":0}
'''
    @Shared
    protected String jsonPayloadInvalidTotals = '''
{"accountNameOwner":"test_brian","accountType":"credit","activeStatus":true,"moniker":"1234","dateClosed":0}
'''
    @Shared
    protected String jsonPayloadMissingAccountType = '''
{"accountNameOwner":"test_brian","activeStatus":true,"moniker":"1234","dateClosed":0}
'''
    @Shared
    protected String jsonPayloadEmptyAccountNameOwner = '''
{"accountNameOwner":"","accountType":"credit","activeStatus":true,"moniker":"1234","dateClosed":0}
'''
    @Shared
    protected String jsonPayloadInvalidAccountType = '''
{"accountNameOwner":"test_brian","accountType":"invalid","activeStatus":true,"moniker":"1234","dateClosed":0}
'''

    @Shared
    protected String endpointName = 'account'

    void 'test insert Account'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, account.toString())

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(account.accountNameOwner)
        0 * _
    }

    void 'test insert Account - duplicate'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, account.toString())

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        0 * _
    }

    void 'test insert Account - empty'() {
        given:
        Account account = AccountBuilder.builder().withAccountNameOwner('').build()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, account.toString())

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

        when:
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, account.toString())

        then:
        insertResponse.statusCode == HttpStatus.OK
        0 * _

        when:
        ResponseEntity<String> selectResponse = selectEndpoint(endpointName, account.accountNameOwner)

        then:
        selectResponse.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test find Account found'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, account.accountNameOwner)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test find Account - not found'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, UUID.randomUUID().toString())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test delete Account'() {
        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, account.accountNameOwner)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test delete Account - referenced by a transaction from a payment'() {
        given:
        String referencedByTransaction = 'referenced_brian'
        // First create the account
        insertEndpoint(endpointName, '{\"accountNameOwner\":\"referenced_brian\",\"accountType\":\"credit\",\"activeStatus\":true,\"moniker\":\"0000\"}')
        // Create a payment that references this account
        insertEndpoint('payment', '{\"accountNameOwner\":\"referenced_brian\",\"amount\":25.00,\"guidSource\":\"78f65481-f351-4142-aff6-73e99d2a286d\",\"guidDestination\":\"0db56665-0d47-414e-93c5-e5ae4c5e4299\",\"transactionDate\":\"2020-11-12\"}')

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, referencedByTransaction)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test rename AccountNameOwner - existing new account'() {
        given:
        String newName = 'foo_brian'
        String oldName = 'new_brian'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("/api/account/rename?old=${oldName}&new=${newName}"),
                HttpMethod.PUT, entity, String)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test rename AccountNameOwner'() {
        given:
        String oldName = 'foo_brian'
        String newName = 'new_brian'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("/api/account/rename?old=${oldName}&new=${newName}"),
                HttpMethod.PUT, entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test deleteAccount endpoint - failure for irregular payload'() {
        when:
        ResponseEntity<String> response1 = deleteEndpoint(endpointName, '1')
        ResponseEntity<String> response2 = deleteEndpoint(endpointName, 'adding/junk')

        then:
        response1.statusCode == HttpStatus.NOT_FOUND
        response2.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test insertAccount endpoint - failure for irregular payload'() {
        when:
        ResponseEntity<String> response1 = insertEndpoint(endpointName, jsonPayloadMissingAccountType)
        ResponseEntity<String> response2 = insertEndpoint(endpointName, '{"test":1}')
        ResponseEntity<String> response3 = insertEndpoint(endpointName, 'badJson')

        then:
        response1.statusCode == HttpStatus.BAD_REQUEST
        response2.statusCode == HttpStatus.BAD_REQUEST
        response3.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }
}
