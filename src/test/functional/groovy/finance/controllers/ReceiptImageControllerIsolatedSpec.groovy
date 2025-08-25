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
class ReceiptImageControllerIsolatedSpec extends BaseControllerSpec {

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
        ResponseEntity<String> response = insertEndpoint(endpointName, payload)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should reject receipt image insertion with non-existent transaction id'() {
        given:
        ReceiptImage receiptImage = SmartReceiptImageBuilder.builderForOwner(testOwner)
                .withTransactionId(99999L)
                .buildAndValidate()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, receiptImage.toString())

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
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
        ResponseEntity<String> response = insertEndpoint(endpointName, receiptImage.toString())

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
        ResponseEntity<String> response = insertEndpoint(endpointName, receiptImage.toString())

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
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, receiptImage.toString())

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