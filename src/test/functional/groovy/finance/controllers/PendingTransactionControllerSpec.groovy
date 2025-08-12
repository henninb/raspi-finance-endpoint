package finance.controllers

import finance.Application
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Stepwise

@Slf4j
@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
class PendingTransactionControllerSpec extends BaseControllerSpec {

    @Shared
    Long createdPendingTransactionId

    void "test insert pending transaction successfully"() {
        given: "a valid pending transaction payload"
        String payload = '{"accountNameOwner": "foo_brian", "transactionDate": "2025-01-01", "description": "functional test pending transaction", "amount": 75.25}'

        when: "posting to insert pending transaction endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/insert",
            HttpMethod.POST, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body != null

        and: "response should contain pending transaction data"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.accountNameOwner == "foo_brian"
        jsonResponse.description == "functional test pending transaction"
        jsonResponse.amount == 75.25
        jsonResponse.pendingTransactionId != null
        
        
        cleanup:
        def extractedId = jsonResponse.pendingTransactionId
        createdPendingTransactionId = extractedId
    }

    void "test insert pending transaction with invalid payload"() {
        given: "an invalid pending transaction payload"
        String payload = '{"invalidField": "invalid"}'

        when: "posting to insert pending transaction endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/insert",
            HttpMethod.POST, entity, String)

        then: "response should be bad request"
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    void "test get all pending transactions successfully"() {
        when: "getting all pending transactions"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/all",
            HttpMethod.GET, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body != null

        and: "response should contain pending transactions list"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse instanceof List
        jsonResponse.size() >= 1
    }

    void "test delete pending transaction by ID successfully"() {
        given: "a valid pending transaction ID"
        Long pendingTransactionId = createdPendingTransactionId

        when: "deleting the pending transaction"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/delete/${pendingTransactionId}",
            HttpMethod.DELETE, entity, String)

        then: "response should be successful with no content"
        response.statusCode == HttpStatus.NO_CONTENT
    }

    void "test delete pending transaction with non-existent ID"() {
        given: "a non-existent pending transaction ID"
        Long nonExistentId = 999999L

        when: "deleting the non-existent pending transaction"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/delete/${nonExistentId}",
            HttpMethod.DELETE, entity, String)

        then: "response should be internal server error"
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    void "test get all pending transactions when none exist"() {
        given: "all pending transactions are deleted"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        // First delete all pending transactions
        restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/delete/all",
            HttpMethod.DELETE, entity, String)

        when: "getting all pending transactions"
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/all",
            HttpMethod.GET, entity, String)

        then: "response should be not found"
        response.statusCode == HttpStatus.NOT_FOUND
    }

    void "test delete all pending transactions successfully"() {
        given: "some pending transactions exist"
        String payload = '{"accountNameOwner": "foo_brian", "transactionDate": "2025-01-02", "description": "test transaction for delete all", "amount": 25.00}'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(payload, headers)

        // Insert a pending transaction first
        restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/insert",
            HttpMethod.POST, entity, String)

        when: "deleting all pending transactions"
        HttpEntity deleteEntity = new HttpEntity<>(null, headers)
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/delete/all",
            HttpMethod.DELETE, deleteEntity, String)

        then: "response should be successful with no content"
        response.statusCode == HttpStatus.NO_CONTENT
    }

    void "test unauthorized access to pending transaction endpoints"() {
        given: "no authentication token"
        String payload = '{"accountNameOwner": "foo_brian", "transactionDate": "2025-01-03", "description": "test", "amount": 50.00}'

        when: "posting to insert pending transaction endpoint without token"
        HttpHeaders noAuthHeaders = new HttpHeaders()
        noAuthHeaders.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, noAuthHeaders)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/insert",
            HttpMethod.POST, entity, String)

        then: "response should be successful (endpoint is not secured)"
        response.statusCode == HttpStatus.OK
    }

    void "test insert pending transaction with missing required fields"() {
        given: "a pending transaction payload with missing required fields"
        String payload = '{"description": "test transaction without account"}'

        when: "posting to insert pending transaction endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/pending/transaction/insert",
            HttpMethod.POST, entity, String)

        then: "response should be bad request"
        response.statusCode == HttpStatus.BAD_REQUEST
    }
}