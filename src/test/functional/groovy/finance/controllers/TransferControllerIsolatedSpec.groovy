package finance.controllers

import finance.domain.Transfer
import finance.helpers.SmartTransferBuilder
import finance.helpers.SmartAccountBuilder
import finance.helpers.TransferTestContext
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@ActiveProfiles("func")
class TransferControllerIsolatedSpec extends BaseControllerSpec {

    @Shared
    protected String endpointName = 'transfer'

    @Shared
    protected TransferTestContext transferTestContext

    def setupSpec() {
        // Parent setupSpec() is called automatically for base data
        transferTestContext = testFixtures.createTransferContext(testOwner)
    }

    void 'should successfully insert new transfer with isolated test data'() {
        given:
        // Use pattern-compliant account names matching TestDataManager logic
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = "testowner"

        String sourceAccountName = "primary_${cleanOwner}".toLowerCase()
        String destAccountName = "secondary_${cleanOwner}".toLowerCase()

        // Create source account first
        String sourceAccountJson = """
        {
            "accountId": 0,
            "accountNameOwner": "${sourceAccountName}",
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

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccountJson)
        assert sourceAccountResponse.statusCode == HttpStatus.CREATED || sourceAccountResponse.statusCode == HttpStatus.CONFLICT

        // Create destination account
        String destAccountJson = """
        {
            "accountId": 0,
            "accountNameOwner": "${destAccountName}",
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

        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccountJson)
        assert destAccountResponse.statusCode == HttpStatus.CREATED || destAccountResponse.statusCode == HttpStatus.CONFLICT

        // Create transfer JSON with valid UUIDs for both transactions
        String transferJson = """
        {
            "transferId": 0,
            "sourceAccount": "${sourceAccountName}",
            "destinationAccount": "${destAccountName}",
            "transactionDate": "2023-01-01",
            "amount": 150.75,
            "guidSource": "ba665bc2-22b6-4123-a566-6f5ab3d796d1",
            "guidDestination": "ba665bc2-22b6-4123-a566-6f5ab3d796d2",
            "activeStatus": true
        }
        """

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, transferJson)

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains('"sourceAccount":"' + sourceAccountName + '"')
        response.body.contains('"destinationAccount":"' + destAccountName + '"')
        response.body.contains('"amount":150.75')
        0 * _
    }

    void 'should successfully handle different transfer amounts'() {
        given:
        // Use pattern-compliant account names
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = "testowner"

        String sourceAccountName = "amountsrc_${cleanOwner}".toLowerCase()
        String destAccountName = "amountdest_${cleanOwner}".toLowerCase()

        // Create accounts first
        String sourceAccountJson = """
        {
            "accountId": 0,
            "accountNameOwner": "${sourceAccountName}",
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

        String destAccountJson = """
        {
            "accountId": 0,
            "accountNameOwner": "${destAccountName}",
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

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccountJson)
        assert sourceAccountResponse.statusCode == HttpStatus.CREATED || sourceAccountResponse.statusCode == HttpStatus.CONFLICT

        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccountJson)
        assert destAccountResponse.statusCode == HttpStatus.CREATED || destAccountResponse.statusCode == HttpStatus.CONFLICT

        // Small transfer
        String transferSmallJson = """
        {
            "transferId": 0,
            "sourceAccount": "${sourceAccountName}",
            "destinationAccount": "${destAccountName}",
            "transactionDate": "2023-01-01",
            "amount": 25.50,
            "guidSource": "ba665bc2-22b6-4123-a566-6f5ab3d796d3",
            "guidDestination": "ba665bc2-22b6-4123-a566-6f5ab3d796d4",
            "activeStatus": true
        }
        """

        // Large transfer
        String transferLargeJson = """
        {
            "transferId": 0,
            "sourceAccount": "${sourceAccountName}",
            "destinationAccount": "${destAccountName}",
            "transactionDate": "2023-01-02",
            "amount": 999.99,
            "guidSource": "ba665bc2-22b6-4123-a566-6f5ab3d796d5",
            "guidDestination": "ba665bc2-22b6-4123-a566-6f5ab3d796d6",
            "activeStatus": true
        }
        """

        when:
        ResponseEntity<String> smallResponse = insertEndpoint(endpointName, transferSmallJson)
        ResponseEntity<String> largeResponse = insertEndpoint(endpointName, transferLargeJson)

        then:
        smallResponse.statusCode == HttpStatus.OK
        smallResponse.body.contains('"amount":25.5')
        largeResponse.statusCode == HttpStatus.OK
        largeResponse.body.contains('"amount":999.99')
        0 * _
    }

    void 'should successfully handle active and inactive transfers'() {
        given:
        // Use pattern-compliant account names
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = "testowner"

        String sourceAccountName = "statussrc_${cleanOwner}".toLowerCase()
        String destAccountName = "statusdest_${cleanOwner}".toLowerCase()

        // Create accounts first
        String sourceAccountJson = """
        {
            "accountId": 0,
            "accountNameOwner": "${sourceAccountName}",
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

        String destAccountJson = """
        {
            "accountId": 0,
            "accountNameOwner": "${destAccountName}",
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

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccountJson)
        assert sourceAccountResponse.statusCode == HttpStatus.CREATED || sourceAccountResponse.statusCode == HttpStatus.CONFLICT

        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccountJson)
        assert destAccountResponse.statusCode == HttpStatus.CREATED || destAccountResponse.statusCode == HttpStatus.CONFLICT

        // Active transfer
        String activeTransferJson = """
        {
            "transferId": 0,
            "sourceAccount": "${sourceAccountName}",
            "destinationAccount": "${destAccountName}",
            "transactionDate": "2023-01-03",
            "amount": 200.00,
            "guidSource": "ba665bc2-22b6-4123-a566-6f5ab3d796d7",
            "guidDestination": "ba665bc2-22b6-4123-a566-6f5ab3d796d8",
            "activeStatus": true
        }
        """

        // Inactive transfer
        String inactiveTransferJson = """
        {
            "transferId": 0,
            "sourceAccount": "${sourceAccountName}",
            "destinationAccount": "${destAccountName}",
            "transactionDate": "2023-01-04",
            "amount": 300.00,
            "guidSource": "ba665bc2-22b6-4123-a566-6f5ab3d796d9",
            "guidDestination": "ba665bc2-22b6-4123-a566-6f5ab3d79600",
            "activeStatus": false
        }
        """

        when:
        ResponseEntity<String> activeResponse = insertEndpoint(endpointName, activeTransferJson)
        ResponseEntity<String> inactiveResponse = insertEndpoint(endpointName, inactiveTransferJson)

        then:
        activeResponse.statusCode == HttpStatus.OK
        activeResponse.body.contains('"activeStatus":true')
        inactiveResponse.statusCode == HttpStatus.OK
        inactiveResponse.body.contains('"activeStatus":false')
        0 * _
    }

    void 'should successfully select all transfers'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/transfer/select"),
                HttpMethod.GET, entity, String)

        then:
        response.statusCode == HttpStatus.OK
        response.body.startsWith('[') // Should be a JSON array
        0 * _
    }

    void 'should reject transfer with invalid JSON payload'() {
        given:
        String invalidJson = '{"invalid": "data"}'
        String malformedJson = 'not valid json'

        when:
        ResponseEntity<String> response1 = insertEndpoint(endpointName, invalidJson)
        ResponseEntity<String> response2 = insertEndpoint(endpointName, malformedJson)

        then:
        response1.statusCode == HttpStatus.BAD_REQUEST
        response2.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }
}