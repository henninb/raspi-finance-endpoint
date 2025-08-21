package finance.controllers

import finance.domain.Parameter
import finance.helpers.SmartParameterBuilder
import finance.helpers.ParameterTestContext
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@ActiveProfiles("func")
class ParameterControllerIsolatedSpec extends BaseControllerSpec {

    @Shared
    protected String endpointName = 'parm'  // Note: matches original spec

    @Shared
    protected ParameterTestContext parameterTestContext

    def setupSpec() {
        // Parent setupSpec() is called automatically for base data
        parameterTestContext = testFixtures.createParameterTestContext(testOwner)
    }

    void 'should successfully insert new parameter with isolated test data'() {
        given:
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("newparam")
                .withUniqueParameterValue("newvalue")
                .buildAndValidate()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, parameter.toString())

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.contains(parameter.parameterValue)
        0 * _
    }

    void 'should reject duplicate parameter insertion'() {
        given:
        Parameter parameter = parameterTestContext.createUniqueParameter("duplicate", "dupvalue")

        // Insert first time
        ResponseEntity<String> firstInsert = insertEndpoint(endpointName, parameter.toString())

        when:
        // Try to insert same parameter again
        ResponseEntity<String> response = insertEndpoint(endpointName, parameter.toString())

        then:
        firstInsert.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'should successfully find existing test parameter'() {
        given:
        // Create a parameter in the database first to ensure it exists
        Parameter testParameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("findable")
                .withUniqueParameterValue("findablevalue")
                .buildAndValidate()
        
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, testParameter.toString())

        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, testParameter.parameterName)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.OK
        response.body.contains(testParameter.parameterValue)
        0 * _
    }

    void 'should return not found for non-existent parameter'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, "nonexistent_${testOwner}")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should successfully delete parameter by name'() {
        given:
        Parameter parameter = parameterTestContext.createUniqueParameter("todelete", "deletevalue")
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, parameter.toString())

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, parameter.parameterName)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should reject parameter insertion with empty name'() {
        given:
        String invalidPayload = '{"parameterName":"","parameterValue":"somevalue","activeStatus":true}'

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, invalidPayload)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should reject parameter insertion with empty value'() {
        given:
        String invalidPayload = '{"parameterName":"somename","parameterValue":"","activeStatus":true}'

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, invalidPayload)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should successfully handle payment account parameter scenario'() {
        given:
        Parameter paymentParam = parameterTestContext.createPaymentAccountParameter()

        when:
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, paymentParam.toString())
        ResponseEntity<String> selectResponse = selectEndpoint(endpointName, paymentParam.parameterName)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        selectResponse.statusCode == HttpStatus.OK
        selectResponse.body.contains(paymentParam.parameterValue)
        0 * _
    }

    void 'should successfully handle config parameter scenarios'() {
        given:
        Parameter configParam1 = parameterTestContext.createConfigParameter("database")
        Parameter configParam2 = parameterTestContext.createConfigParameter("cache")

        when:
        ResponseEntity<String> insertResponse1 = insertEndpoint(endpointName, configParam1.toString())
        ResponseEntity<String> insertResponse2 = insertEndpoint(endpointName, configParam2.toString())

        then:
        insertResponse1.statusCode == HttpStatus.CREATED
        insertResponse2.statusCode == HttpStatus.CREATED
        insertResponse1.body.contains(configParam1.parameterValue)
        insertResponse2.body.contains(configParam2.parameterValue)
        0 * _
    }

    void 'should successfully select all parameters'() {
        given:
        // Add a few unique parameters to test 'select all'
        Parameter param1 = parameterTestContext.createUniqueParameter("all1", "value1")
        Parameter param2 = parameterTestContext.createUniqueParameter("all2", "value2")

        insertEndpoint(endpointName, param1.toString())
        insertEndpoint(endpointName, param2.toString())

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/parm/select/active"),
                HttpMethod.GET, entity, String)

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(param1.parameterValue)
        response.body.contains(param2.parameterValue)
        0 * _
    }

    void 'should return not found when operating on non-existent parameters'() {
        given:
        String nonExistentParameter = "missing_${testOwner}"

        when:
        ResponseEntity<String> deleteResponse = deleteEndpoint(endpointName, nonExistentParameter)
        ResponseEntity<String> selectResponse = selectEndpoint(endpointName, nonExistentParameter)

        then:
        deleteResponse.statusCode == HttpStatus.NOT_FOUND
        selectResponse.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should reject parameter insertion with invalid payload'() {
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

    void 'should handle constraint validation for parameter length'() {
        given:
        // Test various invalid parameter patterns
        String parameterNameTooLong = '{"parameterName":"' + 'a' * 51 + '","parameterValue":"value","activeStatus":true}'  // 51 chars - exceeds 50 limit
        String parameterValueTooLong = '{"parameterName":"name","parameterValue":"' + 'b' * 51 + '","activeStatus":true}'  // 51 chars - exceeds 50 limit

        when:
        ResponseEntity<String> nameResponse = insertEndpoint(endpointName, parameterNameTooLong)
        ResponseEntity<String> valueResponse = insertEndpoint(endpointName, parameterValueTooLong)

        then:
        nameResponse.statusCode == HttpStatus.BAD_REQUEST
        valueResponse.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should successfully handle active and inactive parameters'() {
        given:
        Parameter activeParam = parameterTestContext.createActiveParameter("active_param_${testOwner}", "active_value")
        Parameter inactiveParam = parameterTestContext.createInactiveParameter("inactive_param_${testOwner}", "inactive_value")

        when:
        ResponseEntity<String> activeResponse = insertEndpoint(endpointName, activeParam.toString())
        ResponseEntity<String> inactiveResponse = insertEndpoint(endpointName, inactiveParam.toString())

        then:
        activeResponse.statusCode == HttpStatus.CREATED
        inactiveResponse.statusCode == HttpStatus.CREATED
        activeResponse.body.contains('"activeStatus":true')
        inactiveResponse.body.contains('"activeStatus":false')
        0 * _
    }

    void 'should complete full parameter lifecycle - insert, find, delete'() {
        given:
        Parameter parameter = parameterTestContext.createUniqueParameter("lifecycle", "lifecyclevalue")

        when: 'insert parameter'
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, parameter.toString())

        then: 'insert succeeds'
        insertResponse.statusCode == HttpStatus.CREATED
        insertResponse.body.contains(parameter.parameterValue)
        0 * _

        when: 'find parameter'
        ResponseEntity<String> findResponse = selectEndpoint(endpointName, parameter.parameterName)

        then: 'find succeeds'
        findResponse.statusCode == HttpStatus.OK
        findResponse.body.contains(parameter.parameterValue)
        0 * _

        when: 'attempt duplicate insert'
        ResponseEntity<String> duplicateResponse = insertEndpoint(endpointName, parameter.toString())

        then: 'duplicate is rejected'
        duplicateResponse.statusCode == HttpStatus.CONFLICT
        0 * _

        when: 'delete parameter'
        ResponseEntity<String> deleteResponse = deleteEndpoint(endpointName, parameter.parameterName)

        then: 'delete succeeds'
        deleteResponse.statusCode == HttpStatus.OK
        0 * _

        when: 'find after delete'
        ResponseEntity<String> findAfterDeleteResponse = selectEndpoint(endpointName, parameter.parameterName)

        then: 'parameter not found after delete'
        findAfterDeleteResponse.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }
}
