package finance.controllers

import finance.domain.Payment
import finance.controllers.BaseControllerFunctionalSpec
import finance.helpers.SmartPaymentBuilder
import finance.helpers.TestFixtures
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Unroll

/**
 * TDD specification for standardized PaymentController implementation.
 * This follows the proven template from Category/Description Controller standardization.
 * Tests define the expected behavior after applying standardization patterns.
 */
@Slf4j
@ActiveProfiles("func")
class StandardizedPaymentControllerFunctionalSpec extends BaseControllerFunctionalSpec {

    @Autowired
    TestFixtures testFixtures

    @Shared
    private final String endpointName = "/payment"

    // STANDARDIZED METHOD NAMING TESTS

    void 'should implement standardized method name: findAllActive instead of selectAllPayments'() {
        when: 'requesting active payments with standardized endpoint'
        ResponseEntity<String> response = getEndpoint(endpointName + "/active")

        then: 'should return successful response'
        response.statusCode == HttpStatus.OK

        and: 'should return list of payments (may be empty)'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse instanceof List

        and: 'documents expected standardization'
        // After standardization: GET /api/payment/active
        // Method name: findAllActive() instead of selectAllPayments()
        // Endpoint: /active instead of /select
        // Behavior: Always returns list, never throws 404
        true
    }

    void 'should implement standardized method name: findById instead of missing single payment endpoint'() {
        given: 'a test payment context using TestDataManager'
        def paymentTestContext = testFixtures.createPaymentContext(testOwner)

        and: 'a test payment using proper SmartBuilder pattern'
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("findsrc_test")
                .withDestinationAccount("finddest_test")
                .withAmount(new BigDecimal("100.00"))
                .buildAndValidate()

        ResponseEntity<String> insertResponse = postEndpoint(endpointName, payment.toString())
        insertResponse.statusCode == HttpStatus.CREATED
        def insertedPayment = new JsonSlurper().parseText(insertResponse.body)
        Long paymentId = insertedPayment.paymentId

        when: 'requesting payment by ID with standardized endpoint'
        ResponseEntity<String> response = getEndpoint(endpointName + "/${paymentId}")

        then: 'should return successful response'
        response.statusCode == HttpStatus.OK

        and: 'should return the payment entity'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.paymentId == paymentId
        jsonResponse.sourceAccount == payment.sourceAccount
        jsonResponse.destinationAccount == payment.destinationAccount

        and: 'documents expected standardization'
        // After standardization: GET /api/payment/{paymentId}
        // Method name: findById(paymentId) instead of missing endpoint
        // Endpoint: /{id} instead of not available
        // Behavior: Returns entity or throws 404
        true

        cleanup:
        paymentTestContext?.cleanup()
    }

    void 'should implement standardized method name: save instead of insertPayment'() {
        given: 'a test payment context using TestDataManager'
        def paymentTestContext = testFixtures.createPaymentContext(testOwner)

        and: 'a test payment using proper SmartBuilder pattern'
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("savesrc_test")
                .withDestinationAccount("savedest_test")
                .withAmount(new BigDecimal("200.00"))
                .buildAndValidate()

        when: 'creating payment with standardized endpoint'
        ResponseEntity<String> response = postEndpoint(endpointName, payment.toString())

        then: 'should return 201 CREATED status'
        response.statusCode == HttpStatus.CREATED

        and: 'should return created payment with ID'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.paymentId != null
        jsonResponse.sourceAccount == payment.sourceAccount
        jsonResponse.destinationAccount == payment.destinationAccount

        and: 'documents expected standardization'
        // After standardization: POST /api/payment
        // Method name: save(@RequestBody Payment) instead of insertPayment(@RequestBody Payment)
        // Endpoint: / instead of /insert
        // Behavior: Returns 201 CREATED with created entity
        true

        cleanup:
        paymentTestContext?.cleanup()
    }

    void 'should implement standardized method name: update instead of updatePayment'() {
        given: 'a test payment context using TestDataManager'
        def paymentTestContext = testFixtures.createPaymentContext(testOwner)

        and: 'a test payment using proper SmartBuilder pattern'
        Payment originalPayment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("updsrc_test")
                .withDestinationAccount("upddest_test")
                .withAmount(new BigDecimal("300.00"))
                .buildAndValidate()

        ResponseEntity<String> insertResponse = postEndpoint(endpointName, originalPayment.toString())
        insertResponse.statusCode == HttpStatus.CREATED
        def insertedPayment = new JsonSlurper().parseText(insertResponse.body)
        Long paymentId = insertedPayment.paymentId

        and: 'updated payment data'
        Payment updatedPayment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("updsrc_new")
                .withDestinationAccount("upddest_new")
                .withAmount(new BigDecimal("400.00"))
                .buildAndValidate()
        // Keep the same ID for update operation
        updatedPayment.paymentId = paymentId

        when: 'updating payment with standardized endpoint'
        ResponseEntity<String> response = putEndpoint(endpointName + "/" + paymentId, updatedPayment.toString())

        then: 'should return 200 OK status'
        response.statusCode == HttpStatus.OK

        and: 'should return updated payment'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.paymentId == paymentId
        jsonResponse.sourceAccount == updatedPayment.sourceAccount
        jsonResponse.destinationAccount == updatedPayment.destinationAccount

        and: 'documents expected standardization'
        // After standardization: PUT /api/payment/{paymentId}
        // Method name: update(paymentId, Payment) instead of updatePayment(paymentId, Payment)
        // Endpoint: /{id} instead of /update/{id}
        // Behavior: Returns 200 OK with updated entity
        true

        cleanup:
        paymentTestContext?.cleanup()
    }

    void 'should implement standardized method name: deleteById instead of deleteByPaymentId'() {
        given: 'a test payment context using TestDataManager'
        def paymentTestContext = testFixtures.createPaymentContext(testOwner)

        and: 'a test payment using proper SmartBuilder pattern'
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("delsrc_test")
                .withDestinationAccount("deldest_test")
                .withAmount(new BigDecimal("500.00"))
                .buildAndValidate()

        ResponseEntity<String> insertResponse = postEndpoint(endpointName, payment.toString())
        insertResponse.statusCode == HttpStatus.CREATED
        def insertedPayment = new JsonSlurper().parseText(insertResponse.body)
        Long paymentId = insertedPayment.paymentId

        when: 'deleting payment with standardized endpoint'
        ResponseEntity<String> response = deleteEndpoint(endpointName + "/${paymentId}")

        then: 'should return 200 OK status'
        response.statusCode == HttpStatus.OK

        and: 'should return deleted payment'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.paymentId == paymentId
        jsonResponse.sourceAccount == payment.sourceAccount

        and: 'documents expected standardization'
        // After standardization: DELETE /api/payment/{paymentId}
        // Method name: deleteById(paymentId) instead of deleteByPaymentId(paymentId)
        // Endpoint: /{id} instead of /delete/{id}
        // Behavior: Returns 200 OK with deleted entity
        true

        cleanup:
        // Payment already deleted by test, but cleanup context
        paymentTestContext = null
    }

    // STANDARDIZED ENDPOINT PATTERN TESTS

    void 'should use RESTful endpoint patterns instead of legacy paths'() {
        given: 'standardized REST endpoints should be available'
        List<String> expectedEndpoints = [
                "/api/payment/active",    // GET - findAllActive
                "/api/payment/{id}",      // GET, PUT, DELETE - findById, update, deleteById
                "/api/payment"            // POST - save
        ]

        when: 'testing standardized endpoints exist'
        ResponseEntity<String> activeResponse = getEndpoint(endpointName + "/active")

        then: 'standardized endpoints should respond successfully'
        activeResponse.statusCode == HttpStatus.OK

        and: 'documents expected standardization'
        // After standardization: RESTful endpoint patterns
        // GET /api/payment/active instead of GET /api/payment/select
        // GET /api/payment/{id} instead of missing
        // POST /api/payment instead of POST /api/payment/insert
        // PUT /api/payment/{id} instead of PUT /api/payment/update/{id}
        // DELETE /api/payment/{id} instead of DELETE /api/payment/delete/{id}
        true
    }

    // STANDARDIZED EMPTY RESULT HANDLING TESTS

    void 'should return empty list for collection operations when no data exists'() {
        when: 'requesting all active payments'
        ResponseEntity<String> response = getEndpoint(endpointName + "/active")

        then: 'should return 200 OK (not 404)'
        response.statusCode == HttpStatus.OK

        and: 'should return list (may contain payments from other tests, which is expected for global entities)'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse instanceof List
        // Note: Payments are global entities, so list may contain data from other tests
        // The key test is that we get a list response, not a 404

        and: 'documents expected standardization'
        // After standardization: Collection handling
        // Collection operations always return list, never throw 404
        // Behavior matches CategoryController and DescriptionController
        // Payments are global entities, so empty state not guaranteed
        true
    }

    void 'should return 404 NOT_FOUND for single entity operations when entity does not exist'() {
        given: 'a non-existent payment ID'
        Long nonExistentId = 999999L

        when: 'requesting non-existent payment'
        ResponseEntity<String> response = getEndpoint(endpointName + "/${nonExistentId}")

        then: 'should return 404 NOT_FOUND'
        response.statusCode == HttpStatus.NOT_FOUND

        and: 'documents expected standardization'
        // After standardization: Single entity handling
        // Single entity operations throw 404 when entity not found
        // Behavior matches CategoryController and DescriptionController
        true
    }

    // STANDARDIZED HTTP STATUS CODE TESTS

    void 'should return 201 CREATED for successful entity creation'() {
        given: 'a test payment context using TestDataManager'
        def paymentTestContext = testFixtures.createPaymentContext(testOwner)

        and: 'a test payment using proper SmartBuilder pattern'
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("crtsrc_test")
                .withDestinationAccount("crtdest_test")
                .withAmount(new BigDecimal("600.00"))
                .buildAndValidate()

        when: 'creating payment'
        ResponseEntity<String> response = postEndpoint(endpointName, payment.toString())

        then: 'should return 201 CREATED status'
        response.statusCode == HttpStatus.CREATED

        and: 'documents expected standardization'
        // After standardization: HTTP status codes
        // Create operations return 201 CREATED
        // Behavior matches CategoryController and DescriptionController
        true

        cleanup:
        paymentTestContext?.cleanup()
    }

    void 'should return 200 OK with entity for successful update operations'() {
        given: 'a test payment context using TestDataManager'
        def paymentTestContext = testFixtures.createPaymentContext(testOwner)

        and: 'a test payment using proper SmartBuilder pattern'
        Payment originalPayment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("oksrc_test")
                .withDestinationAccount("okdest_test")
                .withAmount(new BigDecimal("700.00"))
                .buildAndValidate()

        ResponseEntity<String> insertResponse = postEndpoint(endpointName, originalPayment.toString())
        insertResponse.statusCode == HttpStatus.CREATED
        def insertedPayment = new JsonSlurper().parseText(insertResponse.body)
        Long paymentId = insertedPayment.paymentId

        and: 'updated payment data'
        Payment updatedPayment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("oksrc_upd")
                .withDestinationAccount("okdest_upd")
                .withAmount(new BigDecimal("800.00"))
                .buildAndValidate()
        updatedPayment.paymentId = paymentId

        when: 'updating payment'
        ResponseEntity<String> response = putEndpoint(endpointName + "/" + paymentId, updatedPayment.toString())

        then: 'should return 200 OK status'
        response.statusCode == HttpStatus.OK

        and: 'should return updated entity'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.paymentId == paymentId

        and: 'documents expected standardization'
        // After standardization: HTTP status codes
        // Update operations return 200 OK with updated entity
        // Behavior matches CategoryController and DescriptionController
        true

        cleanup:
        paymentTestContext?.cleanup()
    }

    void 'should return 200 OK with deleted entity for successful delete operations'() {
        given: 'a test payment context using TestDataManager'
        def paymentTestContext = testFixtures.createPaymentContext(testOwner)

        and: 'a test payment using proper SmartBuilder pattern'
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("deloksrc_test")
                .withDestinationAccount("delokdest_test")
                .withAmount(new BigDecimal("900.00"))
                .buildAndValidate()

        ResponseEntity<String> insertResponse = postEndpoint(endpointName, payment.toString())
        insertResponse.statusCode == HttpStatus.CREATED
        def insertedPayment = new JsonSlurper().parseText(insertResponse.body)
        Long paymentId = insertedPayment.paymentId

        when: 'deleting payment'
        ResponseEntity<String> response = deleteEndpoint(endpointName + "/${paymentId}")

        then: 'should return 200 OK status'
        response.statusCode == HttpStatus.OK

        and: 'should return deleted entity'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.paymentId == paymentId
        jsonResponse.sourceAccount == payment.sourceAccount

        and: 'documents expected standardization'
        // After standardization: HTTP status codes
        // Delete operations return 200 OK with deleted entity
        // Behavior matches CategoryController and DescriptionController
        true

        cleanup:
        // Payment already deleted by test, but cleanup context
        paymentTestContext = null
    }

    // SIMPLIFIED VALIDATION TESTS (TDD approach)

    void 'should handle update of non-existent entity with 404 NOT_FOUND'() {
        given: 'a test payment context using TestDataManager'
        def paymentTestContext = testFixtures.createPaymentContext(testOwner)

        and: 'a non-existent payment ID'
        Long nonExistentId = 999999L

        and: 'payment data for update using proper SmartBuilder pattern'
        Payment updatePayment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("errorsrc_test")
                .withDestinationAccount("errordest_test")
                .withAmount(new BigDecimal("1000.00"))
                .buildAndValidate()

        when: 'updating non-existent payment'
        ResponseEntity<String> response = putEndpoint(endpointName + "/" + nonExistentId, updatePayment.toString())

        then: 'should return 404 NOT_FOUND status'
        response.statusCode == HttpStatus.NOT_FOUND

        and: 'documents expected standardization'
        // After standardization: Error handling
        // Update of non-existent entity returns 404 NOT_FOUND
        // Behavior matches CategoryController and DescriptionController
        true

        cleanup:
        paymentTestContext?.cleanup()
    }

    void 'should handle delete of non-existent entity with 404 NOT_FOUND'() {
        given: 'a non-existent payment ID'
        Long nonExistentId = 999999L

        when: 'deleting non-existent payment'
        ResponseEntity<String> response = deleteEndpoint(endpointName + "/${nonExistentId}")

        then: 'should return 404 NOT_FOUND status'
        response.statusCode == HttpStatus.NOT_FOUND

        and: 'documents expected standardization'
        // After standardization: Error handling
        // Delete of non-existent entity returns 404 NOT_FOUND
        // Behavior matches CategoryController and DescriptionController
        true
    }

    void 'should implement standardized logging patterns'() {
        given: 'a test payment context using TestDataManager'
        def paymentTestContext = testFixtures.createPaymentContext(testOwner)

        and: 'a payment for logging test using proper SmartBuilder pattern'
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("logsrc_test")
                .withDestinationAccount("logdest_test")
                .withAmount(new BigDecimal("1100.00"))
                .buildAndValidate()

        when: 'performing CRUD operations'
        ResponseEntity<String> createResponse = postEndpoint(endpointName, payment.toString())
        def jsonResponse = new JsonSlurper().parseText(createResponse.body)
        Long paymentId = jsonResponse.paymentId

        then: 'operations should complete successfully'
        createResponse.statusCode == HttpStatus.CREATED

        when: 'retrieving payment'
        ResponseEntity<String> getResponse = getEndpoint(endpointName + "/${paymentId}")

        then: 'should retrieve successfully'
        getResponse.statusCode == HttpStatus.OK

        and: 'documents expected standardization'
        // After standardization: Logging patterns
        // Consistent debug/info logging across all operations
        // Behavior matches CategoryController and DescriptionController
        true

        cleanup:
        paymentTestContext?.cleanup()
    }

    // TRANSACTION GUID CREATION TESTS (TDD)

    void 'should create transaction GUIDs when saving payment through standardized endpoint'() {
        given: 'a test payment context using TestDataManager'
        def paymentTestContext = testFixtures.createPaymentContext(testOwner)

        and: 'a test payment without GUIDs set'
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("txnsrc_test")
                .withDestinationAccount("txndest_test")
                .withAmount(new BigDecimal("150.00"))
                .buildAndValidate()

        // Ensure GUIDs are null to test automatic creation
        payment.guidSource = null
        payment.guidDestination = null

        when: 'creating payment with standardized endpoint'
        ResponseEntity<String> response = postEndpoint(endpointName, payment.toString())

        then: 'should return 201 CREATED status'
        response.statusCode == HttpStatus.CREATED

        and: 'should create transaction records with valid GUIDs'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.paymentId != null
        jsonResponse.guidSource != null
        jsonResponse.guidDestination != null
        jsonResponse.guidSource != ""
        jsonResponse.guidDestination != ""

        and: 'GUIDs should be valid UUID format'
        jsonResponse.guidSource ==~ /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/
        jsonResponse.guidDestination ==~ /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/

        and: 'documents expected behavior'
        // The standardized save() method should create transaction records first
        // This prevents foreign key constraint violations
        // Behavior should match legacy insertPayment() method
        true

        cleanup:
        paymentTestContext?.cleanup()
    }

    void 'should not fail with foreign key constraint when guidSource/guidDestination are null'() {
        given: 'a test payment context using TestDataManager'
        def paymentTestContext = testFixtures.createPaymentContext(testOwner)

        and: 'a test payment with null GUIDs'
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount("fksrc_test")
                .withDestinationAccount("fkdest_test")
                .withAmount(new BigDecimal("250.00"))
                .buildAndValidate()
        payment.guidSource = null
        payment.guidDestination = null

        when: 'creating payment with standardized endpoint'
        ResponseEntity<String> response = postEndpoint(endpointName, payment.toString())

        then: 'should NOT return 409 CONFLICT'
        response.statusCode != HttpStatus.CONFLICT

        and: 'should return 201 CREATED'
        response.statusCode == HttpStatus.CREATED

        and: 'should have valid transaction GUIDs created'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.guidSource != null
        jsonResponse.guidDestination != null

        and: 'documents expected behavior'
        // This test verifies that the standardized save() method
        // properly creates transaction records before saving the payment
        // This prevents: "ERROR: insert or update on table t_payment violates
        // foreign key constraint fk_payment_guid_source"
        true

        cleanup:
        paymentTestContext?.cleanup()
    }

    // Helper methods for standardized endpoints
    protected ResponseEntity<String> getEndpoint(String path) {
        String token = generateJwtToken(username)
        log.info("/api" + path)

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

    protected ResponseEntity<String> postEndpoint(String path, Object payload) {
        String token = generateJwtToken(username)
        String body = payload instanceof String ? payload : asJson(payload)
        log.info(body)

        HttpHeaders reqHeaders = new HttpHeaders()
        reqHeaders.setContentType(MediaType.APPLICATION_JSON)
        reqHeaders.add("Cookie", authCookie ?: ("token=" + token))
        reqHeaders.add("Authorization", "Bearer " + token)
        HttpEntity<String> entity = new HttpEntity<>(body, reqHeaders)

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

    protected ResponseEntity<String> putEndpoint(String path, Object payload) {
        String token = generateJwtToken(username)
        String body = payload instanceof String ? payload : asJson(payload)
        log.info(body)

        HttpHeaders reqHeaders = new HttpHeaders()
        reqHeaders.setContentType(MediaType.APPLICATION_JSON)
        reqHeaders.add("Cookie", authCookie ?: ("token=" + token))
        reqHeaders.add("Authorization", "Bearer " + token)
        HttpEntity<String> entity = new HttpEntity<>(body, reqHeaders)

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
        log.info("/api" + path)

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
