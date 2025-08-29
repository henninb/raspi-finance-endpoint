package finance.controllers

import finance.domain.PendingTransaction
import finance.helpers.SmartPendingTransactionBuilder
import finance.helpers.SmartAccountBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import java.util.UUID

@Slf4j
@ActiveProfiles("func")
class PendingTransactionControllerIsolatedSpec extends BaseControllerSpec {

    @Shared String endpointName = 'pending/transaction'

    def setupSpec() {
        log.info("Setting up PendingTransactionController isolated tests for owner: ${testOwner}")
    }

    def cleanupSpec() {
        log.info("Cleaning up PendingTransactionController isolated test data for owner: ${testOwner}")
        // PendingTransaction doesn't have FK constraints - simple cleanup if needed
    }

    void 'should successfully insert new pending transaction with isolated test data'() {
        given: 'a valid pending transaction with proper account setup'
        // First create account using SmartAccountBuilder (same pattern as ValidationAmountController)
        def account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('testacct')
            .asDebit()
            .buildAndValidate()

        ResponseEntity<String> accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Create pending transaction using SmartPendingTransactionBuilder with same account name
        PendingTransaction pendingTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(account.accountNameOwner)
            .withUniqueDescription('func-test-pending')
            .withAmount('75.25')
            .asPending()
            .buildAndValidate()

        when: 'posting to insert pending transaction endpoint'
        ResponseEntity<String> response = insertEndpoint(endpointName, pendingTransaction.toString())

        then: 'response should be successful'
        response.statusCode == HttpStatus.OK
        response.body != null

        and: 'response should contain pending transaction data with unique test values'
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.accountNameOwner == pendingTransaction.accountNameOwner
        jsonResponse.description == pendingTransaction.description
        jsonResponse.amount == 75.25
        jsonResponse.pendingTransactionId != null

        0 * _
    }

    void 'should reject pending transaction insertion with invalid payload'() {
        given: 'an invalid pending transaction payload'
        String invalidPayload = '{"invalidField": "invalid"}'

        when: 'posting invalid payload to insert endpoint'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(invalidPayload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/insert",
            HttpMethod.POST, entity, String)

        then: 'response should be bad request'
        response.statusCode == HttpStatus.BAD_REQUEST

        0 * _
    }

    void 'should reject pending transaction with missing required fields'() {
        given: 'a pending transaction payload missing required fields'
        String incompletePayload = '{"description": "test transaction without account"}'

        when: 'posting incomplete payload to insert endpoint'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(incompletePayload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/insert",
            HttpMethod.POST, entity, String)

        then: 'response should be bad request'
        response.statusCode == HttpStatus.BAD_REQUEST

        0 * _
    }

    void 'should reject pending transaction with invalid account name pattern'() {
        when: 'creating pending transaction with invalid account name pattern using SmartBuilder'
        SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner('invalid-account-name-no-underscore')  // Invalid pattern
            .withUniqueDescription('test-invalid-pattern')
            .withAmount('50.00')
            .buildAndValidate()

        then: 'constraint validation should fail'
        IllegalStateException ex = thrown()
        ex.message.contains('must match alpha_underscore pattern')

        0 * _
    }

    void 'should successfully retrieve all pending transactions'() {
        given: 'at least one pending transaction exists'
        // Create account first using SmartAccountBuilder
        def account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('retrieve')
            .asDebit()
            .buildAndValidate()

        insertEndpoint('account', account.toString())

        // Create pending transaction using SmartPendingTransactionBuilder
        PendingTransaction pendingTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(account.accountNameOwner)
            .withUniqueDescription('test-retrieve-all')
            .withAmount('100.00')
            .buildAndValidate()

        // Insert the test data first
        insertEndpoint(endpointName, pendingTransaction.toString())

        when: 'getting all pending transactions'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/all",
            HttpMethod.GET, entity, String)

        then: 'response should be successful'
        response.statusCode == HttpStatus.OK
        response.body != null

        and: 'response should contain pending transactions list'
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse instanceof List
        jsonResponse.size() >= 1

        0 * _
    }

    void 'should successfully delete pending transaction by ID'() {
        given: 'a valid pending transaction to delete'
        // Create account first using SmartAccountBuilder
        def account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('delete')
            .asDebit()
            .buildAndValidate()

        insertEndpoint('account', account.toString())

        // Create pending transaction using SmartPendingTransactionBuilder
        PendingTransaction pendingTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(account.accountNameOwner)
            .withUniqueDescription('test-delete-by-id')
            .withAmount('150.75')
            .buildAndValidate()

        // Insert and extract ID
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, pendingTransaction.toString())
        JsonSlurper jsonSlurper = new JsonSlurper()
        def insertedData = jsonSlurper.parseText(insertResponse.body)
        Long pendingTransactionId = insertedData.pendingTransactionId

        when: 'deleting the pending transaction by ID'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/delete/${pendingTransactionId}",
            HttpMethod.DELETE, entity, String)

        then: 'response should be successful with no content'
        response.statusCode == HttpStatus.NO_CONTENT

        0 * _
    }

    void 'should handle deletion of non-existent pending transaction ID gracefully'() {
        given: 'a non-existent pending transaction ID'
        Long nonExistentId = 999999L

        when: 'deleting the non-existent pending transaction'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/delete/${nonExistentId}",
            HttpMethod.DELETE, entity, String)

        then: 'response should be internal server error'
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR

        0 * _
    }

    void 'should successfully delete all pending transactions'() {
        given: 'some pending transactions exist'
        // Create accounts first using SmartAccountBuilder
        def account1 = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('deleteall1')
            .asDebit()
            .buildAndValidate()

        def account2 = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('deleteall2')
            .asDebit()
            .buildAndValidate()

        insertEndpoint('account', account1.toString())
        insertEndpoint('account', account2.toString())

        // Create pending transactions using SmartPendingTransactionBuilder
        PendingTransaction pt1 = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(account1.accountNameOwner)
            .withUniqueDescription('test-delete-all-1')
            .withAmount('25.00')
            .buildAndValidate()

        PendingTransaction pt2 = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(account2.accountNameOwner)
            .withUniqueDescription('test-delete-all-2')
            .withAmount('35.50')
            .buildAndValidate()

        // Insert test data
        insertEndpoint(endpointName, pt1.toString())
        insertEndpoint(endpointName, pt2.toString())

        when: 'deleting all pending transactions'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/delete/all",
            HttpMethod.DELETE, entity, String)

        then: 'response should be successful with no content'
        response.statusCode == HttpStatus.NO_CONTENT

        0 * _
    }

    void 'should return not found when getting all pending transactions after deletion'() {
        given: 'all pending transactions are deleted'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        // First delete all pending transactions
        restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/delete/all",
            HttpMethod.DELETE, entity, String)

        when: 'getting all pending transactions after deletion'
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/all",
            HttpMethod.GET, entity, String)

        then: 'response should be not found'
        response.statusCode == HttpStatus.NOT_FOUND

        0 * _
    }

    void 'should allow unauthorized access to pending transaction endpoints'() {
        given: 'a valid pending transaction without authentication'
        // Create account first using SmartAccountBuilder
        def account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName('noauth')
            .asDebit()
            .buildAndValidate()

        insertEndpoint('account', account.toString())

        // Create pending transaction using SmartPendingTransactionBuilder
        PendingTransaction pendingTransaction = SmartPendingTransactionBuilder.builderForOwner(testOwner)
            .withAccountNameOwner(account.accountNameOwner)
            .withUniqueDescription('test-no-auth')
            .withAmount('50.00')
            .buildAndValidate()

        when: 'posting to insert pending transaction endpoint without token'
        HttpHeaders noAuthHeaders = new HttpHeaders()
        noAuthHeaders.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(pendingTransaction.toString(), noAuthHeaders)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/insert",
            HttpMethod.POST, entity, String)

        then: 'response should be successful (endpoint is not secured)'
        response.statusCode == HttpStatus.OK

        0 * _
    }
}