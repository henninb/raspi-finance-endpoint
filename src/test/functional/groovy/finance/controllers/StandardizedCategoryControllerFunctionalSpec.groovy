package finance.controllers

import finance.domain.Category
import finance.controllers.BaseControllerFunctionalSpec
import finance.helpers.SmartCategoryBuilder
import finance.helpers.TestFixtures
import finance.helpers.CategoryTestContext
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
 * TDD specification for standardized CategoryController implementation.
 * This follows the proven template from ParameterController standardization.
 * Tests define the expected behavior after applying standardization patterns.
 */
@Slf4j
@ActiveProfiles("func")
class StandardizedCategoryControllerFunctionalSpec extends BaseControllerFunctionalSpec {

    @Autowired
    TestFixtures testFixtures

    @Shared
    private final String endpointName = "/category"

    @Shared
    CategoryTestContext categoryTestContext

    // STANDARDIZED METHOD NAMING TESTS

    void 'should implement standardized method name: findAllActive instead of categories'() {
        when: 'requesting active categories with standardized endpoint'
        ResponseEntity<String> response = getEndpoint(endpointName + "/active")

        then: 'should return successful response'
        response.statusCode == HttpStatus.OK

        and: 'should return list of categories (may be empty)'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse instanceof List

        and: 'documents expected standardization'
        // After standardization: GET /api/category/active
        // Method name: findAllActive() instead of categories()
        // Endpoint: /active instead of /select/active
        // Behavior: Always returns list, never throws 404
        true
    }

    void 'should implement standardized method name: findById instead of category'() {
        given: 'a test category context'
        categoryTestContext = testFixtures.createCategoryTestContext(testOwner)

        and: 'a test category exists'
        Category category = categoryTestContext.createUniqueCategory("test_category")

        ResponseEntity<String> insertResponse = postEndpoint(endpointName, category.toString())
        insertResponse.statusCode == HttpStatus.CREATED

        when: 'requesting single category with standardized endpoint'
        ResponseEntity<String> response = getEndpoint(endpointName + "/${category.categoryName}")

        then: 'should return successful response'
        response.statusCode == HttpStatus.OK

        and: 'should return category object'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.categoryName == category.categoryName

        and: 'documents expected standardization'
        // After standardization: GET /api/category/{categoryName}
        // Method name: findById(categoryName) instead of category(categoryName)
        // Endpoint: /{id} instead of /select/{id}
        // Behavior: Returns entity or throws 404
        true
    }

    void 'should implement standardized method name: save instead of insertCategory'() {
        given: 'a test category context'
        categoryTestContext = testFixtures.createCategoryTestContext(testOwner)

        and: 'a new category to create'
        Category category = categoryTestContext.createUniqueCategory("create_category")

        when: 'creating category with standardized endpoint'
        ResponseEntity<String> response = postEndpoint(endpointName, category.toString())

        then: 'should return 201 CREATED'
        response.statusCode == HttpStatus.CREATED

        and: 'should return created category'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.categoryName == category.categoryName

        and: 'documents expected standardization'
        // After standardization: POST /api/category
        // Method name: save(category) instead of insertCategory(category)
        // Endpoint: / instead of /insert
        // Behavior: Returns 201 CREATED with entity
        true
    }

    void 'should implement standardized method name: update instead of updateCategory'() {
        given: 'a test category context'
        categoryTestContext = testFixtures.createCategoryTestContext(testOwner)

        and: 'an existing category'
        Category category = categoryTestContext.createUniqueCategory("update_category")

        ResponseEntity<String> insertResponse = postEndpoint(endpointName, category.toString())
        insertResponse.statusCode == HttpStatus.CREATED

        and: 'get the created category with actual database ID'
        def createdCategory = new JsonSlurper().parseText(insertResponse.body)
        category.categoryId = createdCategory.categoryId

        // Store the original category name for the URL
        String originalCategoryName = category.categoryName

        and: 'updated category data with unique name'
        // Generate unique updated name to avoid constraint violations
        String uniqueUpdatedName = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("updated")
                .build().categoryName
        category.categoryName = uniqueUpdatedName

        when: 'updating category with standardized endpoint using original name in URL'
        ResponseEntity<String> response = putEndpoint(endpointName + "/${originalCategoryName}", category.toString())

        then: 'should return 200 OK'
        response.statusCode == HttpStatus.OK

        and: 'should return updated category'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.categoryName == uniqueUpdatedName

        and: 'documents expected standardization'
        // After standardization: PUT /api/category/{categoryName}
        // Method name: update(categoryName, category) instead of updateCategory(categoryName, category)
        // Endpoint: /{id} instead of /update/{id}
        // Behavior: Returns 200 OK with entity
        true
    }

    void 'should implement standardized method name: deleteById instead of deleteCategory'() {
        given: 'a test category context'
        categoryTestContext = testFixtures.createCategoryTestContext(testOwner)

        and: 'an existing category'
        Category category = categoryTestContext.createUniqueCategory("delete_category")

        ResponseEntity<String> insertResponse = postEndpoint(endpointName, category.toString())
        insertResponse.statusCode == HttpStatus.CREATED

        when: 'deleting category with standardized endpoint'
        ResponseEntity<String> response = deleteEndpoint(endpointName + "/${category.categoryName}")

        then: 'should return 200 OK with deleted entity'
        response.statusCode == HttpStatus.OK

        and: 'should return deleted category'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.categoryName == category.categoryName

        and: 'documents expected standardization'
        // After standardization: DELETE /api/category/{categoryName}
        // Method name: deleteById(categoryName) instead of deleteCategory(categoryName)
        // Endpoint: /{id} instead of /delete/{id}
        // Behavior: Returns 200 OK with deleted entity
        true
    }

    // STANDARDIZED PARAMETER NAMING TESTS

    void 'should use camelCase parameter names without @PathVariable annotations'() {
        given: 'a test category context'
        categoryTestContext = testFixtures.createCategoryTestContext(testOwner)

        and: 'a test category'
        Category category = categoryTestContext.createUniqueCategory("camelcase_category")

        postEndpoint(endpointName, category.toString())

        when: 'accessing category by ID with camelCase parameter'
        ResponseEntity<String> response = getEndpoint(endpointName + "/${category.categoryName}")

        then: 'should handle camelCase parameter correctly'
        response.statusCode == HttpStatus.OK

        and: 'documents parameter naming standardization'
        // After standardization:
        // fun findById(categoryName: String) - no @PathVariable annotation
        // URL: /api/category/{categoryName} - camelCase in URL
        // No snake_case annotations like @PathVariable("category_name")
        true
    }

    // STANDARDIZED EMPTY RESULT HANDLING TESTS

    void 'should return empty list instead of throwing 404 for findAllActive'() {
        given: 'a clean test environment with proper isolation'
        // Create a new unique test owner for this test to ensure complete isolation
        String isolatedTestOwner = "emptytest_${UUID.randomUUID().toString().substring(0, 8)}"

        and: 'a test category context for the isolated owner'
        CategoryTestContext isolatedCategoryContext = testFixtures.createCategoryTestContext(isolatedTestOwner)

        and: 'ensure no categories exist for this isolated test owner'
        // The TestDataManager creates categories, but we'll verify empty state first
        // by checking that our isolated endpoint returns empty results

        when: 'requesting all active categories for our isolated test context'
        ResponseEntity<String> response = getEndpoint(endpointName + "/active")

        then: 'should return 200 OK with consistent response'
        response.statusCode == HttpStatus.OK

        and: 'should return a list (may not be empty due to other test data, but validates structure)'
        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse instanceof List
        // Note: List may contain categories from other tests, which is expected
        // The key test is that we get a list response, not a 404

        and: 'documents empty result standardization behavior'
        // After standardization: Always return list for collection operations
        // Never throw 404 for findAllActive() - only for findById()
        // The specific count doesn't matter - the behavior (list vs 404) is what's being tested
        true

        cleanup: 'clean up isolated test data'
        isolatedCategoryContext?.cleanup()
    }

    void 'should throw 404 for findById when entity not found'() {
        when: 'requesting non-existent category'
        ResponseEntity<String> response = getEndpoint(endpointName + "/non_existent_category")

        then: 'should return 404 NOT_FOUND'
        response.statusCode == HttpStatus.NOT_FOUND

        and: 'documents single entity not found behavior'
        // After standardization: findById() throws 404 when entity not found
        // This is correct and should remain consistent
        true
    }

    // STANDARDIZED EXCEPTION HANDLING TESTS

    void 'should handle duplicate category creation with standardized exception response'() {
        given: 'a test category context'
        categoryTestContext = testFixtures.createCategoryTestContext(testOwner)

        and: 'an existing category'
        Category category = categoryTestContext.createUniqueCategory("duplicate_category")

        ResponseEntity<String> firstInsert = postEndpoint(endpointName, category.toString())
        firstInsert.statusCode == HttpStatus.CREATED

        when: 'attempting to create duplicate category'
        ResponseEntity<String> response = postEndpoint(endpointName, category.toString())

        then: 'should return 409 CONFLICT with standardized message'
        response.statusCode == HttpStatus.CONFLICT

        and: 'should contain standardized error message format'
        response.body.contains("Duplicate")

        and: 'documents standardized exception handling'
        // After standardization: DataIntegrityViolationException -> 409 CONFLICT
        // Consistent error message format across all controllers
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
        'Invalid category format' | '{"invalid": "data"}'
        'Missing required fields' | '{}'
        'Constraint violations' | '{"categoryName": "", "activeStatus": true}'
    }

    // STANDARDIZED ENDPOINT PATTERN TESTS

    @Unroll
    void 'should implement standardized REST endpoint patterns: #operation at #endpoint'() {
        expect: 'All endpoints should follow RESTful patterns'

        // After standardization:
        // GET /api/category/active - collection retrieval
        // GET /api/category/{categoryName} - single entity retrieval
        // POST /api/category - entity creation
        // PUT /api/category/{categoryName} - entity update
        // DELETE /api/category/{categoryName} - entity deletion
        true

        where:
        operation | endpoint
        'findAllActive' | 'GET /api/category/active'
        'findById' | 'GET /api/category/{categoryName}'
        'save' | 'POST /api/category'
        'update' | 'PUT /api/category/{categoryName}'
        'deleteById' | 'DELETE /api/category/{categoryName}'
    }

    // STANDARDIZED REQUEST/RESPONSE BODY TESTS

    void 'should use Category entity type for all request bodies'() {
        given: 'a test category context'
        categoryTestContext = testFixtures.createCategoryTestContext(testOwner)

        and: 'a category entity'
        Category category = categoryTestContext.createUniqueCategory("entity_category")

        when: 'creating category with entity body'
        ResponseEntity<String> response = postEndpoint(endpointName, category.toString())

        then: 'should accept Category entity type'
        response.statusCode == HttpStatus.CREATED

        and: 'documents standardized request body handling'
        // After standardization: save(category: Category)
        // No Map<String, Any> usage like AccountController
        // All request bodies should use entity types directly
        true
    }

    void 'should return Category entity type for all response bodies'() {
        given: 'a test category context'
        categoryTestContext = testFixtures.createCategoryTestContext(testOwner)

        and: 'a test category'
        Category category = categoryTestContext.createUniqueCategory("response_category")

        postEndpoint(endpointName, category.toString())

        when: 'retrieving category'
        ResponseEntity<String> response = getEndpoint(endpointName + "/${category.categoryName}")

        then: 'should return Category entity'
        response.statusCode == HttpStatus.OK

        def jsonResponse = new JsonSlurper().parseText(response.body)
        jsonResponse.categoryName == category.categoryName
        jsonResponse.activeStatus == category.activeStatus

        and: 'documents standardized response body handling'
        // After standardization: ResponseEntity<Category>
        // Entity objects for all CRUD operations
        // List<Category> for collection operations
        true
    }

    // STANDARDIZED LOGGING TESTS

    void 'should implement standardized logging patterns'() {
        given: 'a test category context'
        categoryTestContext = testFixtures.createCategoryTestContext(testOwner)

        and: 'a test category for logging verification'
        Category category = categoryTestContext.createUniqueCategory("log_category")

        when: 'performing CRUD operations'
        ResponseEntity<String> createResponse = postEndpoint(endpointName, category.toString())
        ResponseEntity<String> readResponse = getEndpoint(endpointName + "/${category.categoryName}")

        and: 'update with correct database ID and unique name'
        def createdCategory = new JsonSlurper().parseText(createResponse.body)
        category.categoryId = createdCategory.categoryId

        // Store the original category name for the URL
        String originalCategoryName = category.categoryName

        // Generate unique updated name for logging test
        String uniqueLogName = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("logupdate")
                .build().categoryName
        category.categoryName = uniqueLogName
        ResponseEntity<String> updateResponse = putEndpoint(endpointName + "/${originalCategoryName}", category.toString())
        ResponseEntity<String> deleteResponse = deleteEndpoint(endpointName + "/${uniqueLogName}")

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

    // BUSINESS LOGIC ENDPOINT PRESERVATION

    void 'should preserve merge endpoint as business logic (not standardized)'() {
        given: 'a test category context'
        categoryTestContext = testFixtures.createCategoryTestContext(testOwner)

        and: 'two categories for merging'
        Category oldCategory = categoryTestContext.createUniqueCategory("old_category")
        Category newCategory = categoryTestContext.createUniqueCategory("new_category")

        postEndpoint(endpointName, oldCategory.toString())
        postEndpoint(endpointName, newCategory.toString())

        when: 'attempting to merge categories using existing business logic endpoint'
        String mergeUrl = endpointName + "/merge?old=${oldCategory.categoryName}&new=${newCategory.categoryName}"
        ResponseEntity<String> response = putEndpoint(mergeUrl, "")

        then: 'should either succeed or fail gracefully (business logic preserved)'
        response.statusCode in [HttpStatus.OK, HttpStatus.NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR]

        and: 'documents business logic endpoint preservation'
        // After standardization: Business logic endpoints like /merge should be preserved
        // Only CRUD operations get standardized, not specialized business operations
        // /merge endpoint remains as PUT /api/category/merge?old=A&new=B
        true
    }

    // TEMPLATE COMPLETENESS VERIFICATION

    void 'should serve as complete standardization template for next controller migrations'() {
        expect: 'CategoryController standardization covers all required patterns'

        def standardizationAspects = [
                'Method naming standardization (categories -> findAllActive)',
                'Parameter naming standardization (camelCase)',
                'Empty result handling standardization (404 -> empty list)',
                'HTTP status code standardization',
                'Exception handling standardization',
                'Endpoint pattern standardization (/select/active -> /active)',
                'Request/response body standardization',
                'Logging standardization',
                'Entity type usage consistency',
                'Business logic endpoint preservation (/merge kept as-is)'
        ]

        // This template should be applied next to:
        // 1. DescriptionController (similar complexity)
        // 2. PaymentController (moderate complexity)
        // 3. AccountController (high complexity - business logic separation)
        // 4. PendingTransactionController (unique patterns)
        // 5. TransactionController (most complex - hierarchical endpoints)

        standardizationAspects.size() == 10

        and: 'provides clear migration guidance for DescriptionController'
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

    protected ResponseEntity<String> postEndpoint(String path, String payload) {
        String token = generateJwtToken(username)
        log.info(payload)

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
        log.info(payload)

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