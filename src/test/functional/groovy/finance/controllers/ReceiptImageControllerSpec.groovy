package finance.controllers

import finance.Application
import finance.domain.Category
import finance.domain.ReceiptImage
import finance.helpers.CategoryBuilder
import finance.helpers.ReceiptImageBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.IgnoreRest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReceiptImageControllerSpec extends BaseControllerSpec {
    @Shared
    protected ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()

    String payload = '''
{"transactionId":1, "jpgImage":"test", "activeStatus":true}
'''
    @Ignore('this test should fail')
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

    void 'test insert receiptImage'() {
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
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }
}
