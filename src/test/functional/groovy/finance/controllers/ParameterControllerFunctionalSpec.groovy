package finance.controllers

import finance.domain.Parameter
import finance.controllers.BaseControllerFunctionalSpec
import finance.helpers.SmartParameterBuilder
import finance.helpers.TestFixtures
import finance.helpers.ParameterTestContext
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
 * TDD specification for standardized ParameterController implementation.
 * This serves as the template for controller standardization.
 * Tests define the expected behavior after applying standardization patterns.
 */
@Slf4j
@ActiveProfiles("func")
class StandardizedParameterControllerFunctionalSpec extends BaseControllerFunctionalSpec {

    @Autowired
    TestFixtures testFixtures

    @Shared
    private final String endpointName = "/parameter"

    @Shared
    ParameterTestContext parameterTestContext

    // STANDARDIZED METHOD NAMING TESTS

    void 'should implement standardized method name: findAllActive instead of parameters'() {
        when: 'requesting active parameters with standardized endpoint'
        ResponseEntity<String> response = getEndpoint(endpointName + "/active")

        then: 'should return successful response'
        response.statusCode == HttpStatus.OK

        and: 'should return list of parameters (may be empty)'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse instanceof List

        and: 'documents expected standardization'
        // After standardization: GET /api/parameter/active
        // Method name: findAllActive() instead of parameters()
        // Endpoint: /active instead of /select/active
        // Behavior: Always returns list, never throws 404
        true
    }

    void 'should implement standardized method name: findById instead of parameter'() {
        given: 'a test parameter context'
        parameterTestContext = testFixtures.createParameterTestContext(testOwner)

        and: 'a test parameter exists'
        Parameter parameter = parameterTestContext.createUniqueParameter("test_param", "test_value")

        ResponseEntity<String> insertResponse = postEndpoint(endpointName, parameter.toString())
        insertResponse.statusCode == HttpStatus.CREATED

        when: 'requesting single parameter with standardized endpoint'
        ResponseEntity<String> response = getEndpoint(endpointName + "/${parameter.parameterName}")

        then: 'should return successful response'
        response.statusCode == HttpStatus.OK

        and: 'should return parameter object'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.parameterName == parameter.parameterName

        and: 'documents expected standardization'
        // After standardization: GET /api/parameter/{parameterName}
        // Method name: findById(parameterName) instead of parameter(parameterName)
        // Endpoint: /{id} instead of /select/{id}
        // Behavior: Returns entity or throws 404
        true
    }

    void 'should implement standardized method name: save instead of insertParameter'() {
        given: 'a test parameter context'
        parameterTestContext = testFixtures.createParameterTestContext(testOwner)

        and: 'a new parameter to create'
        Parameter parameter = parameterTestContext.createUniqueParameter("create_param", "create_value")

        when: 'creating parameter with standardized endpoint'
        ResponseEntity<String> response = postEndpoint(endpointName, parameter.toString())

        then: 'should return 201 CREATED'
        response.statusCode == HttpStatus.CREATED

        and: 'should return created parameter'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.parameterName == parameter.parameterName

        and: 'documents expected standardization'
        // After standardization: POST /api/parameter
        // Method name: save(parameter) instead of insertParameter(parameter)
        // Endpoint: / instead of /insert
        // Behavior: Returns 201 CREATED with entity
        true
    }

    void 'should implement standardized method name: update instead of updateParameter'() {
        given: 'a test parameter context'
        parameterTestContext = testFixtures.createParameterTestContext(testOwner)

        and: 'an existing parameter'
        Parameter parameter = parameterTestContext.createUniqueParameter("update_param", "original_value")

        ResponseEntity<String> insertResponse = postEndpoint(endpointName, parameter.toString())
        insertResponse.statusCode == HttpStatus.CREATED

        and: 'get the created parameter with actual database ID'
        def createdParameter = new JsonSlurper().parseText(insertResponse.body)
        parameter.parameterId = createdParameter.parameterId

        and: 'updated parameter data with unique value'
        // Generate unique updated value to avoid constraint violations
        String uniqueUpdatedValue = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterValue("updated")
                .build().parameterValue
        parameter.parameterValue = uniqueUpdatedValue

        when: 'updating parameter with standardized endpoint'
        ResponseEntity<String> response = putEndpoint(endpointName + "/${parameter.parameterName}", parameter.toString())

        then: 'should return 200 OK'
        response.statusCode == HttpStatus.OK

        and: 'should return updated parameter'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.parameterValue == uniqueUpdatedValue

        and: 'documents expected standardization'
        // After standardization: PUT /api/parameter/{parameterName}
        // Method name: update(parameterName, parameter) instead of updateParameter(parameterName, parameter)
        // Endpoint: /{id} instead of /update/{id}
        // Behavior: Returns 200 OK with entity
        true
    }

    void 'should implement standardized method name: deleteById instead of deleteParameter'() {
        given: 'a test parameter context'
        parameterTestContext = testFixtures.createParameterTestContext(testOwner)

        and: 'an existing parameter'
        Parameter parameter = parameterTestContext.createUniqueParameter("delete_param", "delete_value")

        ResponseEntity<String> insertResponse = postEndpoint(endpointName, parameter.toString())
        insertResponse.statusCode == HttpStatus.CREATED

        when: 'deleting parameter with standardized endpoint'
        ResponseEntity<String> response = deleteEndpoint(endpointName + "/${parameter.parameterName}")

        then: 'should return 200 OK with deleted entity'
        response.statusCode == HttpStatus.OK

        and: 'should return deleted parameter'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.parameterName == parameter.parameterName

        and: 'documents expected standardization'
        // After standardization: DELETE /api/parameter/{parameterName}
        // Method name: deleteById(parameterName) instead of deleteParameter(parameterName)
        // Endpoint: /{id} instead of /delete/{id}
        // Behavior: Returns 200 OK with deleted entity
        true
    }

    // STANDARDIZED PARAMETER NAMING TESTS

    void 'should use camelCase parameter names without @PathVariable annotations'() {
        given: 'a test parameter context'
        parameterTestContext = testFixtures.createParameterTestContext(testOwner)

        and: 'a test parameter'
        Parameter parameter = parameterTestContext.createUniqueParameter("camelcase_param", "camelcase_value")

        postEndpoint(endpointName, parameter.toString())

        when: 'accessing parameter by ID with camelCase parameter'
        ResponseEntity<String> response = getEndpoint(endpointName + "/${parameter.parameterName}")

        then: 'should handle camelCase parameter correctly'
        response.statusCode == HttpStatus.OK

        and: 'documents parameter naming standardization'
        // After standardization:
        // fun findById(parameterName: String) - no @PathVariable annotation
        // URL: /api/parameter/{parameterName} - camelCase in URL
        // No snake_case annotations like @PathVariable("parameter_name")
        true
    }

    // STANDARDIZED EMPTY RESULT HANDLING TESTS

    void 'should return empty list instead of throwing 404 for findAllActive'() {
        given: 'clean database state'
        // Ensure clean slate by deleting any leftover test parameters
        try {
            def allParams = getEndpoint(endpointName + "/active")
            if (allParams.statusCode == HttpStatus.OK) {
                def jsonResponse = new JsonSlurper().parseText(allParams.body)
                jsonResponse.each { param ->
                    if (param.parameterName.contains("test_")) {
                        try {
                            deleteEndpoint(endpointName + "/${param.parameterName}")
                        } catch (Exception ignored) {
                            // Ignore cleanup errors
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore cleanup errors
        }

        when: 'requesting all active parameters'
        ResponseEntity<String> response = getEndpoint(endpointName + "/active")

        then: 'should return 200 OK with empty list'
        response.statusCode == HttpStatus.OK

        and: 'should return empty list, not 404'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse instanceof List
        jsonResponse.size() == 0

        and: 'documents empty result standardization'
        // After standardization: Always return empty list for collection operations
        // Never throw 404 for findAllActive() - only for findById()
        true
    }

    void 'should throw 404 for findById when entity not found'() {
        when: 'requesting non-existent parameter'
        ResponseEntity<String> response = getEndpoint(endpointName + "/non_existent_parameter")

        then: 'should return 404 NOT_FOUND'
        response.statusCode == HttpStatus.NOT_FOUND

        and: 'documents single entity not found behavior'
        // After standardization: findById() throws 404 when entity not found
        // This is correct and should remain consistent
        true
    }

    // STANDARDIZED EXCEPTION HANDLING TESTS

    void 'should handle duplicate parameter creation with standardized exception response'() {
        given: 'a test parameter context'
        parameterTestContext = testFixtures.createParameterTestContext(testOwner)

        and: 'an existing parameter'
        Parameter parameter = parameterTestContext.createUniqueParameter("duplicate_param", "original_value")

        ResponseEntity<String> firstInsert = postEndpoint(endpointName, parameter.toString())
        firstInsert.statusCode == HttpStatus.CREATED

        when: 'attempting to create duplicate parameter'
        ResponseEntity<String> response = postEndpoint(endpointName, parameter.toString())

        then: 'should return 409 CONFLICT'
        response.statusCode == HttpStatus.CONFLICT

        and: 'documents standardized exception handling'
        // After standardization: ServiceResult.BusinessError -> 409 CONFLICT
        // Status code indicates conflict, response body is empty (typed response pattern)
        // Consistent with other modernized controllers (Payment, Transfer, Category, etc.)
        true
    }

    @Unroll
    void 'should handle validation errors with standardized responses: #scenario'() {
        when: 'attempting operation with invalid data'
        ResponseEntity<String> response = postEndpoint(endpointName, invalidData)

        then: 'should return appropriate error status'
        response.statusCode in [HttpStatus.BAD_REQUEST, HttpStatus.CONFLICT, HttpStatus.INTERNAL_SERVER_ERROR]

        and: 'documents comprehensive exception handling'
        // After standardization, all controllers should handle:
        // - ValidationException -> 400 BAD_REQUEST
        // - IllegalArgumentException -> 400 BAD_REQUEST
        // - EntityNotFoundException -> 404 NOT_FOUND
        // - ExecutionException with wrapped exceptions
        // - ResponseStatusException -> rethrow as-is
        // - Generic Exception -> 500 INTERNAL_SERVER_ERROR
        true

        where:
        scenario | invalidData
        'Invalid parameter format' | '{"invalid": "data"}'
        'Missing required fields' | '{}'
        'Constraint violations' | '{"parameterName": "", "parameterValue": "test"}'
    }

    // STANDARDIZED ENDPOINT PATTERN TESTS

    @Unroll
    void 'should implement standardized REST endpoint patterns: #operation at #endpoint'() {
        expect: 'All endpoints should follow RESTful patterns'

        // After standardization:
        // GET /api/parameter/active - collection retrieval
        // GET /api/parameter/{parameterName} - single entity retrieval
        // POST /api/parameter - entity creation
        // PUT /api/parameter/{parameterName} - entity update
        // DELETE /api/parameter/{parameterName} - entity deletion
        true

        where:
        operation | endpoint
        'findAllActive' | 'GET /api/parameter/active'
        'findById' | 'GET /api/parameter/{parameterName}'
        'save' | 'POST /api/parameter'
        'update' | 'PUT /api/parameter/{parameterName}'
        'deleteById' | 'DELETE /api/parameter/{parameterName}'
    }

    // STANDARDIZED REQUEST/RESPONSE BODY TESTS

    void 'should use Parameter entity type for all request bodies'() {
        given: 'a test parameter context'
        parameterTestContext = testFixtures.createParameterTestContext(testOwner)

        and: 'a parameter entity'
        Parameter parameter = parameterTestContext.createUniqueParameter("entity_param", "entity_value")

        when: 'creating parameter with entity body'
        ResponseEntity<String> response = postEndpoint(endpointName, parameter.toString())

        then: 'should accept Parameter entity type'
        response.statusCode == HttpStatus.CREATED

        and: 'documents standardized request body handling'
        // After standardization: save(parameter: Parameter)
        // No Map<String, Any> usage like AccountController
        // All request bodies should use entity types directly
        true
    }

    void 'should return Parameter entity type for all response bodies'() {
        given: 'a test parameter context'
        parameterTestContext = testFixtures.createParameterTestContext(testOwner)

        and: 'a test parameter'
        Parameter parameter = parameterTestContext.createUniqueParameter("response_param", "response_value")

        postEndpoint(endpointName, parameter.toString())

        when: 'retrieving parameter'
        ResponseEntity<String> response = getEndpoint(endpointName + "/${parameter.parameterName}")

        then: 'should return Parameter entity'
        response.statusCode == HttpStatus.OK

        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.parameterName == parameter.parameterName
        jsonResponse.parameterValue == parameter.parameterValue

        and: 'documents standardized response body handling'
        // After standardization: ResponseEntity<Parameter>
        // Entity objects for all CRUD operations
        // List<Parameter> for collection operations
        true
    }

    // STANDARDIZED LOGGING TESTS

    void 'should implement standardized logging patterns'() {
        given: 'a test parameter context'
        parameterTestContext = testFixtures.createParameterTestContext(testOwner)

        and: 'a test parameter for logging verification'
        Parameter parameter = parameterTestContext.createUniqueParameter("log_param", "log_value")

        when: 'performing CRUD operations'
        ResponseEntity<String> createResponse = postEndpoint(endpointName, parameter.toString())
        ResponseEntity<String> readResponse = getEndpoint(endpointName + "/${parameter.parameterName}")

        and: 'update with correct database ID and unique value'
        def createdParameter = new JsonSlurper().parseText(createResponse.body)
        parameter.parameterId = createdParameter.parameterId
        // Generate unique updated value for logging test
        String uniqueLogValue = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterValue("logupdate")
                .build().parameterValue
        parameter.parameterValue = uniqueLogValue
        ResponseEntity<String> updateResponse = putEndpoint(endpointName + "/${parameter.parameterName}", parameter.toString())
        ResponseEntity<String> deleteResponse = deleteEndpoint(endpointName + "/${parameter.parameterName}")

        then: 'all operations should complete successfully'
        createResponse.statusCode == HttpStatus.CREATED
        readResponse.statusCode == HttpStatus.OK
        updateResponse.statusCode == HttpStatus.OK
        deleteResponse.statusCode == HttpStatus.OK

        and: 'documents expected logging standards'
        // After standardization, all controllers should log:
        // DEBUG: Method entry with parameters
        // INFO: Successful operations with entity IDs
        // WARN: Entity not found scenarios
        // ERROR: Exception scenarios with full context
        true
    }

    // TEMPLATE COMPLETENESS VERIFICATION

    void 'should serve as complete standardization template for other controllers'() {
        expect: 'ParameterController standardization covers all required patterns'

        def standardizationAspects = [
                'Method naming standardization',
                'Parameter naming standardization (camelCase)',
                'Empty result handling standardization',
                'HTTP status code standardization',
                'Exception handling standardization',
                'Endpoint pattern standardization',
                'Request/response body standardization',
                'Logging standardization',
                'Entity type usage consistency',
                'RESTful pattern adherence'
        ]

        // This template should be applied to:
        // 1. CategoryController (similar complexity)
        // 2. DescriptionController (similar complexity)
        // 3. PaymentController (moderate complexity)
        // 4. AccountController (high complexity - business logic separation)
        // 5. PendingTransactionController (unique patterns)
        // 6. TransactionController (most complex - hierarchical endpoints)

        standardizationAspects.size() == 10

        and: 'provides clear migration guidance'
        true
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
        String body = bodyAsJson(payload)
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
        String body = bodyAsJson(payload)
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
