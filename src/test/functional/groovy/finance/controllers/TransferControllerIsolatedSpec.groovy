package finance.controllers

import groovy.json.JsonSlurper
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@ActiveProfiles("func")
class TransferControllerIsolatedSpec extends BaseControllerSpec {

    @Shared
    protected String endpointName = 'transfer'

    @Shared
    Long createdTransferId

    def setupSpec() {
        // BaseControllerSpec.setupSpec() creates minimal accounts for testOwner
        // Nothing else needed here
    }

    void 'should successfully select all transfers'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/${endpointName}/select"),
                HttpMethod.GET, entity, String)

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.startsWith('[')
        0 * _
    }

    void 'should insert a transfer successfully with isolated test data'() {
        given:
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = 'testowner'

        String sourceAccountName = "primary_${cleanOwner}".toLowerCase()
        String destAccountName = "secondary_${cleanOwner}".toLowerCase()

        String payload = """
        {
          "transferId": 0,
          "sourceAccount": "${sourceAccountName}",
          "destinationAccount": "${destAccountName}",
          "transactionDate": "2023-12-01",
          "amount": 150.75,
          "guidSource": "ba665bc2-22b6-4123-a566-6f5ab3d796d1",
          "guidDestination": "ba665bc2-22b6-4123-a566-6f5ab3d796d2",
          "activeStatus": true
        }
        """

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payload)

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        def parsed = new JsonSlurper().parseText(response.body)
        parsed.sourceAccount == sourceAccountName
        parsed.destinationAccount == destAccountName
        parsed.amount == 150.75
        parsed.transferId instanceof Number

        cleanup:
        createdTransferId = parsed.transferId as Long
        0 * _
    }

    void 'should return conflict when inserting duplicate transfer'() {
        given:
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = 'testowner'

        String sourceAccountName = "primary_${cleanOwner}".toLowerCase()
        String destAccountName = "secondary_${cleanOwner}".toLowerCase()

        String payload = """
        {
          "transferId": 0,
          "sourceAccount": "${sourceAccountName}",
          "destinationAccount": "${destAccountName}",
          "transactionDate": "2023-12-01",
          "amount": 150.75,
          "guidSource": "ba665bc2-22b6-4123-a566-6f5ab3d796d1",
          "guidDestination": "ba665bc2-22b6-4123-a566-6f5ab3d796d2",
          "activeStatus": true
        }
        """

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payload)

        then:
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'should delete transfer by id successfully'() {
        given:
        Long transferId = createdTransferId

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, transferId.toString())

        then:
        response.statusCode == HttpStatus.OK
        response.body != null
        with(new JsonSlurper().parseText(response.body)) {
            it.transferId == transferId
        }
        0 * _
    }

    void 'should return not found deleting non-existent transfer id'() {
        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, '999999')

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should reject invalid JSON payloads'() {
        given:
        String invalidPayload = '{"invalid":"data"}'
        String badJson = 'bad json'

        when:
        ResponseEntity<String> response1 = insertEndpoint(endpointName, invalidPayload)
        ResponseEntity<String> response2 = insertEndpoint(endpointName, badJson)

        then:
        response1.statusCode == HttpStatus.BAD_REQUEST
        response2.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }
}
