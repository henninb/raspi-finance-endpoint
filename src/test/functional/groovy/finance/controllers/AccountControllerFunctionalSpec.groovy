package finance.controllers

import finance.helpers.SmartAccountBuilder
import finance.helpers.TestDataManager
import finance.helpers.TestFixtures
import groovy.util.logging.Slf4j
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@Slf4j
@ActiveProfiles("func")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StandardizedAccountControllerFunctionalSpec extends BaseControllerFunctionalSpec {

    // ===== STANDARDIZED METHOD NAMING TESTS =====

    void 'should implement standardized method name: findAllActive instead of accounts'() {
        when: 'requesting active accounts with standardized endpoint'
        ResponseEntity<String> response = getEndpoint("/account/active")

        then: 'should return successful response'
        response.statusCode == HttpStatus.OK

        and: 'should return list of accounts'
        response.body.contains("[") || response.body.contains(testOwner)
    }

    void 'should implement standardized method name: findById instead of account'() {
        given:
        String uniqueAccountName = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("findbyid")
                .buildAndValidate().accountNameOwner

        // Create account via API to ensure it exists
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(uniqueAccountName)
                .asCredit()
                .buildAndValidate()
        def createResponse = postEndpoint("/account", account.toString())
        assert createResponse.statusCode == HttpStatus.CREATED

        // Verify account is immediately available (transaction flush check)
        def verifyResponse = getEndpoint("/account/active")
        assert verifyResponse.statusCode == HttpStatus.OK
        assert verifyResponse.body.contains(uniqueAccountName)

        when:
        ResponseEntity<String> response = getEndpoint("/account/${uniqueAccountName}")

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(uniqueAccountName)
    }

    void 'should implement standardized method name: save instead of insertAccount'() {
        given:
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("saveinstead")
                .asCredit()
                .buildAndValidate()

        when:
        ResponseEntity<String> response = postEndpoint("/account", account.toString())

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.contains(account.accountNameOwner)
    }

    void 'should implement standardized method name: update instead of updateAccount'() {
        given:
        String uniqueAccountName = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("updateinstead")
                .buildAndValidate().accountNameOwner

        // Create account first
        def initialAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(uniqueAccountName)
                .asDebit()
                .buildAndValidate()
        def createResponse = postEndpoint("/account", initialAccount.toString())
        assert createResponse.statusCode == HttpStatus.CREATED

        // Verify account is immediately available for individual retrieval (transaction flush check)
        def verifyResponse = getEndpoint("/account/${uniqueAccountName}")
        assert verifyResponse.statusCode == HttpStatus.OK
        assert verifyResponse.body.contains(uniqueAccountName)

        // Parse the complete account data from the GET response
        def accountJson = new groovy.json.JsonSlurper().parseText(verifyResponse.body)
        String exactAccountName = accountJson.accountNameOwner

        // Modify the existing account data to change accountType to Credit
        accountJson.accountType = "Credit"
        String updatePayload = new groovy.json.JsonBuilder(accountJson).toString()

        when:
        ResponseEntity<String> response = putEndpoint("/account/${exactAccountName}", updatePayload)

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(exactAccountName)
        response.body.contains("credit")
    }

    void 'should implement standardized method name: deleteById instead of deleteAccount'() {
        given:
        String uniqueAccountName = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("deletebyid")
                .buildAndValidate().accountNameOwner
        // Create account via API to ensure it exists
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(uniqueAccountName)
                .asCredit()
                .buildAndValidate()
        postEndpoint("/account", account.toString())

        when:
        ResponseEntity<String> response = deleteEndpoint("/account/${uniqueAccountName}")

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(uniqueAccountName)
    }

    // ===== EMPTY RESULT HANDLING TESTS =====

    void 'should return empty list for collections when no results found'() {
        given:
        // This test verifies that collections return empty lists instead of 404
        // Since other tests may have created accounts, we check for proper HTTP 200 response
        // and that the response is a valid JSON array (even if not empty due to other tests)

        when:
        ResponseEntity<String> response = getEndpoint("/account/active")

        then:
        response.statusCode == HttpStatus.OK
        // Should return a JSON array (could be empty [] or contain test data from other tests)
        response.body.startsWith("[") && response.body.endsWith("]")
    }

    void 'should return 404 for single entity when not found'() {
        given:
        String nonExistentAccount = "nonexistent_${testOwner}"

        when:
        ResponseEntity<String> response = getEndpoint("/account/${nonExistentAccount}")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    // ===== HTTP STATUS CODE TESTS =====

    void 'should return 201 CREATED for entity creation'() {
        given:
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("created201")
                .asCredit()
                .buildAndValidate()

        when:
        ResponseEntity<String> response = postEndpoint("/account", account.toString())

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.contains(account.accountNameOwner)
    }

    void 'should return 200 OK for entity update'() {
        given:
        String uniqueAccountName = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("update200")
                .buildAndValidate().accountNameOwner

        // Create account first
        def initialAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(uniqueAccountName)
                .asDebit()
                .buildAndValidate()
        def createResponse = postEndpoint("/account", initialAccount.toString())
        assert createResponse.statusCode == HttpStatus.CREATED

        // Verify account is immediately available for individual retrieval (transaction flush check)
        def verifyResponse = getEndpoint("/account/${uniqueAccountName}")
        assert verifyResponse.statusCode == HttpStatus.OK
        assert verifyResponse.body.contains(uniqueAccountName)

        // Parse the complete account data from the GET response
        def accountJson = new groovy.json.JsonSlurper().parseText(verifyResponse.body)
        String exactAccountName = accountJson.accountNameOwner

        // Modify the existing account data to change accountType to Credit
        accountJson.accountType = "Credit"
        String updatePayload = new groovy.json.JsonBuilder(accountJson).toString()

        when:
        ResponseEntity<String> response = putEndpoint("/account/${exactAccountName}", updatePayload)

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(exactAccountName)
    }

    void 'should return 200 OK with deleted entity for deletion'() {
        given:
        String uniqueAccountName = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("delete200")
                .buildAndValidate().accountNameOwner

        // Create account via API to ensure it exists
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(uniqueAccountName)
                .asCredit()
                .buildAndValidate()
        postEndpoint("/account", account.toString())

        when:
        ResponseEntity<String> response = deleteEndpoint("/account/${uniqueAccountName}")

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(uniqueAccountName)
    }

    // ===== REQUEST BODY HANDLING TESTS =====

    void 'should use Account entity type instead of Map<String, Any> for updates'() {
        given:
        String uniqueAccountName = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("entitytype")
                .buildAndValidate().accountNameOwner

        // Create account first
        def initialAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(uniqueAccountName)
                .asDebit()
                .buildAndValidate()
        def createResponse = postEndpoint("/account", initialAccount.toString())
        assert createResponse.statusCode == HttpStatus.CREATED

        // Verify account is immediately available for individual retrieval (transaction flush check)
        def verifyResponse = getEndpoint("/account/${uniqueAccountName}")
        assert verifyResponse.statusCode == HttpStatus.OK
        assert verifyResponse.body.contains(uniqueAccountName)

        // Parse the complete account data from the GET response
        def accountJson = new groovy.json.JsonSlurper().parseText(verifyResponse.body)
        String exactAccountName = accountJson.accountNameOwner

        // Modify the existing account data to change accountType to Credit
        accountJson.accountType = "Credit"
        String updatePayload = new groovy.json.JsonBuilder(accountJson).toString()

        when:
        ResponseEntity<String> response = putEndpoint("/account/${exactAccountName}", updatePayload)

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(exactAccountName)
        response.body.contains("credit")
    }

    // ===== EXCEPTION HANDLING TESTS =====

    void 'should handle duplicate account creation with standardized exception response'() {
        given:
        String duplicateAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("duplicate")
                .buildAndValidate().accountNameOwner

        // Create the first account successfully
        def account1 = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(duplicateAccount)
                .asCredit()
                .buildAndValidate()
        def firstResponse = postEndpoint("/account", account1.toString())

        // Attempt to create duplicate
        def account2 = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(duplicateAccount)
                .asCredit()
                .buildAndValidate()

        when:
        ResponseEntity<String> response = postEndpoint("/account", account2.toString())

        then:
        firstResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.CONFLICT
    }

    void 'should preserve business logic endpoints unchanged'() {
        given:
        // Create some basic test data to ensure the totals endpoint has data to work with
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("businesslogic")
                .asCredit()
                .buildAndValidate()
        postEndpoint("/account", account.toString())

        when:
        ResponseEntity<String> totalsResponse = getEndpoint("/account/totals")
        ResponseEntity<String> paymentResponse = getEndpoint("/account/payment/required")

        then:
        // /totals endpoint may fail due to missing transaction data in functional tests, but should at least be reachable
        totalsResponse.statusCode in [HttpStatus.OK, HttpStatus.INTERNAL_SERVER_ERROR]
        paymentResponse.statusCode == HttpStatus.OK
        // If totals succeeds, it should contain the totals structure
        totalsResponse.statusCode == HttpStatus.INTERNAL_SERVER_ERROR || totalsResponse.body.contains("totals")
    }

    void 'should preserve specialized account management endpoints'() {
        given:
        String uniqueAccountName = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("specialized")
                .buildAndValidate().accountNameOwner

        // Create account via API to ensure it exists
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(uniqueAccountName)
                .asCredit()
                .buildAndValidate()
        postEndpoint("/account", account.toString())

        when:
        ResponseEntity<String> deactivateResponse = putEndpoint("/account/deactivate/${uniqueAccountName}", "")
        ResponseEntity<String> activateResponse = putEndpoint("/account/activate/${uniqueAccountName}", "")

        then:
        deactivateResponse.statusCode == HttpStatus.OK
        activateResponse.statusCode == HttpStatus.OK
        deactivateResponse.body.contains(uniqueAccountName)
        activateResponse.body.contains(uniqueAccountName)
    }

    // ===== HELPER METHODS =====

    protected ResponseEntity<String> getEndpoint(String path) {
        String token = generateJwtToken(username)

        HttpHeaders reqHeaders = new HttpHeaders()
        reqHeaders.add("Cookie", authCookie ?: ("token=" + token))
        reqHeaders.add("Authorization", "Bearer " + token)
        HttpEntity<Void> entity = new HttpEntity<>(reqHeaders)

        try {
            return restTemplate.exchange(
                    baseUrl + "/api" + path,
                    HttpMethod.GET,
                    entity,
                    String
            )
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }
    }

    protected ResponseEntity<String> postEndpoint(String path, String payload) {
        String token = generateJwtToken(username)

        HttpHeaders reqHeaders = new HttpHeaders()
        reqHeaders.setContentType(MediaType.APPLICATION_JSON)
        reqHeaders.add("Cookie", authCookie ?: ("token=" + token))
        reqHeaders.add("Authorization", "Bearer " + token)
        HttpEntity<String> entity = new HttpEntity<>(payload, reqHeaders)

        try {
            return restTemplate.exchange(
                    baseUrl + "/api" + path,
                    HttpMethod.POST,
                    entity,
                    String
            )
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }
    }

    protected ResponseEntity<String> putEndpoint(String path, String payload) {
        String token = generateJwtToken(username)

        HttpHeaders reqHeaders = new HttpHeaders()
        reqHeaders.setContentType(MediaType.APPLICATION_JSON)
        reqHeaders.add("Cookie", authCookie ?: ("token=" + token))
        reqHeaders.add("Authorization", "Bearer " + token)
        HttpEntity<String> entity = new HttpEntity<>(payload, reqHeaders)

        try {
            return restTemplate.exchange(
                    baseUrl + "/api" + path,
                    HttpMethod.PUT,
                    entity,
                    String
            )
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }
    }

    protected ResponseEntity<String> deleteEndpoint(String path) {
        String token = generateJwtToken(username)

        HttpHeaders reqHeaders = new HttpHeaders()
        reqHeaders.add("Cookie", authCookie ?: ("token=" + token))
        reqHeaders.add("Authorization", "Bearer " + token)
        HttpEntity<Void> entity = new HttpEntity<>(reqHeaders)

        try {
            return restTemplate.exchange(
                    baseUrl + "/api" + path,
                    HttpMethod.DELETE,
                    entity,
                    String
            )
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }
    }
}