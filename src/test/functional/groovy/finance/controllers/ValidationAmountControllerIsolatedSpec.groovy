package finance.controllers

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
        // Create a dedicated account for this specific test
        // Use pattern ^[a-z-]*_[a-z]*$ - lowercase letters/hyphens_lowercase letters
        String accountJson = """
        {
            "accountId": 0,
            "accountNameOwner": "valtest-account_testowner",
            "accountType": "debit",
            "activeStatus": true,
            "moniker": "0000",
            "outstanding": 0.00,
            "future": 0.00,
            "cleared": 0.00,
            "dateClosed": "1970-01-01T00:00:00.000Z",
            "validationDate": "2024-01-01T10:00:00.000Z"
        }
        """

        ResponseEntity<String> accountResponse = insertEndpoint('account', accountJson)
        assert accountResponse.statusCode == HttpStatus.CREATED

        // Extract accountId from the created account response
        String accountBody = accountResponse.body
        String accountIdStr = (accountBody =~ /"accountId":(\d+)/)[0][1]
        Long accountId = Long.parseLong(accountIdStr)

        String validationAmountJson = """
        {
            "validationId": 0,
            "accountId": ${accountId},
            "validationDate": "2024-01-01T10:00:00.000Z",
            "activeStatus": true,
            "transactionState": "Cleared",
            "amount": 75.50
        }
        """

        when:
        ResponseEntity<String> response = insertValidationAmountEndpoint(validationAmountJson, testOwner)

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains('"amount":75.50')
        response.body.contains('"activeStatus":true')
        0 * _
    }

    void 'should successfully handle different transaction states'() {
        given:
        String clearedAmountJson = """
        {
            "validationId": 0,
            "accountId": 1,
            "validationDate": "2024-01-01T10:00:00.000Z",
            "activeStatus": true,
            "transactionState": "Cleared",
            "amount": 100.00
        }
        """

        String outstandingAmountJson = """
        {
            "validationId": 0,
            "accountId": 1,
            "validationDate": "2024-01-01T10:00:00.000Z",
            "activeStatus": true,
            "transactionState": "Outstanding",
            "amount": 200.00
        }
        """

        when:
        ResponseEntity<String> clearedResponse = insertValidationAmountEndpoint(clearedAmountJson, testOwner)
        ResponseEntity<String> outstandingResponse = insertValidationAmountEndpoint(outstandingAmountJson, testOwner)

        then:
        clearedResponse.statusCode == HttpStatus.OK
        outstandingResponse.statusCode == HttpStatus.OK
        clearedResponse.body.contains('"transactionState":"cleared"')
        outstandingResponse.body.contains('"transactionState":"outstanding"')
        0 * _
    }

    void 'should successfully handle active and inactive validation amounts'() {
        given:
        String activeAmountJson = """
        {
            "validationId": 0,
            "accountId": 1,
            "validationDate": "2024-01-01T10:00:00.000Z",
            "activeStatus": true,
            "transactionState": "Cleared",
            "amount": 150.00
        }
        """

        String inactiveAmountJson = """
        {
            "validationId": 0,
            "accountId": 1,
            "validationDate": "2024-01-01T10:00:00.000Z",
            "activeStatus": false,
            "transactionState": "Cleared",
            "amount": 250.00
        }
        """

        when:
        ResponseEntity<String> activeResponse = insertValidationAmountEndpoint(activeAmountJson, testOwner)
        ResponseEntity<String> inactiveResponse = insertValidationAmountEndpoint(inactiveAmountJson, testOwner)

        then:
        activeResponse.statusCode == HttpStatus.OK
        inactiveResponse.statusCode == HttpStatus.OK
        activeResponse.body.contains('"activeStatus":true')
        inactiveResponse.body.contains('"activeStatus":false')
        0 * _
    }

    void 'should successfully handle different amount ranges'() {
        given:
        String smallAmountJson = """
        {
            "validationId": 0,
            "accountId": 1,
            "validationDate": "2024-01-01T10:00:00.000Z",
            "activeStatus": true,
            "transactionState": "Cleared",
            "amount": 5.00
        }
        """

        String largeAmountJson = """
        {
            "validationId": 0,
            "accountId": 1,
            "validationDate": "2024-01-01T10:00:00.000Z",
            "activeStatus": true,
            "transactionState": "Cleared",
            "amount": 9999.99
        }
        """

        when:
        ResponseEntity<String> smallResponse = insertValidationAmountEndpoint(smallAmountJson, testOwner)
        ResponseEntity<String> largeResponse = insertValidationAmountEndpoint(largeAmountJson, testOwner)

        then:
        smallResponse.statusCode == HttpStatus.OK
        largeResponse.statusCode == HttpStatus.OK
        smallResponse.body.contains('"amount":5.00')
        largeResponse.body.contains('"amount":9999.99')
        0 * _
    }

    void 'should reject validation amount with invalid amount precision'() {
        given:
        String invalidAmountJson = """
        {
            "validationId": 0,
            "accountId": 1,
            "validationDate": "2024-01-01T10:00:00.000Z",
            "activeStatus": true,
            "transactionState": "Cleared",
            "amount": 25.123456
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
            "validationId": 0,
            "accountId": 1,
            "validationDate": "2024-01-01T10:00:00.000Z",
            "activeStatus": true,
            "transactionState": "INVALID_STATE",
            "amount": 100.00
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
}
