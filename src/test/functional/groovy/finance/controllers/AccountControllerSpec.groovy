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
@ActiveProfiles("func")
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
        response.statusCode == HttpStatus.CREATED
        response.body.contains(account.accountNameOwner)
        0 * _
    }

    void 'should reject duplicate account insertion'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, account.toString())

        then:
        response.statusCode == HttpStatus.CONFLICT
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
        insertResponse.statusCode == HttpStatus.CREATED
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

    void 'should successfully delete account with cascade delete of related records'() {
        given:
        String referencedByTransaction = 'referenced_brian'  // Use existing account from data.sql
        
        // Create a payment that references existing transactions from data.sql
        String paymentPayload = """
{"accountNameOwner":"${referencedByTransaction}","sourceAccount":"${referencedByTransaction}","destinationAccount":"bank_brian","amount":50.00,"guidSource":"ba665bc2-22b6-4123-a566-6f5ab3d796dh","guidDestination":"ba665bc2-22b6-4123-a566-6f5ab3d796di","transactionDate":"2020-11-13"}
"""
        ResponseEntity<String> paymentResponse = insertEndpoint('payment', paymentPayload)

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, referencedByTransaction)

        then:
        paymentResponse.statusCode == HttpStatus.OK
        response.statusCode == HttpStatus.OK  // Account deletion succeeds due to cascade delete
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
        existingResponse.statusCode == HttpStatus.CREATED
        sourceResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.CONFLICT
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
        insertResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should return not found when deleting account with non-existent valid identifiers'() {
        when:
        ResponseEntity<String> response1 = deleteEndpoint(endpointName, 'non_existent_account')
        ResponseEntity<String> response2 = deleteEndpoint(endpointName, 'another_missing_account')

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

    void 'should successfully deactivate an active account'() {
        given:
        Account testAccount = AccountBuilder.builder()
                .withAccountNameOwner('test_deactivate')
                .withActiveStatus(true)
                .build()
        
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, testAccount.toString())
        
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> deactivateResponse = restTemplate.exchange(
                createURLWithPort("/api/account/deactivate/${testAccount.accountNameOwner}"),
                HttpMethod.PUT, entity, String)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        deactivateResponse.statusCode == HttpStatus.OK
        deactivateResponse.body.contains('"activeStatus":false')
        0 * _
    }

    void 'should successfully activate an inactive account'() {
        given:
        Account testAccount = AccountBuilder.builder()
                .withAccountNameOwner('test_activate')
                .withActiveStatus(false)
                .build()
        
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, testAccount.toString())
        
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> activateResponse = restTemplate.exchange(
                createURLWithPort("/api/account/activate/${testAccount.accountNameOwner}"),
                HttpMethod.PUT, entity, String)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        activateResponse.statusCode == HttpStatus.OK
        activateResponse.body.contains('"activeStatus":true')
        0 * _
    }

    void 'should return not found when trying to deactivate non-existent account'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/account/deactivate/non_existent_account"),
                HttpMethod.PUT, entity, String)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should return not found when trying to activate non-existent account'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/account/activate/non_existent_account"),
                HttpMethod.PUT, entity, String)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }
}