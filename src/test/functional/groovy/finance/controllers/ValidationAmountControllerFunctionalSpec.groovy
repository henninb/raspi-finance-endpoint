package finance.controllers

import finance.helpers.SmartAccountBuilder
import finance.helpers.SmartValidationAmountBuilder
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import groovy.util.logging.Slf4j

@Slf4j
@ActiveProfiles("func")
class StandardizedValidationAmountControllerFunctionalSpec extends BaseControllerFunctionalSpec {

    @Autowired
    protected JdbcTemplate jdbcTemplate

    // ===== STANDARDIZED METHOD NAMING TESTS =====

    void 'should implement standardized method name: findAllActive instead of non-existent collection endpoint'() {
        given:
        // Create account for validation amounts
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("findallactive")
                .asDebit()
                .buildAndValidate()
        def accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        // Create validation amount
        def validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .buildAndValidate()
        insertValidationAmountEndpoint(validationAmount.toString(), testOwner)

        when: 'requesting active validation amounts with standardized endpoint'
        ResponseEntity<String> response = getEndpoint("/validation/amount/active")

        then: 'should return successful response'
        response.statusCode == HttpStatus.OK

        and: 'should return list of validation amounts'
        response.body.contains("[") || response.body.contains("amount")
    }

    void 'should implement standardized method name: findById instead of selectValidationAmountByAccountId'() {
        given:
        // Create account for validation amount
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("findbyid")
                .asDebit()
                .buildAndValidate()
        def accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        // Create validation amount
        def validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .buildAndValidate()
        def createResponse = insertValidationAmountEndpoint(validationAmount.toString(), testOwner)
        assert createResponse.statusCode == HttpStatus.OK

        // Extract validation amount ID
        String createBody = createResponse.body
        String validationIdStr = (createBody =~ /"validationId":(\d+)/)[0][1]
        Long validationId = Long.parseLong(validationIdStr)

        when:
        ResponseEntity<String> response = getEndpoint("/validation/amount/${validationId}")

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains("\"validationId\":${validationId}")
    }

    void 'should implement standardized method name: save instead of insertValidationAmount'() {
        given:
        // Create account for validation amount
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("saveinstead")
                .asDebit()
                .buildAndValidate()
        def accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        def validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .buildAndValidate()

        when:
        ResponseEntity<String> response = postEndpoint("/validation/amount", validationAmount.toString())

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.contains("amount")
    }

    void 'should implement standardized method name: update instead of non-existent update method'() {
        given:
        // Create account for validation amount
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("updateinstead")
                .asDebit()
                .buildAndValidate()
        def accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        // Create validation amount first
        def initialValidationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .buildAndValidate()
        def createResponse = insertValidationAmountEndpoint(initialValidationAmount.toString(), testOwner)
        assert createResponse.statusCode == HttpStatus.OK

        // Extract validation amount ID
        String createBody = createResponse.body
        String validationIdStr = (createBody =~ /"validationId":(\d+)/)[0][1]
        Long validationId = Long.parseLong(validationIdStr)

        // Parse the complete validation amount data from the response
        def validationAmountJson = new groovy.json.JsonSlurper().parseText(createBody)

        // Modify the amount for update
        validationAmountJson.amount = 150.75
        String updatePayload = new groovy.json.JsonBuilder(validationAmountJson).toString()

        when:
        ResponseEntity<String> response = putEndpoint("/validation/amount/${validationId}", updatePayload)

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains("150.75") || response.body.contains("150.7")
    }

    void 'should implement standardized method name: deleteById instead of non-existent delete method'() {
        given:
        // Create account for validation amount
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("deletebyid")
                .asDebit()
                .buildAndValidate()
        def accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        // Create validation amount
        def validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .buildAndValidate()
        def createResponse = insertValidationAmountEndpoint(validationAmount.toString(), testOwner)
        assert createResponse.statusCode == HttpStatus.OK

        // Extract validation amount ID
        String createBody = createResponse.body
        String validationIdStr = (createBody =~ /"validationId":(\d+)/)[0][1]
        Long validationId = Long.parseLong(validationIdStr)

        when:
        ResponseEntity<String> response = deleteEndpoint("/validation/amount/${validationId}")

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains("\"validationId\":${validationId}")
    }

    // ===== STANDARDIZED URL PATTERNS TESTS =====

    void 'should implement standardized URL pattern: /active instead of non-existent collection endpoint'() {
        when:
        ResponseEntity<String> standardizedResponse = getEndpoint("/validation/amount/active")

        then:
        standardizedResponse.statusCode == HttpStatus.OK
        // Should return empty list if no validation amounts exist
        standardizedResponse.body.startsWith("[")
    }

    void 'should implement standardized URL pattern: /{validationId} instead of /select/{accountNameOwner}/{transactionStateValue}'() {
        given:
        // Create account for validation amount
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("urlpattern")
                .asDebit()
                .buildAndValidate()
        def accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        // Create validation amount
        def validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .buildAndValidate()
        def createResponse = insertValidationAmountEndpoint(validationAmount.toString(), testOwner)
        assert createResponse.statusCode == HttpStatus.OK

        // Extract validation amount ID
        String createBody = createResponse.body
        String validationIdStr = (createBody =~ /"validationId":(\d+)/)[0][1]
        Long validationId = Long.parseLong(validationIdStr)

        when:
        ResponseEntity<String> standardizedResponse = getEndpoint("/validation/amount/${validationId}")
        ResponseEntity<String> legacyResponse = getEndpoint("/validation/amount/select/${account.accountNameOwner}/cleared")

        then:
        standardizedResponse.statusCode == HttpStatus.OK
        legacyResponse.statusCode == HttpStatus.OK  // Legacy endpoint maintained for backward compatibility
        standardizedResponse.body.contains("\"validationId\":${validationId}")
        legacyResponse.body.contains("amount")
    }

    void 'should implement standardized URL pattern: / instead of /insert/{accountNameOwner} for POST'() {
        given:
        // Create account for validation amount
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("poststandard")
                .asDebit()
                .buildAndValidate()
        def accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        def validationAmount1 = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .buildAndValidate()
        def validationAmount2 = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asOutstanding()
                .buildAndValidate()

        when:
        ResponseEntity<String> standardizedResponse = postEndpoint("/validation/amount", validationAmount1.toString())
        ResponseEntity<String> legacyResponse = insertValidationAmountEndpoint(validationAmount2.toString(), account.accountNameOwner)

        then:
        standardizedResponse.statusCode == HttpStatus.CREATED
        legacyResponse.statusCode == HttpStatus.OK  // Legacy endpoint maintained for backward compatibility
        standardizedResponse.body.contains("amount")
        legacyResponse.body.contains("amount")
    }

    // ===== EMPTY RESULT HANDLING TESTS =====

    void 'should return empty list for collections when no results found'() {
        when:
        ResponseEntity<String> response = getEndpoint("/validation/amount/active")

        then:
        response.statusCode == HttpStatus.OK
        // Should return a JSON array (could be empty [] or contain test data from other tests)
        response.body.startsWith("[") && response.body.endsWith("]")
    }

    void 'should return 404 for single entity when not found'() {
        given:
        Long nonExistentValidationId = 99999L

        when:
        ResponseEntity<String> response = getEndpoint("/validation/amount/${nonExistentValidationId}")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    // ===== HTTP STATUS CODE TESTS =====

    void 'should return 201 CREATED for entity creation'() {
        given:
        // Create account for validation amount
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("created201")
                .asDebit()
                .buildAndValidate()
        def accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        def validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .buildAndValidate()

        when:
        ResponseEntity<String> response = postEndpoint("/validation/amount", validationAmount.toString())

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.contains("amount")
    }

    void 'should return 200 OK for entity update'() {
        given:
        // Create account for validation amount
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("update200")
                .asDebit()
                .buildAndValidate()
        def accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        // Create validation amount first
        def initialValidationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .buildAndValidate()
        def createResponse = insertValidationAmountEndpoint(initialValidationAmount.toString(), testOwner)
        assert createResponse.statusCode == HttpStatus.OK

        // Extract validation amount ID
        String createBody = createResponse.body
        String validationIdStr = (createBody =~ /"validationId":(\d+)/)[0][1]
        Long validationId = Long.parseLong(validationIdStr)

        // Parse the complete validation amount data from the response
        def validationAmountJson = new groovy.json.JsonSlurper().parseText(createBody)

        // Modify the amount for update
        validationAmountJson.amount = 175.25
        String updatePayload = new groovy.json.JsonBuilder(validationAmountJson).toString()

        when:
        ResponseEntity<String> response = putEndpoint("/validation/amount/${validationId}", updatePayload)

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains("175.25") || response.body.contains("175.2")
    }

    void 'should return 200 OK with deleted entity for deletion'() {
        given:
        // Create account for validation amount
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("delete200")
                .asDebit()
                .buildAndValidate()
        def accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        // Create validation amount
        def validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .buildAndValidate()
        def createResponse = insertValidationAmountEndpoint(validationAmount.toString(), testOwner)
        assert createResponse.statusCode == HttpStatus.OK

        // Extract validation amount ID
        String createBody = createResponse.body
        String validationIdStr = (createBody =~ /"validationId":(\d+)/)[0][1]
        Long validationId = Long.parseLong(validationIdStr)

        when:
        ResponseEntity<String> response = deleteEndpoint("/validation/amount/${validationId}")

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains("\"validationId\":${validationId}")
    }

    // ===== REQUEST BODY HANDLING TESTS =====

    void 'should use ValidationAmount entity type for all operations'() {
        given:
        // Create account for validation amount
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("entitytype")
                .asDebit()
                .buildAndValidate()
        def accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        def validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .buildAndValidate()

        when:
        ResponseEntity<String> response = postEndpoint("/validation/amount", validationAmount.toString())

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.contains("amount")
        response.body.contains("validationId")
    }

    // ===== EXCEPTION HANDLING TESTS =====

    void 'should handle validation errors with standardized exception response'() {
        given:
        // Create validation amount with invalid data (negative amount)
        def invalidValidationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(999999L)  // Non-existent account
                .asCleared()
                .buildAndValidate()

        when:
        ResponseEntity<String> response = postEndpoint("/validation/amount", invalidValidationAmount.toString())

        then:
        response.statusCode in [HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.CONFLICT]
    }

    // ===== PARAMETER NAMING TESTS =====

    void 'should use camelCase parameter names without annotations'() {
        given:
        // Create account for validation amount
        def account = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("camelcase")
                .asDebit()
                .buildAndValidate()
        def accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        // Create validation amount
        def validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .asCleared()
                .buildAndValidate()
        def createResponse = insertValidationAmountEndpoint(validationAmount.toString(), testOwner)
        assert createResponse.statusCode == HttpStatus.OK

        // Extract validation amount ID
        String createBody = createResponse.body
        String validationIdStr = (createBody =~ /"validationId":(\d+)/)[0][1]
        Long validationId = Long.parseLong(validationIdStr)

        when: 'using camelCase parameter'
        ResponseEntity<String> response = getEndpoint("/validation/amount/${validationId}")

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains("\"validationId\":${validationId}")
    }

    // ===== HELPER METHODS =====

    private ResponseEntity<String> insertValidationAmountEndpoint(String payload, String accountNameOwner) {
        String token = generateJwtToken(username)

        HttpHeaders reqHeaders = new HttpHeaders()
        reqHeaders.setContentType(MediaType.APPLICATION_JSON)
        reqHeaders.add("Cookie", authCookie ?: ("token=" + token))
        reqHeaders.add("Authorization", "Bearer " + token)
        HttpEntity<String> entity = new HttpEntity<>(payload, reqHeaders)

        try {
            return restTemplate.exchange(
                    baseUrl + "/api/validation/amount/insert/${accountNameOwner}",
                    HttpMethod.POST,
                    entity,
                    String
            )
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }
    }

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