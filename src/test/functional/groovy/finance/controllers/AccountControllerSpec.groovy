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
    protected Account account = AccountBuilder.builder().withAccountNameOwner('unique_b').build()

    @Shared
    protected String jsonPayloadInvalidActiveStatus = '''
{"accountNameOwner":"test_b","accountType":"credit","activeStatus":"invalid","moniker":"1234","dateClosed":0}
'''
    @Shared
    protected String jsonPayloadInvalidTotals = '''
{"accountNameOwner":"test_b","accountType":"credit","activeStatus":true,"moniker":"1234","dateClosed":0}
'''
    @Shared
    protected String jsonPayloadMissingAccountType = '''
{"accountNameOwner":"test_b","activeStatus":true,"moniker":"1234","dateClosed":0}
'''
    @Shared
    protected String jsonPayloadEmptyAccountNameOwner = '''
{"accountNameOwner":"","accountType":"credit","activeStatus":true,"moniker":"1234","dateClosed":0}
'''
    @Shared
    protected String jsonPayloadInvalidAccountType = '''
{"accountNameOwner":"test_b","accountType":"invalid","activeStatus":true,"moniker":"1234","dateClosed":0}
'''

    @Shared
    protected String endpointName = 'account'

    void 'should successfully insert new account'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, account.toString())

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(account.accountNameOwner)
        0 * _
    }

    void 'should reject duplicate account insertion'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, account.toString())

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        0 * _
    }

    void 'should reject account insertion with empty account name owner'() {
        given:
        Account account = AccountBuilder.builder().withAccountNameOwner('').build()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, account.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should successfully insert and retrieve inactive account'() {
        given:
        Account account = AccountBuilder.builder()
                .withAccountNameOwner('nonactive_b')
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

    void 'should successfully find account by account name owner'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, account.accountNameOwner)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should return not found for non-existent account'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, UUID.randomUUID().toString())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should successfully delete account by account name owner'() {
        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, account.accountNameOwner)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should fail to delete account when referenced by payment transaction'() {
        given:
        String referencedByTransaction = 'referenced_b'
        String destinationAccountName = 'destination_b'
        // First create the accounts
        def accountPayload = AccountBuilder.builder().withAccountNameOwner(referencedByTransaction).build().toString()
        ResponseEntity<String> accountResponse = insertEndpoint(endpointName, accountPayload)
        def destinationAccountPayload = AccountBuilder.builder().withAccountNameOwner(destinationAccountName).build().toString()
        insertEndpoint(endpointName, destinationAccountPayload)

        // Create a payment that references this account
        String paymentPayload = """
{"accountNameOwner":"${referencedByTransaction}","sourceAccount":"${referencedByTransaction}","destinationAccount":"${destinationAccountName}","amount":25.00,"guidSource":"78f65481-f351-4142-aff6-73e99d2a286d","guidDestination":"0db56665-0d47-414e-93c5-e5ae4c5e4299","transactionDate":"2020-11-12"}
"""
        ResponseEntity<String> paymentResponse = insertEndpoint('payment', paymentPayload)

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, referencedByTransaction)

        then:
        accountResponse.statusCode == HttpStatus.OK
        paymentResponse.statusCode == HttpStatus.OK
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        0 * _
    }

    void 'should fail to rename account when target name already exists'() {
        given:
        String newName = 'existing_b'
        String oldName = 'source_b'
        // Create both accounts that are needed for this test
        def existingPayload = AccountBuilder.builder().withAccountNameOwner(newName).build().toString()
        ResponseEntity<String> existingResponse = insertEndpoint(endpointName, existingPayload)
        def sourcePayload = AccountBuilder.builder().withAccountNameOwner(oldName).build().toString()
        ResponseEntity<String> sourceResponse = insertEndpoint(endpointName, sourcePayload)

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("/api/account/rename?old=${oldName}&new=${newName}"),
                HttpMethod.PUT, entity, String)

        then:
        existingResponse.statusCode == HttpStatus.OK
        sourceResponse.statusCode == HttpStatus.OK
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        0 * _
    }

    void 'should successfully rename account name owner'() {
        given:
        String oldName = 'foo_b'
        String newName = 'new_b'
        def accountPayload = AccountBuilder.builder().withAccountNameOwner(oldName).build().toString()
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, accountPayload)
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("/api/account/rename?old=${oldName}&new=${newName}"),
                HttpMethod.PUT, entity, String)

        then:
        insertResponse.statusCode == HttpStatus.OK
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should return not found when deleting account with invalid identifiers'() {
        when:
        ResponseEntity<String> response1 = deleteEndpoint(endpointName, '1')
        ResponseEntity<String> response2 = deleteEndpoint(endpointName, 'adding/junk')

        then:
        response1.statusCode == HttpStatus.NOT_FOUND
        response2.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should reject account insertion with invalid payload'() {
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