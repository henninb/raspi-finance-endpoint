package finance.controllers

import finance.domain.ImageFormatType
import finance.domain.ReceiptImage
import finance.services.IReceiptImageService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import spock.lang.Specification
import spock.lang.Subject

class ReceiptImageControllerSpec extends Specification {

    IReceiptImageService receiptImageService = GroovyMock(IReceiptImageService)

    @Subject
    ReceiptImageController controller = new ReceiptImageController(receiptImageService)

    private ReceiptImage sample() {
        def ri = new ReceiptImage(receiptImageId: 0L, transactionId: 123L, activeStatus: true)
        ri.imageFormatType = ImageFormatType.Jpeg
        ri.image = 'hello'.bytes
        ri.thumbnail = 'hi'.bytes
        return ri
    }

    def "insertReceiptImage returns 400 on validation errors"() {
        given:
        BindingResult br = GroovyMock(BindingResult)
        br.hasErrors() >> true
        br.getFieldErrors() >> [new FieldError('receiptImage', 'image', 'Invalid'), new FieldError('receiptImage', 'thumbnail', 'Invalid')]

        when:
        ResponseEntity<Map<String,String>> response = controller.insertReceiptImage(sample(), br)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * receiptImageService._
    }

    def "insertReceiptImage returns 500 when service throws exception"() {
        given:
        BindingResult br = GroovyMock(BindingResult)
        br.hasErrors() >> false

        when:
        ResponseEntity<Map<String,String>> response = controller.insertReceiptImage(sample(), br)

        then:
        1 * receiptImageService.insertReceiptImage(_) >> { throw new RuntimeException('Service error') }
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body.error == 'Failed to insert receipt image'
    }

    def "selectReceiptImage returns 200 when present else 404"() {
        when:
        ResponseEntity<Map<String, Object>> response = controller.selectReceiptImage(9L)

        then:
        1 * receiptImageService.findByReceiptImageId(9L) >> Optional.of(sample())
        response.statusCode == HttpStatus.OK
        response.body.message == 'Receipt image found'

        when:
        ResponseEntity<Map<String, Object>> response2 = controller.selectReceiptImage(10L)

        then:
        1 * receiptImageService.findByReceiptImageId(10L) >> Optional.empty()
        response2.statusCode == HttpStatus.NOT_FOUND
        response2.body.error == 'Receipt image not found'
    }
}
