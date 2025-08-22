package finance.controllers

import finance.domain.Category
import finance.helpers.SmartCategoryBuilder
import finance.helpers.CategoryTestContext
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@ActiveProfiles("func")
class CategoryControllerIsolatedSpec extends BaseControllerSpec {

    @Shared
    protected String endpointName = 'category'

    @Shared
    protected CategoryTestContext categoryTestContext

    def setupSpec() {
        // Parent setupSpec() is called automatically for base data
        categoryTestContext = testFixtures.createCategoryTestContext(testOwner)
    }

    void 'should successfully insert new category with isolated test data'() {
        given:
        Category category = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("newcat")
                .buildAndValidate()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, category.toString())

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.contains(category.categoryName)
        0 * _
    }

    void 'should reject duplicate category insertion'() {
        given:
        Category category = categoryTestContext.createUniqueCategory("duplicate")

        // Insert first time
        ResponseEntity<String> firstInsert = insertEndpoint(endpointName, category.toString())

        when:
        // Try to insert same category again
        ResponseEntity<String> response = insertEndpoint(endpointName, category.toString())

        then:
        firstInsert.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'should successfully find existing test category'() {
        given:
        // Create a category in the database first to ensure it exists
        Category testCategory = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("findable")
                .buildAndValidate()

        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, testCategory.toString())

        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, testCategory.categoryName)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.OK
        response.body.contains(testCategory.categoryName)
        0 * _
    }

    void 'should return not found for non-existent category'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, "nonexistent_${testOwner}")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should successfully delete category by name'() {
        given:
        Category category = categoryTestContext.createUniqueCategory("todelete")
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, category.toString())

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, category.categoryName)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should reject category insertion with empty name'() {
        given:
        String invalidPayload = '{"categoryName":"","activeStatus":true}'

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, invalidPayload)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should successfully activate and deactivate categories'() {
        given:
        Category testCategory = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("toggle")
                .asActive()
                .buildAndValidate()

        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, testCategory.toString())

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")

        // First get the inserted category to get the real ID
        ResponseEntity<String> fetchResponse = selectEndpoint(endpointName, testCategory.categoryName)

        when:
        // Parse the fetched category to get the actual category ID
        def jsonSlurper = new groovy.json.JsonSlurper()
        def fetchedCategoryData = jsonSlurper.parseText(fetchResponse.body)

        // Create update payload with real category ID and set to inactive
        String deactivatePayload = """{"categoryId":${fetchedCategoryData.categoryId},"categoryName":"${testCategory.categoryName}","activeStatus":false,"categoryCount":${fetchedCategoryData.categoryCount ?: 0}}"""
        HttpEntity deactivateEntity = new HttpEntity<>(deactivatePayload, headers)

        ResponseEntity<String> deactivateResponse = restTemplate.exchange(
                createURLWithPort("/api/category/update/${testCategory.categoryName}"),
                HttpMethod.PUT, deactivateEntity, String)

        // Create update payload with real category ID and set to active
        String activatePayload = """{"categoryId":${fetchedCategoryData.categoryId},"categoryName":"${testCategory.categoryName}","activeStatus":true,"categoryCount":${fetchedCategoryData.categoryCount ?: 0}}"""
        HttpEntity activateEntity = new HttpEntity<>(activatePayload, headers)

        ResponseEntity<String> activateResponse = restTemplate.exchange(
                createURLWithPort("/api/category/update/${testCategory.categoryName}"),
                HttpMethod.PUT, activateEntity, String)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        fetchResponse.statusCode == HttpStatus.OK
        deactivateResponse.statusCode == HttpStatus.OK
        deactivateResponse.body.contains('"activeStatus":false')
        activateResponse.statusCode == HttpStatus.OK
        activateResponse.body.contains('"activeStatus":true')
        0 * _
    }

    void 'should successfully select all categories'() {
        given:
        // Add a few unique categories to test 'select all'
        Category cat1 = categoryTestContext.createUniqueCategory("all1")
        Category cat2 = categoryTestContext.createUniqueCategory("all2")

        insertEndpoint(endpointName, cat1.toString())
        insertEndpoint(endpointName, cat2.toString())

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/category/select/active"),
                HttpMethod.GET, entity, String)

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(cat1.categoryName)
        response.body.contains(cat2.categoryName)
        0 * _
    }

    void 'should return not found when operating on non-existent categories'() {
        given:
        String nonExistentCategory = "missing_${testOwner}"

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> deleteResponse = deleteEndpoint(endpointName, nonExistentCategory)
        ResponseEntity<String> deactivateResponse = restTemplate.exchange(
                createURLWithPort("/api/category/deactivate/${nonExistentCategory}"),
                HttpMethod.PUT, entity, String)
        ResponseEntity<String> activateResponse = restTemplate.exchange(
                createURLWithPort("/api/category/activate/${nonExistentCategory}"),
                HttpMethod.PUT, entity, String)

        then:
        deleteResponse.statusCode == HttpStatus.NOT_FOUND
        deactivateResponse.statusCode == HttpStatus.NOT_FOUND
        activateResponse.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should reject category insertion with invalid payload'() {
        given:
        String malformedPayload = '{"invalid":true}'
        String badJson = 'badJson'

        when:
        ResponseEntity<String> response1 = insertEndpoint(endpointName, malformedPayload)
        ResponseEntity<String> response2 = insertEndpoint(endpointName, badJson)

        then:
        response1.statusCode == HttpStatus.BAD_REQUEST
        response2.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should handle constraint validation for category name patterns'() {
        given:
        // Test various invalid category name patterns
        String categoryWithSpaces = '{"categoryName":"invalid category","activeStatus":true}'
        String categoryWithSpecialChars = '{"categoryName":"invalid@category","activeStatus":true}'
        String categoryTooLong = '{"categoryName":"' + 'a' * 51 + '","activeStatus":true}'  // 51 chars - exceeds 50 limit

        when:
        ResponseEntity<String> spacesResponse = insertEndpoint(endpointName, categoryWithSpaces)
        ResponseEntity<String> specialCharsResponse = insertEndpoint(endpointName, categoryWithSpecialChars)
        ResponseEntity<String> tooLongResponse = insertEndpoint(endpointName, categoryTooLong)

        then:
        spacesResponse.statusCode == HttpStatus.BAD_REQUEST
        specialCharsResponse.statusCode == HttpStatus.BAD_REQUEST
        tooLongResponse.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }
}