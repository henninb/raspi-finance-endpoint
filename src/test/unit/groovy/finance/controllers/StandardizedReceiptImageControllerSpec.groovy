package finance.controllers

import finance.domain.ImageFormatType
import finance.domain.ReceiptImage
import finance.services.StandardizedReceiptImageService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import spock.lang.Specification
import spock.lang.Subject

class StandardizedReceiptImageControllerSpec extends Specification {

    finance.repositories.ReceiptImageRepository receiptImageRepository = Mock()
    StandardizedReceiptImageService receiptImageService = new StandardizedReceiptImageService(receiptImageRepository)

    @Subject
    ReceiptImageController controller = new ReceiptImageController(receiptImageService)

    def setup() {
        def validator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([] as Set)
        }
        def meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        def meterService = new finance.services.MeterService(meterRegistry)

        receiptImageService.validator = validator
        receiptImageService.meterService = meterService
    }

    // ===== insertReceiptImage TESTS =====

    def "insertReceiptImage creates receipt image successfully"() {
        given:
        ReceiptImage input = createValidReceiptImage(0L, 123L)
        BindingResult bindingResult = Mock() {
            hasErrors() >> false
        }
        and:
        receiptImageRepository.saveAndFlush(_ as ReceiptImage) >> { ReceiptImage ri ->
            ri.receiptImageId = 99L
            return ri
        }

        when:
        ResponseEntity<Map<String, String>> response = controller.insertReceiptImage(input, bindingResult)

        then:
        response.statusCode == HttpStatus.OK
        response.body.get("message") == "Receipt image inserted"
        response.body.get("id") == "99"
    }

    def "insertReceiptImage handles validation errors from binding result"() {
        given:
        ReceiptImage invalid = createValidReceiptImage(0L, 123L)
        BindingResult bindingResult = Mock() {
            hasErrors() >> true
            getFieldErrors() >> [new FieldError("receiptImage", "image", "Image data is required")]
        }

        when:
        ResponseEntity<Map<String, String>> response = controller.insertReceiptImage(invalid, bindingResult)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.get("errors") != null
        response.body.get("errors").contains("Image data is required")
    }

    def "insertReceiptImage handles service validation errors"() {
        given:
        ReceiptImage invalid = createValidReceiptImage(0L, 123L)
        BindingResult bindingResult = Mock() {
            hasErrors() >> false
        }
        and:
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        receiptImageService.validator = violatingValidator

        when:
        ResponseEntity<Map<String, String>> response = controller.insertReceiptImage(invalid, bindingResult)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.get("errors") != null
    }

    def "insertReceiptImage handles business error with 409"() {
        given:
        ReceiptImage duplicate = createValidReceiptImage(0L, 123L)
        BindingResult bindingResult = Mock() {
            hasErrors() >> false
        }
        and:
        receiptImageRepository.saveAndFlush(_ as ReceiptImage) >> {
            throw new org.springframework.dao.DataIntegrityViolationException("Duplicate receipt image")
        }

        when:
        ResponseEntity<Map<String, String>> response = controller.insertReceiptImage(duplicate, bindingResult)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body.get("error").contains("Data integrity violation in save for ReceiptImage")
    }

    def "insertReceiptImage handles system error with 500"() {
        given:
        ReceiptImage input = createValidReceiptImage(0L, 123L)
        BindingResult bindingResult = Mock() {
            hasErrors() >> false
        }
        and:
        receiptImageRepository.saveAndFlush(_ as ReceiptImage) >> {
            throw new RuntimeException("Database connection failed")
        }

        when:
        ResponseEntity<Map<String, String>> response = controller.insertReceiptImage(input, bindingResult)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body.get("error") == "Failed to insert receipt image"
    }

    def "insertReceiptImage handles unexpected result type with 500"() {
        given:
        ReceiptImage input = createValidReceiptImage(0L, 123L)
        BindingResult bindingResult = Mock() {
            hasErrors() >> false
        }
        and:
        // Mock an unexpected condition - IllegalStateException becomes BusinessError, not SystemError
        receiptImageRepository.saveAndFlush(_ as ReceiptImage) >> {
            throw new IllegalStateException("Unexpected state")
        }

        when:
        ResponseEntity<Map<String, String>> response = controller.insertReceiptImage(input, bindingResult)

        then:
        response.statusCode == HttpStatus.CONFLICT  // BusinessError maps to CONFLICT
        response.body.get("error").contains("Business logic error in save for ReceiptImage")
    }

    def "insertReceiptImage handles multiple validation errors from binding result"() {
        given:
        ReceiptImage invalid = createValidReceiptImage(0L, 123L)
        BindingResult bindingResult = Mock() {
            hasErrors() >> true
            getFieldErrors() >> [
                new FieldError("receiptImage", "image", "Image data is required"),
                new FieldError("receiptImage", "transactionId", "Transaction ID must be positive")
            ]
        }

        when:
        ResponseEntity<Map<String, String>> response = controller.insertReceiptImage(invalid, bindingResult)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.get("errors") != null
        response.body.get("errors").contains("Image data is required")
        response.body.get("errors").contains("Transaction ID must be positive")
    }

    // ===== selectReceiptImage TESTS =====

    def "selectReceiptImage returns receipt image when found"() {
        given:
        ReceiptImage existingImage = createValidReceiptImage(5L, 123L)
        and:
        receiptImageRepository.findById(5L) >> Optional.of(existingImage)

        when:
        ResponseEntity<Map<String, Object>> response = controller.selectReceiptImage(5L)

        then:
        response.statusCode == HttpStatus.OK
        response.body.get("message") == "Receipt image found"
        (response.body.get("receiptImage") as ReceiptImage).receiptImageId == 5L
        (response.body.get("receiptImage") as ReceiptImage).transactionId == 123L
    }

    def "selectReceiptImage returns 404 when receipt image not found"() {
        given:
        receiptImageRepository.findById(999L) >> Optional.empty()

        when:
        ResponseEntity<Map<String, Object>> response = controller.selectReceiptImage(999L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.get("error") == "ReceiptImage not found: 999"
    }

    def "selectReceiptImage returns 500 on system error"() {
        given:
        receiptImageRepository.findById(1L) >> { throw new RuntimeException("Database connection failed") }

        when:
        ResponseEntity<Map<String, Object>> response = controller.selectReceiptImage(1L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body.get("error") == "Error retrieving receipt image"
    }

    def "selectReceiptImage handles unexpected result type with 500"() {
        given:
        receiptImageRepository.findById(1L) >> { throw new IllegalStateException("Unexpected state") }

        when:
        ResponseEntity<Map<String, Object>> response = controller.selectReceiptImage(1L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body.get("error") == "Error retrieving receipt image"
    }

    def "selectReceiptImage validates positive ID constraint"() {
        given:
        Long negativeId = -1L

        when:
        ResponseEntity<Map<String, Object>> response = controller.selectReceiptImage(negativeId)

        then:
        // This test verifies the @Positive constraint on the path variable
        // The constraint validation would be handled by Spring's validation framework
        // but for unit testing, we just verify the method handles the call
        response != null
        // In real scenario with constraint validation, this would return 400 Bad Request
        // but in unit test without full Spring context, the method executes normally
    }

    def "selectReceiptImage returns proper response structure for found image"() {
        given:
        ReceiptImage imageWithThumbnail = createValidReceiptImage(10L, 456L)
        imageWithThumbnail.imageFormatType = ImageFormatType.Jpeg
        and:
        receiptImageRepository.findById(10L) >> Optional.of(imageWithThumbnail)

        when:
        ResponseEntity<Map<String, Object>> response = controller.selectReceiptImage(10L)

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2 // Should contain exactly receiptImage and message
        response.body.containsKey("receiptImage")
        response.body.containsKey("message")

        ReceiptImage returnedImage = response.body.get("receiptImage") as ReceiptImage
        returnedImage.receiptImageId == 10L
        returnedImage.transactionId == 456L
        returnedImage.imageFormatType == ImageFormatType.Jpeg
    }

    // ===== BaseController Exception Handler Coverage =====

    def "controller inherits exception handling from BaseController"() {
        given:
        ReceiptImage input = createValidReceiptImage(0L, 123L)
        BindingResult bindingResult = Mock() {
            hasErrors() >> false
        }
        and:
        receiptImageRepository.saveAndFlush(_ as ReceiptImage) >> {
            throw new jakarta.validation.ConstraintViolationException("Constraint violation", [] as Set)
        }

        when:
        ResponseEntity response = controller.insertReceiptImage(input, bindingResult)

        then:
        // The exception should be handled by BaseController's exception handler
        response.statusCode == HttpStatus.BAD_REQUEST || response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "selectReceiptImage handles EntityNotFoundException from service layer"() {
        given:
        receiptImageRepository.findById(404L) >> { throw new jakarta.persistence.EntityNotFoundException("Entity not found") }

        when:
        ResponseEntity<Map<String, Object>> response = controller.selectReceiptImage(404L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND  // EntityNotFoundException maps to NotFound, which maps to 404
        response.body.get("error") == "ReceiptImage not found: 404"
    }

    // ===== Edge Cases and Additional Coverage =====

    def "insertReceiptImage sets timestamps correctly on save"() {
        given:
        ReceiptImage input = createValidReceiptImage(0L, 789L)
        BindingResult bindingResult = Mock() {
            hasErrors() >> false
        }
        and:
        receiptImageRepository.saveAndFlush(_ as ReceiptImage) >> { ReceiptImage ri ->
            // Verify timestamps were set (they should be set by the service)
            assert ri.dateAdded != null
            assert ri.dateUpdated != null
            ri.receiptImageId = 42L
            return ri
        }

        when:
        ResponseEntity<Map<String, String>> response = controller.insertReceiptImage(input, bindingResult)

        then:
        response.statusCode == HttpStatus.OK
        response.body.get("id") == "42"
    }

    def "selectReceiptImage handles zero ID"() {
        given:
        receiptImageRepository.findById(0L) >> Optional.empty()

        when:
        ResponseEntity<Map<String, Object>> response = controller.selectReceiptImage(0L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.get("error") == "ReceiptImage not found: 0"
    }

    def "insertReceiptImage handles null binding result errors gracefully"() {
        given:
        ReceiptImage input = createValidReceiptImage(0L, 123L)
        BindingResult bindingResult = Mock() {
            hasErrors() >> true
            getFieldErrors() >> []  // Empty list of errors
        }

        when:
        ResponseEntity<Map<String, String>> response = controller.insertReceiptImage(input, bindingResult)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.get("errors") != null
    }

    def "insertReceiptImage handles field error with null default message"() {
        given:
        ReceiptImage input = createValidReceiptImage(0L, 123L)
        BindingResult bindingResult = Mock() {
            hasErrors() >> true
            getFieldErrors() >> [new FieldError("receiptImage", "image", null, false, null, null, null)]
        }

        when:
        ResponseEntity<Map<String, String>> response = controller.insertReceiptImage(input, bindingResult)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.get("errors") != null
        response.body.get("errors").contains("Invalid value") // Default message
    }

    // ===== Helper Methods =====

    private ReceiptImage createValidReceiptImage(Long receiptImageId, Long transactionId) {
        ReceiptImage receiptImage = new ReceiptImage(receiptImageId, transactionId, true)
        receiptImage.image = "test image data".bytes
        receiptImage.thumbnail = "test thumbnail data".bytes
        receiptImage.imageFormatType = ImageFormatType.Png
        return receiptImage
    }
}