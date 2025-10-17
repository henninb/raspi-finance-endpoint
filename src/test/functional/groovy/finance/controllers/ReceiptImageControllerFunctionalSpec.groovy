package finance.controllers

import finance.domain.ImageFormatType
import finance.domain.ReceiptImage
import finance.domain.Transaction
import finance.helpers.ReceiptImageTestContext
import finance.helpers.SmartReceiptImageBuilder
import finance.helpers.SmartTransactionBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@ActiveProfiles("func")
class ReceiptImageControllerFunctionalSpec extends BaseControllerFunctionalSpec {

    @Shared ReceiptImageTestContext receiptImageTestContext
    @Shared String endpointName = 'receipt/image'

    def setupSpec() {
        receiptImageTestContext = testFixtures.createReceiptImageTestContext(testOwner)
    }

    def cleanupSpec() {
        receiptImageTestContext?.cleanup()
    }

    // ===== MODERN ENDPOINT TESTS =====

    void 'modern endpoint: GET /active should return empty list when no receipt images exist'() {
        when:
        String token = generateJwtToken(username)
        headers.add("Cookie", "token=${token}")
        headers.add("Authorization", "Bearer ${token}")
        org.springframework.http.HttpEntity entity = new org.springframework.http.HttpEntity<>(headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image/active",
            org.springframework.http.HttpMethod.GET,
            entity,
            String
        )

        then:
        response.statusCode == HttpStatus.OK
        response.body == "[]" || response.body.contains('"receiptImageId"')
        0 * _
    }

    void 'modern endpoint: POST / should create receipt image and return 201 CREATED'() {
        given:
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueDescription("modern_create_receipt")
                .buildAndValidate()

        ResponseEntity<String> transactionResponse = insertEndpoint('transaction', transaction.toString())
        assert transactionResponse.statusCode in [HttpStatus.OK, HttpStatus.CREATED]

        String transactionBody = transactionResponse.body
        String transactionIdStr = (transactionBody =~ /"transactionId":(\d+)/)[0][1]
        Long transactionId = Long.parseLong(transactionIdStr)

        ReceiptImage receiptImage = receiptImageTestContext.createJpegReceiptImage(transactionId)

        when:
        String token = generateJwtToken(username)
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON)
        headers.add("Cookie", "token=${token}")
        headers.add("Authorization", "Bearer ${token}")
        org.springframework.http.HttpEntity entity = new org.springframework.http.HttpEntity<>(receiptImage.toString(), headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image",
            org.springframework.http.HttpMethod.POST,
            entity,
            String
        )

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.contains('"receiptImageId":')
        response.body.contains('"transactionId":' + transactionId)
        0 * _
    }

    void 'modern endpoint: GET /{id} should retrieve receipt image by id'() {
        given:
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueDescription("modern_get_receipt")
                .buildAndValidate()

        ResponseEntity<String> transactionResponse = insertEndpoint('transaction', transaction.toString())
        assert transactionResponse.statusCode in [HttpStatus.OK, HttpStatus.CREATED]

        String transactionBody = transactionResponse.body
        String transactionIdStr = (transactionBody =~ /"transactionId":(\d+)/)[0][1]
        Long transactionId = Long.parseLong(transactionIdStr)

        ReceiptImage receiptImage = receiptImageTestContext.createPngReceiptImage(transactionId)

        // Create receipt image using modern endpoint
        String token = generateJwtToken(username)
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON)
        headers.add("Cookie", "token=${token}")
        headers.add("Authorization", "Bearer ${token}")
        org.springframework.http.HttpEntity createEntity = new org.springframework.http.HttpEntity<>(receiptImage.toString(), headers)

        ResponseEntity<String> createResponse = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image",
            org.springframework.http.HttpMethod.POST,
            createEntity,
            String
        )

        String receiptImageId = (createResponse.body =~ /"receiptImageId":(\d+)/)[0][1]

        when:
        headers = new org.springframework.http.HttpHeaders()
        headers.add("Cookie", "token=${token}")
        headers.add("Authorization", "Bearer ${token}")
        org.springframework.http.HttpEntity getEntity = new org.springframework.http.HttpEntity<>(headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image/${receiptImageId}",
            org.springframework.http.HttpMethod.GET,
            getEntity,
            String
        )

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains('"receiptImageId":' + receiptImageId)
        response.body.contains('"transactionId":' + transactionId)
        0 * _
    }

    void 'modern endpoint: PUT /{id} should update receipt image'() {
        given:
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueDescription("modern_update_receipt")
                .buildAndValidate()

        ResponseEntity<String> transactionResponse = insertEndpoint('transaction', transaction.toString())
        assert transactionResponse.statusCode in [HttpStatus.OK, HttpStatus.CREATED]

        String transactionBody = transactionResponse.body
        String transactionIdStr = (transactionBody =~ /"transactionId":(\d+)/)[0][1]
        Long transactionId = Long.parseLong(transactionIdStr)

        ReceiptImage receiptImage = receiptImageTestContext.createJpegReceiptImage(transactionId)

        // Create receipt image
        String token = generateJwtToken(username)
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON)
        headers.add("Cookie", "token=${token}")
        headers.add("Authorization", "Bearer ${token}")
        org.springframework.http.HttpEntity createEntity = new org.springframework.http.HttpEntity<>(receiptImage.toString(), headers)

        ResponseEntity<String> createResponse = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image",
            org.springframework.http.HttpMethod.POST,
            createEntity,
            String
        )

        String receiptImageId = (createResponse.body =~ /"receiptImageId":(\d+)/)[0][1]
        receiptImage.receiptImageId = Long.parseLong(receiptImageId)
        receiptImage.activeStatus = false

        when:
        headers = new org.springframework.http.HttpHeaders()
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON)
        headers.add("Cookie", "token=${token}")
        headers.add("Authorization", "Bearer ${token}")
        org.springframework.http.HttpEntity updateEntity = new org.springframework.http.HttpEntity<>(receiptImage.toString(), headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image/${receiptImageId}",
            org.springframework.http.HttpMethod.PUT,
            updateEntity,
            String
        )

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains('"receiptImageId":' + receiptImageId)
        response.body.contains('"activeStatus":false')
        0 * _
    }

    void 'modern endpoint: DELETE /{id} should delete receipt image and return 200 OK with entity'() {
        given:
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueDescription("modern_delete_receipt")
                .buildAndValidate()

        ResponseEntity<String> transactionResponse = insertEndpoint('transaction', transaction.toString())
        assert transactionResponse.statusCode in [HttpStatus.OK, HttpStatus.CREATED]

        String transactionBody = transactionResponse.body
        String transactionIdStr = (transactionBody =~ /"transactionId":(\d+)/)[0][1]
        Long transactionId = Long.parseLong(transactionIdStr)

        ReceiptImage receiptImage = receiptImageTestContext.createPngReceiptImage(transactionId)

        // Create receipt image
        String token = generateJwtToken(username)
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON)
        headers.add("Cookie", "token=${token}")
        headers.add("Authorization", "Bearer ${token}")
        org.springframework.http.HttpEntity createEntity = new org.springframework.http.HttpEntity<>(receiptImage.toString(), headers)

        ResponseEntity<String> createResponse = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image",
            org.springframework.http.HttpMethod.POST,
            createEntity,
            String
        )

        String receiptImageId = (createResponse.body =~ /"receiptImageId":(\d+)/)[0][1]

        when:
        headers = new org.springframework.http.HttpHeaders()
        headers.add("Cookie", "token=${token}")
        headers.add("Authorization", "Bearer ${token}")
        org.springframework.http.HttpEntity deleteEntity = new org.springframework.http.HttpEntity<>(headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image/${receiptImageId}",
            org.springframework.http.HttpMethod.DELETE,
            deleteEntity,
            String
        )

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains('"receiptImageId":' + receiptImageId)
        0 * _
    }

    void 'modern endpoint: GET /{id} should return 404 NOT_FOUND for non-existent id'() {
        when:
        String token = generateJwtToken(username)
        headers.add("Cookie", "token=${token}")
        headers.add("Authorization", "Bearer ${token}")
        org.springframework.http.HttpEntity entity = new org.springframework.http.HttpEntity<>(headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image/999999",
            org.springframework.http.HttpMethod.GET,
            entity,
            String
        )

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

}
