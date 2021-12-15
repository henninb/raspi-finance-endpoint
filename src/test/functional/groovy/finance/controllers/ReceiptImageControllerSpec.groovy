package finance.controllers

import finance.Application
import finance.domain.ImageFormatType
import finance.domain.ReceiptImage
import finance.helpers.ReceiptImageBuilder
import finance.repositories.ReceiptImageRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReceiptImageControllerSpec extends BaseControllerSpec {
    @Shared
    protected ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()

    @Autowired
    protected ReceiptImageRepository receiptImageRepository

    @Shared
    protected String endpointName = 'receipt/image'

    void 'test insert receiptImage - bad image'() {
        given:
        String payload = '{"transactionId":1, "image":"test", "activeStatus":true}'

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payload.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test insert receiptImage - transaction does not exist'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().withTransactionId(0).build()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, receiptImage.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test insert receiptImage - jpeg'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder()
                .withJpgImage('/9j/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA8MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/yQALCAABAAEBAREA/8wABgAQEAX/2gAIAQEAAD8A0s8g/9k=')
                .withThumbnail('/9j/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA8MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/yQALCAABAAEBAREA/8wABgAQEAX/2gAIAQEAAD8A0s8g/9k=')
                .withImageFormatType(ImageFormatType.Jpeg)
                .withTransactionId(22530)
                .build()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, receiptImage.toString())

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test insert receiptImage - png'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder()
                .withJpgImage('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg==')
                .withThumbnail('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg==')
                .withImageFormatType(ImageFormatType.Png)
                .withTransactionId(22531)
                .build()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, receiptImage.toString())

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test insert receiptImage - find'() {
        given:
        Optional<ReceiptImage> receiptImageOptional = receiptImageRepository.findByTransactionId(receiptImage.transactionId)
        Long receiptImageId = receiptImageOptional.get().receiptImageId

        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, receiptImageId.toString())

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    @Ignore('This test should return a 400, but is currently returning a 200.')
    void 'test insert receiptImage - duplicate'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, receiptImage.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }
}
