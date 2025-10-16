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

    void 'should reject receipt image insertion with invalid base64 image'() {
        given:
        String payload = '{"transactionId":1, "image":"test", "activeStatus":true}'

        when:
        // ReceiptImage uses legacy /insert endpoint - call directly instead of using helper
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        org.springframework.http.HttpEntity entity = new org.springframework.http.HttpEntity<>(payload, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image/insert",
            org.springframework.http.HttpMethod.POST,
            entity,
            String
        )

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }


    void 'should successfully insert jpeg receipt image'() {
        given:
        // Create transaction via HTTP endpoint first (like ValidationAmount does with accounts)
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueDescription("jpeg_receipt")
                .buildAndValidate()

        ResponseEntity<String> transactionResponse = insertEndpoint('transaction', transaction.toString())
        assert transactionResponse.statusCode in [HttpStatus.OK, HttpStatus.CREATED]

        // Extract transaction ID from response
        String transactionBody = transactionResponse.body
        String transactionIdStr = (transactionBody =~ /"transactionId":(\d+)/)[0][1]
        Long transactionId = Long.parseLong(transactionIdStr)

        ReceiptImage receiptImage = receiptImageTestContext.createJpegReceiptImage(transactionId)

        when:
        // ReceiptImage uses legacy /insert endpoint - call directly instead of using helper
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        org.springframework.http.HttpEntity entity = new org.springframework.http.HttpEntity<>(receiptImage.toString(), headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image/insert",
            org.springframework.http.HttpMethod.POST,
            entity,
            String
        )

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains('"id":')
        response.body.contains('"message":"Receipt image inserted"')
        0 * _
    }

    void 'should successfully insert png receipt image'() {
        given:
        // Create transaction via HTTP endpoint first
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueDescription("png_receipt")
                .buildAndValidate()

        ResponseEntity<String> transactionResponse = insertEndpoint('transaction', transaction.toString())
        assert transactionResponse.statusCode in [HttpStatus.OK, HttpStatus.CREATED]

        // Extract transaction ID from response
        String transactionBody = transactionResponse.body
        String transactionIdStr = (transactionBody =~ /"transactionId":(\d+)/)[0][1]
        Long transactionId = Long.parseLong(transactionIdStr)

        ReceiptImage receiptImage = receiptImageTestContext.createPngReceiptImage(transactionId)

        when:
        // ReceiptImage uses legacy /insert endpoint - call directly instead of using helper
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        org.springframework.http.HttpEntity entity = new org.springframework.http.HttpEntity<>(receiptImage.toString(), headers)

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image/insert",
            org.springframework.http.HttpMethod.POST,
            entity,
            String
        )

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains('"id":')
        response.body.contains('"message":"Receipt image inserted"')
        0 * _
    }

    void 'should retrieve receipt image by id when it exists'() {
        given:
        // Create transaction via HTTP endpoint first
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueDescription("retrieve_receipt")
                .buildAndValidate()

        ResponseEntity<String> transactionResponse = insertEndpoint('transaction', transaction.toString())
        assert transactionResponse.statusCode in [HttpStatus.OK, HttpStatus.CREATED]

        // Extract transaction ID from response
        String transactionBody = transactionResponse.body
        String transactionIdStr = (transactionBody =~ /"transactionId":(\d+)/)[0][1]
        Long transactionId = Long.parseLong(transactionIdStr)

        ReceiptImage receiptImage = receiptImageTestContext.createPngReceiptImage(transactionId)

        // First insert the receipt image to get its ID
        // ReceiptImage uses legacy /insert endpoint - call directly instead of using helper
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        org.springframework.http.HttpEntity entity = new org.springframework.http.HttpEntity<>(receiptImage.toString(), headers)

        ResponseEntity<String> insertResponse = restTemplate.exchange(
            "http://localhost:${port}/api/receipt/image/insert",
            org.springframework.http.HttpMethod.POST,
            entity,
            String
        )

        // Try both "id":"1" and "id":1 formats since we're not sure which format is returned
        String receiptImageId
        try {
            receiptImageId = (insertResponse.body =~ /"id":"(\d+)"/)[0][1]
        } catch (IndexOutOfBoundsException e) {
            receiptImageId = (insertResponse.body =~ /"id":(\d+)/)[0][1]
        }

        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, receiptImageId)

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains('"receiptImage":')
        response.body.contains('"transactionId":' + transactionId)
        0 * _
    }
}