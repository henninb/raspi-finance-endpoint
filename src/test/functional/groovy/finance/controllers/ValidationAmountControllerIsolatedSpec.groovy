package finance.controllers

import finance.helpers.SmartAccountBuilder
import finance.helpers.SmartValidationAmountBuilder
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@ActiveProfiles("func")
class ValidationAmountControllerIsolatedSpec extends BaseControllerSpec {

    @Autowired
    protected JdbcTemplate jdbcTemplate

    void 'should successfully insert new validation amount with isolated test data'() {
        given:
        // Create a dedicated account for this specific test using SmartAccountBuilder
        def account = SmartAccountBuilder.builderForOwner(testOwner)
            .withAccountNameOwner("valtest-account_testowner")
            .asDebit()
            .buildAndValidate()

        ResponseEntity<String> accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId from the created account response
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /\"accountId\":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        // Create validation amount using SmartValidationAmountBuilder
        def validationAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountId)
            .withAmount(75.50G)
            .asCleared()
            .buildAndValidate()

        String validationAmountJson = validationAmount.toString()

        when:
        // Path var content is ignored when payload has a valid accountId
        ResponseEntity<String> response = insertValidationAmountEndpoint(validationAmountJson, testOwner)

        then:
        response.statusCode == HttpStatus.OK
        // Jackson may serialize 75.50 as 75.5 — assert accordingly
        response.body.contains('\"amount\":75.5') || response.body.contains('\"amount\":75.50')
        response.body.contains('\"activeStatus\":true')
        0 * _
    }

    void 'should successfully handle different transaction states'() {
        given:
        def account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("valstate")
            .asDebit()
            .buildAndValidate()
        ResponseEntity<String> accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED
        String accountIdStr = (accountResponse.body =~ /\"accountId\":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)
        String accountNameOwner = account.accountNameOwner

        def clearedAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountId)
            .asCleared()
            .withAmount(100.00G)
            .buildAndValidate()

        def outstandingAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountId)
            .asOutstanding()
            .withAmount(200.00G)
            .buildAndValidate()

        when:
        ResponseEntity<String> clearedResponse = insertValidationAmountEndpoint(clearedAmount.toString(), accountNameOwner)
        ResponseEntity<String> outstandingResponse = insertValidationAmountEndpoint(outstandingAmount.toString(), accountNameOwner)

        then:
        clearedResponse.statusCode == HttpStatus.OK
        outstandingResponse.statusCode == HttpStatus.OK
        clearedResponse.body.contains('\"transactionState\":\"cleared\"')
        outstandingResponse.body.contains('\"transactionState\":\"outstanding\"')
        0 * _
    }

    void 'should successfully handle active and inactive validation amounts'() {
        given:
        def account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("valstatus")
            .asDebit()
            .buildAndValidate()
        ResponseEntity<String> accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED
        String accountIdStr = (accountResponse.body =~ /\"accountId\":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)
        String accountNameOwner = account.accountNameOwner

        def activeAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountId)
            .asCleared()
            .asActive()
            .withAmount(150.00G)
            .buildAndValidate()

        def inactiveAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountId)
            .asCleared()
            .asInactive()
            .withAmount(250.00G)
            .buildAndValidate()

        when:
        ResponseEntity<String> activeResponse = insertValidationAmountEndpoint(activeAmount.toString(), accountNameOwner)
        ResponseEntity<String> inactiveResponse = insertValidationAmountEndpoint(inactiveAmount.toString(), accountNameOwner)

        then:
        activeResponse.statusCode == HttpStatus.OK
        inactiveResponse.statusCode == HttpStatus.OK
        activeResponse.body.contains('\"activeStatus\":true')
        inactiveResponse.body.contains('\"activeStatus\":false')
        0 * _
    }

    void 'should successfully handle different amount ranges'() {
        given:
        def account = SmartAccountBuilder.builderForOwner(testOwner)
            .withUniqueAccountName("valrange")
            .asDebit()
            .buildAndValidate()
        ResponseEntity<String> accountResponse = insertEndpoint('account', account.toString())
        assert accountResponse.statusCode == HttpStatus.CREATED
        String accountIdStr = (accountResponse.body =~ /\"accountId\":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)
        String accountNameOwner = account.accountNameOwner

        def smallAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountId)
            .asCleared()
            .withAmount(5.00G)
            .buildAndValidate()

        def largeAmount = SmartValidationAmountBuilder.builderForOwner(testOwner)
            .withAccountId(accountId)
            .asCleared()
            .withAmount(9999.99G)
            .buildAndValidate()

        when:
        ResponseEntity<String> smallResponse = insertValidationAmountEndpoint(smallAmount.toString(), accountNameOwner)
        ResponseEntity<String> largeResponse = insertValidationAmountEndpoint(largeAmount.toString(), accountNameOwner)

        then:
        smallResponse.statusCode == HttpStatus.OK
        largeResponse.statusCode == HttpStatus.OK
        // Jackson serializes BigDecimal(5.00) as 5.0 in this project
        smallResponse.body.contains('\"amount\":5.0')
        largeResponse.body.contains('\"amount\":9999.99')
        0 * _
    }

    void 'should reject validation amount with invalid amount precision'() {
        given:
        String invalidAmountJson = """
        {
            \"validationId\": 0,
            \"accountId\": 1,
            \"validationDate\": \"2024-01-01T10:00:00.000Z\",
            \"activeStatus\": true,
            \"transactionState\": \"Cleared\",
            \"amount\": 25.123456
        }
        """

        when:
        ResponseEntity<String> response = insertValidationAmountEndpoint(invalidAmountJson, testOwner)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should reject validation amount with invalid transaction state'() {
        given:
        String invalidStateJson = """
        {
            \"validationId\": 0,
            \"accountId\": 1,
            \"validationDate\": \"2024-01-01T10:00:00.000Z\",
            \"activeStatus\": true,
            \"transactionState\": \"INVALID_STATE\",
            \"amount\": 100.00
        }
        """

        when:
        ResponseEntity<String> response = insertValidationAmountEndpoint(invalidStateJson, testOwner)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should reject validation amount with malformed JSON payload'() {
        given:
        String malformedPayload = '{"invalid":true}'
        String badJson = 'badJson'

        when:
        ResponseEntity<String> response1 = insertValidationAmountEndpoint(malformedPayload, testOwner)
        ResponseEntity<String> response2 = insertValidationAmountEndpoint(badJson, testOwner)

        then:
        response1.statusCode == HttpStatus.BAD_REQUEST
        response2.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    // Helper methods specific to ValidationAmount controller endpoints
    private ResponseEntity<String> insertValidationAmountEndpoint(String jsonPayload, String accountNameOwner) {
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")

        HttpEntity entity = new HttpEntity<>(jsonPayload, headers)

        return restTemplate.exchange(
            createURLWithPort("/api/validation/amount/insert/${accountNameOwner}"),
            HttpMethod.POST, entity, String
        )
    }

    private String primaryAccountNameForTestOwner() {
        String ownerClean = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (ownerClean.isEmpty()) ownerClean = 'testowner'
        return "primary_${ownerClean}".toLowerCase()
    }
}
