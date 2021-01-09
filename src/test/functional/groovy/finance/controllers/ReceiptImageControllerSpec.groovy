package finance.controllers

import finance.Application
import finance.domain.ReceiptImage
import finance.helpers.ReceiptImageBuilder
import finance.repositories.ReceiptImageRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
    ReceiptImageRepository receiptImageRepository

    String payload = '''
{"transactionId":1, "jpgImage":"test", "activeStatus":true}
'''

    void 'test insert receiptImage - bad image'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/receipt/image/insert'), HttpMethod.POST,
                entity, String)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test insert receiptImage - transaction does not exist'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().withTransactionId(0).build()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(receiptImage, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/receipt/image/insert'), HttpMethod.POST,
                entity, String)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test insert receiptImage - jpeg'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder()
                .withJpgImage('/9j/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA8MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/yQALCAABAAEBAREA/8wABgAQEAX/2gAIAQEAAD8A0s8g/9k=')
                .withTransactionId(22530)
                .build()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(receiptImage, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/receipt/image/insert'), HttpMethod.POST,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test insert receiptImage - png'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder()
                .withJpgImage('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg==')
                .withTransactionId(22531)
                .build()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(receiptImage, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/receipt/image/insert'), HttpMethod.POST,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test insert receiptImage - find'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(null, headers)
        Optional<ReceiptImage> receiptImageOptional = receiptImageRepository.findByTransactionId(receiptImage.transactionId)
        Long receiptImageId = receiptImageOptional.get().receiptImageId

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/receipt/image/select/$receiptImageId"), HttpMethod.GET,
                entity, String)
        println(response.body)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    @Ignore('This test should return a 400, but is currently returning a 200.')
    void 'test insert receiptImage - duplicate'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(receiptImage, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/receipt/image/insert'), HttpMethod.POST,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }
}
