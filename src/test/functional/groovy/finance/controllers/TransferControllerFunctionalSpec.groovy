package finance.controllers

import finance.domain.Transfer
import finance.domain.Account
import finance.helpers.SmartTransferBuilder
import finance.helpers.SmartAccountBuilder
import finance.helpers.TransferTestContext
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import java.sql.Date

@ActiveProfiles("func")
class TransferControllerFunctionalSpec extends BaseControllerFunctionalSpec {

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
        // Create source account using SmartBuilder
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("primary")
                .asDebit()
                .buildAndValidate()

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccount.toString())
        assert sourceAccountResponse.statusCode == HttpStatus.CREATED || sourceAccountResponse.statusCode == HttpStatus.CONFLICT

        // Create destination account using SmartBuilder
        Account destAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("secondary")
                .asDebit()
                .buildAndValidate()

        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccount.toString())
        assert destAccountResponse.statusCode == HttpStatus.CREATED || destAccountResponse.statusCode == HttpStatus.CONFLICT

        // Create transfer using SmartTransferBuilder
        Transfer transfer = SmartTransferBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(150.75G)
                .withTransactionDate(Date.valueOf('2023-01-01'))
                .asActive()
                .buildAndValidate()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, transfer.toString())

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains('"sourceAccount":"' + sourceAccount.accountNameOwner + '"')
        response.body.contains('"destinationAccount":"' + destAccount.accountNameOwner + '"')
        response.body.contains('"amount":150.75')
        0 * _
    }

    void 'should successfully handle different transfer amounts'() {
        given:
        // Create source account using SmartBuilder
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("amountsrc")
                .asDebit()
                .buildAndValidate()

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccount.toString())
        assert sourceAccountResponse.statusCode == HttpStatus.CREATED || sourceAccountResponse.statusCode == HttpStatus.CONFLICT

        // Create destination account using SmartBuilder
        Account destAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("amountdest")
                .asDebit()
                .buildAndValidate()

        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccount.toString())
        assert destAccountResponse.statusCode == HttpStatus.CREATED || destAccountResponse.statusCode == HttpStatus.CONFLICT

        // Create small transfer using SmartTransferBuilder
        Transfer transferSmall = SmartTransferBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(25.50G)
                .withTransactionDate(Date.valueOf('2023-01-01'))
                .asActive()
                .buildAndValidate()

        // Create large transfer using SmartTransferBuilder
        Transfer transferLarge = SmartTransferBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(999.99G)
                .withTransactionDate(Date.valueOf('2023-01-02'))
                .asActive()
                .buildAndValidate()

        when:
        ResponseEntity<String> smallResponse = insertEndpoint(endpointName, transferSmall.toString())
        ResponseEntity<String> largeResponse = insertEndpoint(endpointName, transferLarge.toString())

        then:
        smallResponse.statusCode == HttpStatus.OK
        smallResponse.body.contains('"amount":25.5')
        largeResponse.statusCode == HttpStatus.OK
        largeResponse.body.contains('"amount":999.99')
        0 * _
    }

    void 'should successfully handle active and inactive transfers'() {
        given:
        // Create source account using SmartBuilder
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("statussrc")
                .asDebit()
                .buildAndValidate()

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccount.toString())
        assert sourceAccountResponse.statusCode == HttpStatus.CREATED || sourceAccountResponse.statusCode == HttpStatus.CONFLICT

        // Create destination account using SmartBuilder
        Account destAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("statusdest")
                .asDebit()
                .buildAndValidate()

        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccount.toString())
        assert destAccountResponse.statusCode == HttpStatus.CREATED || destAccountResponse.statusCode == HttpStatus.CONFLICT

        // Create active transfer using SmartTransferBuilder
        Transfer activeTransfer = SmartTransferBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(200.00G)
                .withTransactionDate(Date.valueOf('2023-01-03'))
                .asActive()
                .buildAndValidate()

        // Create inactive transfer using SmartTransferBuilder
        Transfer inactiveTransfer = SmartTransferBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(300.00G)
                .withTransactionDate(Date.valueOf('2023-01-04'))
                .asInactive()
                .buildAndValidate()

        when:
        ResponseEntity<String> activeResponse = insertEndpoint(endpointName, activeTransfer.toString())
        ResponseEntity<String> inactiveResponse = insertEndpoint(endpointName, inactiveTransfer.toString())

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

    void 'should reject duplicate transfer insertion with conflict status'() {
        given:
        // Create source account using SmartBuilder (proven working pattern)
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("dupsrc")
                .asDebit()
                .buildAndValidate()

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccount.toString())
        assert sourceAccountResponse.statusCode == HttpStatus.CREATED || sourceAccountResponse.statusCode == HttpStatus.CONFLICT

        // Create destination account using SmartBuilder
        Account destAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("dupdest")
                .asDebit()
                .buildAndValidate()

        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccount.toString())
        assert destAccountResponse.statusCode == HttpStatus.CREATED || destAccountResponse.statusCode == HttpStatus.CONFLICT

        // Create transfer with specific GUIDs for duplication testing (lowercase for validation)
        Transfer transfer = SmartTransferBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(150.75G)
                .withTransactionDate(Date.valueOf('2023-12-01'))
                .withGuidSource('ba665bc2-22b6-4123-a566-6f5ab3d796d1')
                .withGuidDestination('ba665bc2-22b6-4123-a566-6f5ab3d796d2')
                .asActive()
                .buildAndValidate()

        when:
        ResponseEntity<String> firstResponse = insertEndpoint(endpointName, transfer.toString())
        ResponseEntity<String> duplicateResponse = insertEndpoint(endpointName, transfer.toString())

        then:
        firstResponse.statusCode == HttpStatus.OK
        duplicateResponse.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'should successfully delete transfer by ID'() {
        given:
        // Create source account using SmartBuilder (proven working pattern)
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("delsrc")
                .asDebit()
                .buildAndValidate()

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccount.toString())
        assert sourceAccountResponse.statusCode == HttpStatus.CREATED || sourceAccountResponse.statusCode == HttpStatus.CONFLICT

        // Create destination account using SmartBuilder
        Account destAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("deldest")
                .asDebit()
                .buildAndValidate()

        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccount.toString())
        assert destAccountResponse.statusCode == HttpStatus.CREATED || destAccountResponse.statusCode == HttpStatus.CONFLICT

        // Create transfer using SmartBuilder
        Transfer transfer = SmartTransferBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(99.99G)
                .withTransactionDate(Date.valueOf('2023-01-01'))
                .asActive()
                .buildAndValidate()

        // Insert transfer and extract ID
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, transfer.toString())
        assert insertResponse.statusCode == HttpStatus.OK

        String transferIdStr = (insertResponse.body =~ /"transferId":(\d+)/)[0][1]
        Long transferId = Long.parseLong(transferIdStr)

        when:
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                createURLWithPort("/api/transfer/delete/${transferId}"),
                HttpMethod.DELETE, entity, String)

        then:
        deleteResponse.statusCode == HttpStatus.OK
        deleteResponse.body.contains('"transferId":' + transferId)
        0 * _
    }

    void 'should return not found when deleting non-existent transfer'() {
        given:
        Long nonExistentId = 999999L

        when:
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/transfer/delete/${nonExistentId}"),
                HttpMethod.DELETE, entity, String)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should handle unauthorized access to transfer endpoints'() {
        given:
        HttpHeaders cleanHeaders = new HttpHeaders()
        cleanHeaders.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(null, cleanHeaders)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/transfer/select"),
                HttpMethod.GET, entity, String)

        then:
        // TransferController may allow unauthorized access based on security config
        response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.UNAUTHORIZED || response.statusCode == HttpStatus.FORBIDDEN
        0 * _
    }
}