package finance.controllers

import finance.domain.ImageFormatType
import finance.domain.ReceiptImage
import finance.domain.ServiceResult
import finance.services.ReceiptImageService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

import java.sql.Timestamp

class ReceiptImageControllerSpec extends Specification {

    ReceiptImageService receiptImageService = Mock()

    @Subject
    ReceiptImageController controller = new ReceiptImageController(receiptImageService)

    private ReceiptImage createReceiptImage(Long id = 1L, Long transactionId = 100L) {
        ReceiptImage ri = new ReceiptImage(id, "test_owner", transactionId, true)
        ri.image = "image data".bytes
        ri.thumbnail = "thumbnail data".bytes
        ri.imageFormatType = ImageFormatType.Png
        ri.dateAdded = new Timestamp(System.currentTimeMillis())
        ri.dateUpdated = new Timestamp(System.currentTimeMillis())
        return ri
    }

    def "findAllActive returns list with 200"() {
        given:
        List<ReceiptImage> images = [createReceiptImage(1L, 10L), createReceiptImage(2L, 20L)]
        receiptImageService.findAllActive() >> ServiceResult.Success.of(images)

        when:
        ResponseEntity<List<ReceiptImage>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "findAllActive returns empty list with 200"() {
        given:
        receiptImageService.findAllActive() >> ServiceResult.Success.of([])

        when:
        ResponseEntity<List<ReceiptImage>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActive returns 404 when NotFound"() {
        given:
        receiptImageService.findAllActive() >> ServiceResult.NotFound.of("No images found")

        when:
        ResponseEntity<List<ReceiptImage>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "findAllActive returns 500 on system error"() {
        given:
        receiptImageService.findAllActive() >> ServiceResult.SystemError.of(new RuntimeException("db down"))

        when:
        ResponseEntity<List<ReceiptImage>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "findById returns image with 200 when found"() {
        given:
        ReceiptImage image = createReceiptImage(5L, 50L)
        receiptImageService.findById(5L) >> ServiceResult.Success.of(image)

        when:
        ResponseEntity<ReceiptImage> response = controller.findById(5L)

        then:
        response.statusCode == HttpStatus.OK
        response.body.receiptImageId == 5L
        response.body.transactionId == 50L
    }

    def "findById returns 404 when not found"() {
        given:
        receiptImageService.findById(999L) >> ServiceResult.NotFound.of("ReceiptImage not found: 999")

        when:
        ResponseEntity<ReceiptImage> response = controller.findById(999L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "findById returns 500 on system error"() {
        given:
        receiptImageService.findById(1L) >> ServiceResult.SystemError.of(new RuntimeException("db error"))

        when:
        ResponseEntity<ReceiptImage> response = controller.findById(1L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "save creates image and returns 201"() {
        given:
        ReceiptImage input = createReceiptImage(0L, 100L)
        ReceiptImage saved = createReceiptImage(42L, 100L)
        receiptImageService.save(input) >> ServiceResult.Success.of(saved)

        when:
        ResponseEntity<ReceiptImage> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.receiptImageId == 42L
    }

    def "save returns 400 on validation error"() {
        given:
        ReceiptImage input = createReceiptImage(0L, 100L)
        receiptImageService.save(input) >> ServiceResult.ValidationError.of(["image": "Image is required"])

        when:
        ResponseEntity<ReceiptImage> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null
    }

    def "save returns 409 on business error"() {
        given:
        ReceiptImage input = createReceiptImage(0L, 100L)
        receiptImageService.save(input) >> ServiceResult.BusinessError.of("Duplicate image", "DUPLICATE")

        when:
        ResponseEntity<ReceiptImage> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null
    }

    def "save returns 500 on system error"() {
        given:
        ReceiptImage input = createReceiptImage(0L, 100L)
        receiptImageService.save(input) >> ServiceResult.SystemError.of(new RuntimeException("save failed"))

        when:
        ResponseEntity<ReceiptImage> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "update returns 200 on success"() {
        given:
        ReceiptImage input = createReceiptImage(7L, 70L)
        ReceiptImage updated = createReceiptImage(7L, 70L)
        receiptImageService.update(input) >> ServiceResult.Success.of(updated)

        when:
        ResponseEntity<ReceiptImage> response = controller.update(7L, input)

        then:
        response.statusCode == HttpStatus.OK
        response.body.receiptImageId == 7L
    }

    def "update returns 404 when not found"() {
        given:
        ReceiptImage input = createReceiptImage(99L, 99L)
        receiptImageService.update(input) >> ServiceResult.NotFound.of("ReceiptImage not found: 99")

        when:
        ResponseEntity<ReceiptImage> response = controller.update(99L, input)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "update returns 400 on validation error"() {
        given:
        ReceiptImage input = createReceiptImage(3L, 30L)
        receiptImageService.update(input) >> ServiceResult.ValidationError.of(["image": "Invalid"])

        when:
        ResponseEntity<ReceiptImage> response = controller.update(3L, input)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null
    }

    def "update returns 409 on business error"() {
        given:
        ReceiptImage input = createReceiptImage(4L, 40L)
        receiptImageService.update(input) >> ServiceResult.BusinessError.of("Conflict", "CONFLICT")

        when:
        ResponseEntity<ReceiptImage> response = controller.update(4L, input)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null
    }

    def "update returns 500 on system error"() {
        given:
        ReceiptImage input = createReceiptImage(5L, 50L)
        receiptImageService.update(input) >> ServiceResult.SystemError.of(new RuntimeException("update failed"))

        when:
        ResponseEntity<ReceiptImage> response = controller.update(5L, input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "deleteById returns 200 with deleted image"() {
        given:
        ReceiptImage existing = createReceiptImage(8L, 80L)
        receiptImageService.deleteById(8L) >> ServiceResult.Success.of(existing)

        when:
        ResponseEntity<ReceiptImage> response = controller.deleteById(8L)

        then:
        response.statusCode == HttpStatus.OK
        response.body.receiptImageId == 8L
    }

    def "deleteById returns 404 when not found"() {
        given:
        receiptImageService.deleteById(404L) >> ServiceResult.NotFound.of("ReceiptImage not found: 404")

        when:
        ResponseEntity<ReceiptImage> response = controller.deleteById(404L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "deleteById returns 500 on system error"() {
        given:
        receiptImageService.deleteById(500L) >> ServiceResult.SystemError.of(new RuntimeException("delete failed"))

        when:
        ResponseEntity<ReceiptImage> response = controller.deleteById(500L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }
}
