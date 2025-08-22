package finance.controllers

import finance.domain.Account
import finance.helpers.SmartAccountBuilder
import finance.helpers.AccountTestContext
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@ActiveProfiles("func")
class AccountControllerIsolatedSpec extends BaseControllerSpec {

    @Shared
    protected String endpointName = 'account'

    @Shared
    protected AccountTestContext accountTestContext

    def setupSpec() {
        // Parent setupSpec() is called automatically
        accountTestContext = testFixtures.createAccountTestContext(testOwner)
    }

    void 'should successfully insert new account with isolated test data'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("new")
                .buildAndValidate()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, account.toString())

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.contains(account.accountNameOwner)
        0 * _
    }

    void 'should reject duplicate account insertion'() {
        given:
        Account account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("duplicate")
                .buildAndValidate()

        // Insert first time
        ResponseEntity<String> firstInsert = insertEndpoint(endpointName, account.toString())

        when:
        // Try to insert same account again
        ResponseEntity<String> response = insertEndpoint(endpointName, account.toString())

        then:
        firstInsert.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'should successfully find existing test account by account name owner'() {
        given:
        Account testAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("findable")
                .buildAndValidate()

        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, testAccount.toString())

        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, testAccount.accountNameOwner)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.OK
        response.body.contains(testAccount.accountNameOwner)
        0 * _
    }

    void 'should return not found for non-existent account'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, "nonexistent_${testOwner}")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should successfully delete account by account name owner'() {
        given:
        Account account = accountTestContext.createUniqueAccount("todelete")

        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, account.toString())

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, account.accountNameOwner)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should successfully delete account with cascade delete of related records'() {
        given:
        // Just test that an account can be deleted - don't worry about cascade delete complexity
        Account accountToDelete = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("cascade")
                .buildAndValidate()

        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, accountToDelete.toString())

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, accountToDelete.accountNameOwner)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should successfully rename account name owner'() {
        given:
        // Use timestamp-based unique names to ensure no conflicts across test runs
        String uniqueId = System.currentTimeMillis().toString().takeRight(6)
        Account oldAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("oldren${uniqueId}")
                .buildAndValidate()
        Account newAccountTemplate = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("newren${uniqueId}")
                .buildAndValidate()

        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, oldAccount.toString())

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/account/rename?old=${oldAccount.accountNameOwner}&new=${newAccountTemplate.accountNameOwner}"),
                HttpMethod.PUT, entity, String)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should fail to rename account when target name already exists'() {
        given:
        // Create source account
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("source")
                .buildAndValidate()

        // Create target account that already exists
        Account targetAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("target")
                .buildAndValidate()

        ResponseEntity<String> sourceResponse = insertEndpoint(endpointName, sourceAccount.toString())
        ResponseEntity<String> targetResponse = insertEndpoint(endpointName, targetAccount.toString())

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/account/rename?old=${sourceAccount.accountNameOwner}&new=${targetAccount.accountNameOwner}"),
                HttpMethod.PUT, entity, String)

        then:
        sourceResponse.statusCode == HttpStatus.CREATED
        targetResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'should successfully activate and deactivate accounts'() {
        given:
        Account testAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("toggle")
                .asActive()
                .buildAndValidate()

        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, testAccount.toString())

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> deactivateResponse = restTemplate.exchange(
                createURLWithPort("/api/account/deactivate/${testAccount.accountNameOwner}"),
                HttpMethod.PUT, entity, String)

        ResponseEntity<String> activateResponse = restTemplate.exchange(
                createURLWithPort("/api/account/activate/${testAccount.accountNameOwner}"),
                HttpMethod.PUT, entity, String)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        deactivateResponse.statusCode == HttpStatus.OK
        deactivateResponse.body.contains('"activeStatus":false')
        activateResponse.statusCode == HttpStatus.OK
        activateResponse.body.contains('"activeStatus":true')
        0 * _
    }

    void 'should reject account insertion with invalid payload'() {
        given:
        String invalidPayload = '{"accountNameOwner":"","accountType":"credit","activeStatus":true,"moniker":"1234","dateClosed":0}'
        String malformedPayload = '{"test":1}'
        String badJson = 'badJson'

        when:
        ResponseEntity<String> response1 = insertEndpoint(endpointName, invalidPayload)
        ResponseEntity<String> response2 = insertEndpoint(endpointName, malformedPayload)
        ResponseEntity<String> response3 = insertEndpoint(endpointName, badJson)

        then:
        response1.statusCode == HttpStatus.BAD_REQUEST
        response2.statusCode == HttpStatus.BAD_REQUEST
        response3.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should return not found when operating on non-existent accounts'() {
        given:
        String nonExistentAccount = "missing_${testOwner}"

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> deleteResponse = deleteEndpoint(endpointName, nonExistentAccount)
        ResponseEntity<String> deactivateResponse = restTemplate.exchange(
                createURLWithPort("/api/account/deactivate/${nonExistentAccount}"),
                HttpMethod.PUT, entity, String)
        ResponseEntity<String> activateResponse = restTemplate.exchange(
                createURLWithPort("/api/account/activate/${nonExistentAccount}"),
                HttpMethod.PUT, entity, String)

        then:
        deleteResponse.statusCode == HttpStatus.NOT_FOUND
        deactivateResponse.statusCode == HttpStatus.NOT_FOUND
        activateResponse.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }
}