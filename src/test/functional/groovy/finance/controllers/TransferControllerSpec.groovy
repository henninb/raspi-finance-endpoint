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
class TransferControllerSpec extends BaseControllerSpec {

    @Shared
    Long createdTransferId

    void "test select all transfers"() {
        when: "getting all transfers"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/transfer/select",
            HttpMethod.GET, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body != null

        and: "response should contain transfer list"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse instanceof List
    }

    void "test insert transfer successfully"() {
        given: "a valid transfer payload"
        String payload = '{"sourceAccount": "foo_brian", "destinationAccount": "bank_brian", "transactionDate": "2023-12-01", "amount": 150.75, "guidSource": "ba665bc2-22b6-4123-a566-6f5ab3d796d1", "guidDestination": "ba665bc2-22b6-4123-a566-6f5ab3d796d2"}'

        when: "posting to insert transfer endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/transfer/insert",
            HttpMethod.POST, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body != null

        and: "response should contain transfer data"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.sourceAccount == "foo_brian"
        jsonResponse.destinationAccount == "bank_brian"
        jsonResponse.amount == 150.75
        jsonResponse.transferId != null


        cleanup:
        def extractedTransferId = jsonResponse.transferId
        createdTransferId = extractedTransferId
    }

    void "test insert duplicate transfer should return conflict"() {
        given: "the same transfer payload as previous test"
        String payload = '{"sourceAccount": "foo_brian", "destinationAccount": "bank_brian", "transactionDate": "2023-12-01", "amount": 150.75, "guidSource": "ba665bc2-22b6-4123-a566-6f5ab3d796d1", "guidDestination": "ba665bc2-22b6-4123-a566-6f5ab3d796d2"}'

        when: "posting the same transfer again"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/transfer/insert",
            HttpMethod.POST, entity, String)

        then: "response should be HTTP 409 Conflict"
        response.statusCode == HttpStatus.CONFLICT
    }

    void "test insert transfer with invalid payload"() {
        given: "an invalid transfer payload"
        String payload = '{"invalidField": "invalid"}'

        when: "posting to insert transfer endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/transfer/insert",
            HttpMethod.POST, entity, String)

        then: "response should be bad request or internal server error"
        response.statusCode == HttpStatus.BAD_REQUEST || response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    void "test delete transfer by ID successfully"() {
        given: "a valid transfer ID"
        Long transferId = createdTransferId

        when: "deleting the transfer"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/transfer/delete/${transferId}",
            HttpMethod.DELETE, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body != null

        and: "response should contain deleted transfer data"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.transferId == transferId
    }

    void "test delete transfer with non-existent ID"() {
        given: "a non-existent transfer ID"
        Long nonExistentId = 999999L

        when: "deleting the non-existent transfer"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/transfer/delete/${nonExistentId}",
            HttpMethod.DELETE, entity, String)

        then: "response should be not found"
        response.statusCode == HttpStatus.NOT_FOUND
    }

    void "test unauthorized access to transfer endpoints"() {
        given: "no authentication token"

        when: "getting all transfers without token"
        HttpHeaders cleanHeaders = new HttpHeaders()
        cleanHeaders.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(null, cleanHeaders)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/transfer/select",
            HttpMethod.GET, entity, String)

        then: "response should be unauthorized"
        response.statusCode == HttpStatus.UNAUTHORIZED || response.statusCode == HttpStatus.FORBIDDEN
    }

    void "test insert transfer with missing required fields"() {
        given: "a transfer payload with missing required fields"
        String payload = '{"sourceAccount": "foo_brian", "transactionDate": "2023-12-01"}'

        when: "posting to insert transfer endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/transfer/insert",
            HttpMethod.POST, entity, String)

        then: "response should be bad request or internal server error"
        response.statusCode == HttpStatus.BAD_REQUEST || response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}