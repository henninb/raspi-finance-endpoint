package finance.domain


import finance.helpers.ReceiptImageBuilder
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation

import static finance.utils.Constants.FILED_MUST_BE_GREATER_THAN_ZERO_MESSAGE

class ReceiptImageSpec extends BaseDomainSpec {
//    String payload = """
//{"transactionId":1, "image":"data:image/jpeg;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg==", "activeStatus":true}
//"""
//    String payload = """
//{"transactionId":1, "image":"data:image/jpeg;base64,amFja3Nvbg==", "activeStatus":true}
//"""

    protected String payload = """
{"transactionId":1, "image":"amFja3Nvbg==", "activeStatus":true}
"""

    void 'test ReceiptImage to JSON'() {
        when:
        ReceiptImage receiptImageFromJson = mapper.readValue(payload, ReceiptImage)
        and:
        String result = new String(receiptImageFromJson.image)

        then:
        result == 'jackson'
        noExceptionThrown()
        0 * _
    }

    @Unroll
    void 'test ReceiptImage validation invalid #invalidField has error expectedError'() {
        given:
        ReceiptImage receiptImage = new ReceiptImageBuilder().builder()
                .withTransactionId(transactionId)
                .withActiveStatus(activeStatus)
                .withImageFormatType(imageFormatType)
                .withThumbnail(thumbnail)
                .withImage(image)
                .build()

        when:
        Set<ConstraintViolation<ReceiptImage>> violations = validator.validate(receiptImage)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().invalidValue == receiptImage.properties[invalidField]

        where:
        invalidField    | transactionId | imageFormatType     | image                                                                                              | thumbnail                                                                                          | activeStatus | expectedError                           | errorCount
        'transactionId' | -1            | ImageFormatType.Png | 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg==' | 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg==' | true         | FILED_MUST_BE_GREATER_THAN_ZERO_MESSAGE | 1
    }

    void 'test jsonGetterJpgImage returns base64 encoded image bytes'() {
        given:
        ReceiptImage ri = new ReceiptImage()
        ri.image = "hello world".bytes
        ri.thumbnail = "thumb".bytes

        when:
        String encoded = ri.jsonGetterJpgImage()

        then:
        encoded == java.util.Base64.getEncoder().encodeToString("hello world".bytes)
        noExceptionThrown()
    }

    void 'test toString returns JSON representation'() {
        given:
        ReceiptImage ri = new ReceiptImage()
        ri.image = "img".bytes
        ri.thumbnail = "thumb".bytes

        when:
        String result = ri.toString()

        then:
        result != null
        !result.isEmpty()
        noExceptionThrown()
    }

    void 'test ReceiptImage with explicit owner and activeStatus false'() {
        given:
        ReceiptImage ri = new ReceiptImage(1L, "test_owner", 5L, false)

        expect:
        ri.receiptImageId == 1L
        ri.owner == "test_owner"
        ri.transactionId == 5L
        ri.activeStatus == false
    }
}
