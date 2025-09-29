package finance.controllers

import finance.domain.Description
import finance.helpers.SmartDescriptionBuilder
import finance.helpers.DescriptionTestContext
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@ActiveProfiles("func")
class DescriptionControllerFunctionalSpec extends BaseControllerFunctionalSpec {

    @Shared
    protected String endpointName = 'description'

    @Shared
    protected DescriptionTestContext descriptionTestContext

    def setupSpec() {
        // Parent setupSpec() is called automatically for base data
        descriptionTestContext = testFixtures.createDescriptionTestContext(testOwner)
    }

    void 'should successfully insert new description with isolated test data'() {
        given:
        Description description = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName("newdesc")
                .buildAndValidate()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, description.toString())

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.contains(description.descriptionName)
        0 * _
    }

    void 'should reject duplicate description insertion'() {
        given:
        Description description = descriptionTestContext.createUniqueDescription("duplicate")

        // Insert first time
        ResponseEntity<String> firstInsert = insertEndpoint(endpointName, description.toString())

        when:
        // Try to insert same description again
        ResponseEntity<String> response = insertEndpoint(endpointName, description.toString())

        then:
        firstInsert.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'should successfully find existing test description'() {
        given:
        // Create a description in the database first to ensure it exists
        Description testDescription = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName("findable")
                .buildAndValidate()

        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, testDescription.toString())

        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, testDescription.descriptionName)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.OK
        response.body.contains(testDescription.descriptionName)
        0 * _
    }

    void 'should return not found for non-existent description'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, "nonexistent_${testOwner}")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should successfully delete description by name'() {
        given:
        Description description = descriptionTestContext.createUniqueDescription("todelete")
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, description.toString())

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, description.descriptionName)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should reject description insertion with empty name'() {
        given:
        String invalidPayload = '{"descriptionName":"","activeStatus":true}'

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, invalidPayload)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should successfully handle business type descriptions'() {
        given:
        Description storeDesc = descriptionTestContext.createStoreDescription()
        Description restaurantDesc = descriptionTestContext.createRestaurantDescription()
        Description serviceDesc = descriptionTestContext.createServiceDescription()
        Description onlineDesc = descriptionTestContext.createOnlineDescription()

        when:
        ResponseEntity<String> storeResponse = insertEndpoint(endpointName, storeDesc.toString())
        ResponseEntity<String> restaurantResponse = insertEndpoint(endpointName, restaurantDesc.toString())
        ResponseEntity<String> serviceResponse = insertEndpoint(endpointName, serviceDesc.toString())
        ResponseEntity<String> onlineResponse = insertEndpoint(endpointName, onlineDesc.toString())

        then:
        storeResponse.statusCode == HttpStatus.CREATED
        restaurantResponse.statusCode == HttpStatus.CREATED
        serviceResponse.statusCode == HttpStatus.CREATED
        onlineResponse.statusCode == HttpStatus.CREATED

        storeResponse.body.contains(storeDesc.descriptionName)
        restaurantResponse.body.contains(restaurantDesc.descriptionName)
        serviceResponse.body.contains(serviceDesc.descriptionName)
        onlineResponse.body.contains(onlineDesc.descriptionName)
        0 * _
    }

    void 'should successfully select all descriptions'() {
        given:
        // Add a few unique descriptions to test 'select all'
        Description desc1 = descriptionTestContext.createUniqueDescription("all1")
        Description desc2 = descriptionTestContext.createUniqueDescription("all2")

        insertEndpoint(endpointName, desc1.toString())
        insertEndpoint(endpointName, desc2.toString())

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/description/select/active"),
                HttpMethod.GET, entity, String)

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(desc1.descriptionName)
        response.body.contains(desc2.descriptionName)
        0 * _
    }

    void 'should return not found when operating on non-existent descriptions'() {
        given:
        String nonExistentDescription = "missing_${testOwner}"

        when:
        ResponseEntity<String> deleteResponse = deleteEndpoint(endpointName, nonExistentDescription)
        ResponseEntity<String> selectResponse = selectEndpoint(endpointName, nonExistentDescription)

        then:
        deleteResponse.statusCode == HttpStatus.NOT_FOUND
        selectResponse.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should reject description insertion with invalid payload'() {
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

    void 'should handle constraint validation for description length'() {
        given:
        // Test description name too long (51 chars - exceeds 50 limit)
        String descriptionTooLong = '{"descriptionName":"' + 'a' * 51 + '","activeStatus":true}'

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, descriptionTooLong)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should successfully handle active and inactive descriptions'() {
        given:
        Description activeDesc = descriptionTestContext.createActiveDescription("active_desc_${testOwner}")
        Description inactiveDesc = descriptionTestContext.createInactiveDescription("inactive_desc_${testOwner}")

        when:
        ResponseEntity<String> activeResponse = insertEndpoint(endpointName, activeDesc.toString())
        ResponseEntity<String> inactiveResponse = insertEndpoint(endpointName, inactiveDesc.toString())

        then:
        activeResponse.statusCode == HttpStatus.CREATED
        inactiveResponse.statusCode == HttpStatus.CREATED
        activeResponse.body.contains('"activeStatus":true')
        inactiveResponse.body.contains('"activeStatus":false')
        0 * _
    }

    void 'should successfully handle custom business descriptions'() {
        given:
        String businessType1 = "pharmacy"
        String businessType2 = "gas_station"

        Description customDesc1 = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withBusinessDescription(businessType1)
                .buildAndValidate()
        Description customDesc2 = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withBusinessDescription(businessType2)
                .buildAndValidate()

        when:
        ResponseEntity<String> response1 = insertEndpoint(endpointName, customDesc1.toString())
        ResponseEntity<String> response2 = insertEndpoint(endpointName, customDesc2.toString())

        then:
        response1.statusCode == HttpStatus.CREATED
        response2.statusCode == HttpStatus.CREATED
        response1.body.contains(customDesc1.descriptionName)
        response2.body.contains(customDesc2.descriptionName)
        0 * _
    }

    void 'should complete full description lifecycle - insert, find, delete'() {
        given:
        Description description = descriptionTestContext.createUniqueDescription("lifecycle")

        when: 'insert description'
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, description.toString())

        then: 'insert succeeds'
        insertResponse.statusCode == HttpStatus.CREATED
        insertResponse.body.contains(description.descriptionName)
        0 * _

        when: 'find description'
        ResponseEntity<String> findResponse = selectEndpoint(endpointName, description.descriptionName)

        then: 'find succeeds'
        findResponse.statusCode == HttpStatus.OK
        findResponse.body.contains(description.descriptionName)
        0 * _

        when: 'attempt duplicate insert'
        ResponseEntity<String> duplicateResponse = insertEndpoint(endpointName, description.toString())

        then: 'duplicate is rejected'
        duplicateResponse.statusCode == HttpStatus.CONFLICT
        0 * _

        when: 'delete description'
        ResponseEntity<String> deleteResponse = deleteEndpoint(endpointName, description.descriptionName)

        then: 'delete succeeds'
        deleteResponse.statusCode == HttpStatus.OK
        0 * _

        when: 'find after delete'
        ResponseEntity<String> findAfterDeleteResponse = selectEndpoint(endpointName, description.descriptionName)

        then: 'description not found after delete'
        findAfterDeleteResponse.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should handle description count functionality'() {
        given:
        // Create multiple descriptions to test count feature
        List<Description> descriptions = (1..3).collect { i ->
            descriptionTestContext.createUniqueDescription("count${i}")
        }

        // Insert all descriptions
        descriptions.each { desc ->
            insertEndpoint(endpointName, desc.toString())
        }

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when: 'select all descriptions to check count'
        ResponseEntity<String> selectAllResponse = restTemplate.exchange(
                createURLWithPort("/api/description/select/active"),
                HttpMethod.GET, entity, String)

        then: 'response includes descriptions with count information'
        selectAllResponse.statusCode == HttpStatus.OK
        descriptions.each { desc ->
            assert selectAllResponse.body.contains(desc.descriptionName)
        }
        0 * _
    }


    void 'should merge descriptions and move transactions, deactivating sources'() {
        given:
        String src1 = "src1_${testOwner}".toLowerCase()
        String src2 = "src2_${testOwner}".toLowerCase()
        String target = "target_${testOwner}".toLowerCase()

        // Create two transactions referencing src1 and src2
        def t1 = finance.helpers.SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueAccountName('primary')
                .withDescription(src1)
                .buildAndValidate()
        def t2 = finance.helpers.SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueAccountName('primary')
                .withDescription(src2)
                .buildAndValidate()

        when: 'insert transactions'
        ResponseEntity<String> r1 = insertEndpoint('transaction', t1.toString())
        ResponseEntity<String> r2 = insertEndpoint('transaction', t2.toString())

        then:
        r1.statusCode == HttpStatus.CREATED
        r2.statusCode == HttpStatus.CREATED

        and: 'create target description'
        Description targetDesc = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withDescriptionName(target)
                .buildAndValidate()
        insertEndpoint(endpointName, targetDesc.toString())

        when: 'call merge endpoint'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        def mergeRequest = new finance.domain.MergeDescriptionsRequest([src1, src2], target)
        HttpEntity entity = new HttpEntity<>(mergeRequest.toString(), headers)
        ResponseEntity<String> mergeResponse = restTemplate.exchange(
                createURLWithPort('/api/description/merge'), HttpMethod.POST, entity, String)

        then: 'merge succeeded'
        mergeResponse.statusCode == HttpStatus.OK
        mergeResponse.body.contains(target)

        when: 'fetch transactions by new description and old ones'
        ResponseEntity<String> targetTx = restTemplate.exchange(
                createURLWithPort("/api/transaction/description/${target}"),
                HttpMethod.GET, new HttpEntity<>(null, headers), String)
        ResponseEntity<String> src1Tx = restTemplate.exchange(
                createURLWithPort("/api/transaction/description/${src1}"),
                HttpMethod.GET, new HttpEntity<>(null, headers), String)
        ResponseEntity<String> src2Tx = restTemplate.exchange(
                createURLWithPort("/api/transaction/description/${src2}"),
                HttpMethod.GET, new HttpEntity<>(null, headers), String)

        then: 'transactions moved to target; sources empty'
        targetTx.statusCode == HttpStatus.OK
        targetTx.body.contains(target)
        src1Tx.statusCode == HttpStatus.OK
        !src1Tx.body.contains(src1)
        src2Tx.statusCode == HttpStatus.OK
        !src2Tx.body.contains(src2)

        when: 'sources are deactivated'
        ResponseEntity<String> src1Desc = selectEndpoint('description', src1)
        ResponseEntity<String> src2Desc = selectEndpoint('description', src2)

        then:
        src1Desc.statusCode == HttpStatus.OK
        src1Desc.body.contains('"activeStatus":false')
        src2Desc.statusCode == HttpStatus.OK
        src2Desc.body.contains('"activeStatus":false')
        0 * _
    }

    void 'should normalize names and skip self-merge when target equals a source'() {
        given:
        String targetRaw = " Amazon "
        String srcSame = "AMAZON"
        String srcOther = "b_${testOwner}".toLowerCase()
        String target = "amazon"

        // Create one transaction referencing srcOther
        def t = finance.helpers.SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueAccountName('primary')
                .withDescription(srcOther)
                .buildAndValidate()

        when:
        ResponseEntity<String> r = insertEndpoint('transaction', t.toString())

        then:
        r.statusCode == HttpStatus.CREATED

        and: 'create target description with normalized name'
        Description targetDescription = SmartDescriptionBuilder.builderForOwner(testOwner)
                .withDescriptionName(target)
                .buildAndValidate()
        insertEndpoint(endpointName, targetDescription.toString())

        when: 'call merge with normalization'
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        def mergeRequest = new finance.domain.MergeDescriptionsRequest([srcSame, srcOther], targetRaw)
        HttpEntity entity = new HttpEntity<>(mergeRequest.toString(), headers)
        ResponseEntity<String> mergeResponse = restTemplate.exchange(
                createURLWithPort('/api/description/merge'), HttpMethod.POST, entity, String)

        then:
        mergeResponse.statusCode == HttpStatus.OK
        mergeResponse.body.contains(target)

        when: 'transactions moved to normalized target; srcOther now empty; self-merge skipped implicitly'
        ResponseEntity<String> targetTx = restTemplate.exchange(
                createURLWithPort("/api/transaction/description/${target}"),
                HttpMethod.GET, new HttpEntity<>(null, headers), String)
        ResponseEntity<String> srcOtherTx = restTemplate.exchange(
                createURLWithPort("/api/transaction/description/${srcOther}"),
                HttpMethod.GET, new HttpEntity<>(null, headers), String)

        then:
        targetTx.statusCode == HttpStatus.OK
        targetTx.body.contains(target)
        srcOtherTx.statusCode == HttpStatus.OK
        !srcOtherTx.body.contains(srcOther)
        0 * _
    }

}
