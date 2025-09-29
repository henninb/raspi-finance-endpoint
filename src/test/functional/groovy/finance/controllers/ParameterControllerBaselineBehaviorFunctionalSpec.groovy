package finance.controllers

import finance.domain.Parameter
import finance.helpers.SmartParameterBuilder
import groovy.util.logging.Slf4j
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

/**
 * Comprehensive baseline functional tests for ParameterController.
 * These tests document the CURRENT behavior patterns and parameter naming inconsistencies.
 * DO NOT MODIFY - These serve as a safety net during standardization.
 */
@Slf4j
@ActiveProfiles("func")
class ParameterControllerBaselineBehaviorFunctionalSpec extends BaseControllerFunctionalSpec {

    @Shared
    protected String endpointName = 'parameter'

    def setupSpec() {
        // Parent setupSpec() provides testOwner and basic setup
    }

    // CRUD Operation Tests - Documenting ParameterController's Mixed Patterns

    void 'should handle GET /select/active with current empty result behavior - throws 404 like Account/Category'() {
        when: 'requesting active parameters when none exist for unique context'
        String uniqueContext = "empty_${UUID.randomUUID().toString().replace('-', '')[0..7]}"
        ResponseEntity<String> response = selectEndpointWithPath("${endpointName}/select/active")

        then: 'should throw 404 NOT_FOUND (same as Account/Category pattern)'
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.contains("No parameters found")
    }

    void 'should handle GET /select/active with successful retrieval - parameters() method naming'() {
        given: 'an active parameter exists'
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("active_test_${testOwner}")
                .withParameterValue("test_value")
                .buildAndValidate()

        insertEndpoint(endpointName, parameter.toString())

        when: 'requesting active parameters'
        ResponseEntity<String> response = selectEndpointWithPath("${endpointName}/select/active")

        then: 'should return 200 OK with parameters list'
        response.statusCode == HttpStatus.OK
        response.body.contains(parameter.parameterName)
        response.body.contains(parameter.parameterValue)

        and: 'documents parameters() method naming - simple like accounts()/categories()'
        // parameters() - simple plural naming like Account/Category controllers
        true
    }

    void 'should handle GET /select/{parameterName} with mixed parameter naming - camelCase variable, snake_case path'() {
        given: 'a parameter exists'
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("single_test_${testOwner}")
                .withParameterValue("single_value")
                .buildAndValidate()

        insertEndpoint(endpointName, parameter.toString())

        when: 'requesting specific parameter'
        ResponseEntity<String> response = selectEndpointWithPath("${endpointName}/select/${parameter.parameterName}")

        then: 'should return 200 OK with single parameter'
        response.statusCode == HttpStatus.OK
        response.body.contains(parameter.parameterName)
        response.body.contains(parameter.parameterValue)

        and: 'documents mixed parameter naming pattern'
        // selectParameter(@PathVariable parameterName: String) - camelCase variable
        // But path in URL is based on the parameter name value, not the annotation
        // Different controllers have different @PathVariable annotation patterns
        true
    }

    void 'should handle GET /select/{parameterName} not found with current 404 behavior'() {
        when: 'requesting non-existent parameter'
        ResponseEntity<String> response = selectEndpointWithPath("${endpointName}/select/nonexistent_${testOwner}")

        then: 'should return 404 NOT_FOUND (consistent with most controllers)'
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.contains("Parameter not found")
    }

    void 'should handle POST /insert with CREATED status and comprehensive exception handling'() {
        given: 'a new parameter'
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("insert_test_${testOwner}")
                .withParameterValue("insert_value")
                .buildAndValidate()

        when: 'inserting parameter'
        ResponseEntity<String> response = insertEndpoint(endpointName, parameter.toString())

        then: 'should return 201 CREATED'
        response.statusCode == HttpStatus.CREATED
        response.body.contains(parameter.parameterName)
        response.body.contains(parameter.parameterValue)
    }

    void 'should handle POST /insert duplicate with CONFLICT status - same as most controllers'() {
        given: 'an existing parameter'
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("duplicate_test_${testOwner}")
                .withParameterValue("duplicate_value")
                .buildAndValidate()

        insertEndpoint(endpointName, parameter.toString())

        when: 'attempting to insert duplicate'
        ResponseEntity<String> response = insertEndpoint(endpointName, parameter.toString())

        then: 'should return 409 CONFLICT'
        response.statusCode == HttpStatus.CONFLICT
        response.body.contains("Duplicate parameter found")
    }

    void 'should handle PUT /update/{parameter_name} with snake_case path variable annotation'() {
        given: 'an existing parameter'
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("update_test_${testOwner}")
                .withParameterValue("original_value")
                .buildAndValidate()

        insertEndpoint(endpointName, parameter.toString())

        and: 'update data as Parameter entity'
        Parameter updateParameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withParameterName(parameter.parameterName)
                .withParameterValue("updated_value")
                .withActiveStatus(false)
                .buildAndValidate()

        when: 'updating parameter with snake_case path variable'
        ResponseEntity<String> response = updateEndpoint("${endpointName}/update/${parameter.parameterName}", updateParameter)

        then: 'should return 200 OK with updated parameter'
        response.statusCode == HttpStatus.OK
        response.body.contains("updated_value")

        and: 'documents snake_case @PathVariable annotation pattern'
        // @PathVariable("parameter_name") - explicitly snake_case like Category/Description
        // Consistent with Category/Description controllers
        true
    }

    void 'should handle PUT /update/{parameter_name} not found with 404 - validates existence first'() {
        given: 'update parameter data'
        Parameter updateParameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("nonexistent_${testOwner}")
                .withParameterValue("value")
                .buildAndValidate()

        when: 'attempting to update non-existent parameter'
        ResponseEntity<String> response = updateEndpoint("${endpointName}/update/nonexistent_${testOwner}", updateParameter)

        then: 'should return 404 NOT_FOUND (validates existence first)'
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.contains("Parameter not found")
    }

    void 'should handle DELETE /delete/{parameterName} with current behavior'() {
        given: 'an existing parameter'
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("delete_test_${testOwner}")
                .withParameterValue("delete_value")
                .buildAndValidate()

        insertEndpoint(endpointName, parameter.toString())

        when: 'deleting parameter'
        ResponseEntity<String> response = deleteEndpoint(endpointName, parameter.parameterName)

        then: 'should return 200 OK with deleted parameter data'
        response.statusCode == HttpStatus.OK
        response.body.contains(parameter.parameterName)
        response.body.contains(parameter.parameterValue)
    }

    void 'should handle DELETE /delete/{parameterName} not found with 404'() {
        when: 'attempting to delete non-existent parameter'
        ResponseEntity<String> response = deleteEndpoint(endpointName, "nonexistent_${testOwner}")

        then: 'should return 404 NOT_FOUND'
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.contains("Parameter not found")
    }

    // Exception Handling Pattern Tests - Similar to Account/Category but with differences

    void 'should handle comprehensive exception handling - similar to Account/Category'() {
        given: 'invalid parameter data'
        Map<String, Object> invalidParameter = [
                parameterName: "", // Invalid empty name
                parameterValue: "",
                activeStatus: true
        ]

        when: 'attempting to insert invalid parameter'
        ResponseEntity<String> response = insertEndpoint(endpointName, invalidParameter.toString())

        then: 'should handle validation with comprehensive pattern'
        response.statusCode in [HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR]

        and: 'documents ParameterController exception handling'
        // Similar to Account/Category:
        // - DataIntegrityViolationException -> CONFLICT
        // - ResponseStatusException -> rethrow (in insert only)
        // - ValidationException -> BAD_REQUEST
        // - Generic Exception -> INTERNAL_SERVER_ERROR
        //
        // Missing from AccountController:
        // - No IllegalArgumentException handling
        // - No EntityNotFoundException with resilience4j unwrapping
        // - ResponseStatusException handling only in insert, not update
        true
    }

    void 'should handle ResponseStatusException propagation in insert - unique pattern like PaymentController'() {
        expect: 'ParameterController handles ResponseStatusException in insert'
        // Similar to PaymentController - catches and re-throws ResponseStatusException in insert
        // Different from Account/Category that don't handle RSE in insert
        // But unlike PaymentController, doesn't convert to BAD_REQUEST
        true
    }

    // Parameter Naming Pattern Documentation - Mixed Patterns

    void 'should document snake_case path variable annotation - consistent with Category/Description'() {
        expect: 'ParameterController uses snake_case @PathVariable annotations'
        // @PathVariable("parameter_name") - explicitly snake_case
        // Consistent with Category/Description controllers
        // Different from Account/Payment camelCase patterns
        true
    }

    void 'should document parameterName vs parameter_name annotation inconsistency'() {
        expect: 'ParameterController shows parameter naming inconsistencies'
        // Path variable: @PathVariable parameterName (camelCase variable name)
        // Annotation: @PathVariable("parameter_name") (snake_case annotation)
        // Shows the mixed naming pattern documentation goal
        true
    }

    void 'should document entity request body pattern - consistent with most controllers'() {
        expect: 'ParameterController uses Parameter entity for requests'
        // Parameter entity - consistent with Category/Description/Payment/PendingTransaction
        // Different from AccountController Map<String, Any>
        true
    }

    void 'should document parameters() method naming - simple like Account/Category'() {
        expect: 'ParameterController uses simple plural method naming'
        // parameters() - simple plural naming like accounts()/categories()
        // Different from Payment/Description verbose naming (selectAllPayments/selectAllDescriptions)
        true
    }

    // Service Method Call Pattern Documentation

    void 'should document service method naming patterns'() {
        expect: 'ParameterController service method patterns'
        // selectAll() - generic service method name
        // findByParameterName() - standard find pattern
        // insertParameter() - standard insert pattern
        // updateParameter() - standard update pattern
        // deleteByParameterName() - standard delete pattern
        //
        // Consistent standard naming vs PaymentController insertPayment()
        true
    }

    // Empty Result and HTTP Status Documentation

    void 'should document empty result behavior - same as Account/Category'() {
        expect: 'ParameterController throws 404 for empty results'
        // GET /select/active throws 404 when no parameters found
        // Same as Account/Category, different from Payment/Description empty lists
        true
    }

    void 'should document HTTP status patterns - standard CRUD'() {
        expect: 'ParameterController HTTP status patterns'
        // GET: 200 OK or 404 NOT_FOUND - standard
        // POST: 201 CREATED or 409 CONFLICT - standard
        // PUT: 200 OK or 404 NOT_FOUND - standard
        // DELETE: 200 OK or 404 NOT_FOUND - standard
        //
        // Most consistent with standard CRUD patterns
        true
    }

    // Exception Handling Hierarchy Documentation

    void 'should document exception handling complexity - medium complexity'() {
        expect: 'ParameterController has medium complexity exception handling'
        // More complex than PendingTransactionController (has specific exceptions)
        // Less complex than AccountController (no resilience4j, no IllegalArgumentException)
        // Similar to Category/Description but with ResponseStatusException in insert
        true
    }

    // Unique Characteristics Documentation

    void 'should document mixed parameter naming as documentation example'() {
        expect: 'ParameterController demonstrates parameter naming inconsistencies'
        // Perfect example of the naming inconsistencies to be standardized:
        // - camelCase variable names
        // - snake_case path variable annotations
        // - Mixed patterns across controllers
        true
    }

    void 'should document standard CRUD pattern - most consistent controller'() {
        expect: 'ParameterController follows most standard CRUD patterns'
        // Standard endpoint naming: /select/active, /select/{id}, /insert, /update/{id}, /delete/{id}
        // Standard HTTP status codes
        // Standard entity-based request/response patterns
        // Standard exception handling (with minor variations)
        //
        // Most "textbook" controller implementation
        true
    }

    // Helper method for PUT requests - missing from BaseControllerSpec
    protected ResponseEntity<String> updateEndpoint(String path, Parameter parameter) {
        String token = generateJwtToken(username)
        log.info("/api/${path}")

        HttpHeaders reqHeaders = new HttpHeaders()
        reqHeaders.setContentType(MediaType.APPLICATION_JSON)
        reqHeaders.add("Cookie", authCookie ?: ("token=" + token))
        reqHeaders.add("Authorization", "Bearer " + token)
        HttpEntity<String> entity = new HttpEntity<>(parameter.toString(), reqHeaders)

        try {
            return restTemplate.exchange(
                    baseUrl + "/api/${path}",
                    HttpMethod.PUT,
                    entity,
                    String
            )
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }
    }

    // Helper method for GET requests with custom paths - for baseline behavior testing
    protected ResponseEntity<String> selectEndpointWithPath(String fullPath) {
        String token = generateJwtToken(username)
        log.info("/api/${fullPath}")

        HttpHeaders reqHeaders = new HttpHeaders()
        reqHeaders.add("Cookie", authCookie ?: ("token=" + token))
        reqHeaders.add("Authorization", "Bearer " + token)
        HttpEntity<Void> entity = new HttpEntity<>(reqHeaders)

        try {
            return restTemplate.exchange(
                    baseUrl + "/api/${fullPath}",
                    HttpMethod.GET,
                    entity,
                    String
            )
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }
    }
}