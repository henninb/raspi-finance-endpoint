package finance.controllers

import finance.Application
import finance.domain.ValidationAmount
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
import spock.lang.Stepwise

@Slf4j
@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
class ValidationAmountControllerSpec extends BaseControllerSpec {

    void "test insert validation amount successfully"() {
        given: "a valid validation amount payload"
        def payloadMap = [
            validationDate  : "2024-01-01T00:00:00.000+00:00",
            amount          : 100.50,
            activeStatus    : true,
            transactionState: "cleared"
        ]
        String payload = new groovy.json.JsonOutput().toJson(payloadMap)
        String accountNameOwner = "foo_brian"

        when: "posting to insert validation amount endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/validation/amount/insert/${accountNameOwner}",
            HttpMethod.POST, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body != null

        and: "response should contain validation amount data"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.validationId != 0
        // The account 'foo_brian' is created by src/test/functional/resources/data.sql
        // and is expected to have account_id = 1
        jsonResponse.accountId == 1
        jsonResponse.amount == 100.50
        jsonResponse.transactionState == "cleared"
        jsonResponse.activeStatus == true
    }

    void "test insert validation amount with invalid payload"() {
        given: "an invalid validation amount payload"
        String payload = '{"invalidField": "invalid"}'
        String accountNameOwner = "foo_brian"

        when: "posting to insert validation amount endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/validation/amount/insert/${accountNameOwner}",
            HttpMethod.POST, entity, String)

        then: "response should be bad request"
        response.statusCode == HttpStatus.BAD_REQUEST || response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    void "test select validation amount by account name owner and transaction state"() {
        given: "an account name owner and transaction state"
        String accountNameOwner = "foo_brian"
        String transactionState = "cleared"

        when: "getting validation amount"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/validation/amount/select/${accountNameOwner}/${transactionState}",
            HttpMethod.GET, entity, String)

        then: "response should be successful or not found"
        response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND
    }

    void "test select validation amount with invalid transaction state"() {
        given: "an account name owner and invalid transaction state"
        String accountNameOwner = "foo_brian"
        String transactionState = "invalid_state"

        when: "getting validation amount"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/validation/amount/select/${accountNameOwner}/${transactionState}",
            HttpMethod.GET, entity, String)

        then: "response should be bad request or internal server error"
        response.statusCode == HttpStatus.BAD_REQUEST || response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    void "test unauthorized access to validation amount endpoints"() {
        given: "no authentication token"
        String payload = '{"validationId": 0, "accountId": 0, "validationDate": "2024-01-01T00:00:00.000+00:00", "amount": 100.50, "activeStatus": true, "transactionState": "cleared"}'
        String accountNameOwner = "foo_brian"

        when: "posting to insert validation amount endpoint without token"
        HttpHeaders cleanHeaders = new HttpHeaders()
        cleanHeaders.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, cleanHeaders)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/validation/amount/insert/${accountNameOwner}",
            HttpMethod.POST, entity, String)

        then: "response should be unauthorized"
        response.statusCode == HttpStatus.UNAUTHORIZED || response.statusCode == HttpStatus.FORBIDDEN
    }
}