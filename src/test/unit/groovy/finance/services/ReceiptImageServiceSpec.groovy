package finance.services

import finance.domain.ImageFormatType
import finance.domain.ReceiptImage
import finance.domain.ServiceResult
import finance.helpers.ReceiptImageBuilder
import finance.repositories.ReceiptImageRepository
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import java.util.Optional

/**
 * TDD Test Specification for ReceiptImageService
 * Following the established ServiceResult pattern and TDD methodology
 */
class StandardizedReceiptImageServiceSpec extends BaseServiceSpec {

    def receiptImageRepositoryMock = Mock(ReceiptImageRepository)
    def standardizedReceiptImageService = new ReceiptImageService(receiptImageRepositoryMock)

    void setup() {
        standardizedReceiptImageService.meterService = meterService
        standardizedReceiptImageService.validator = validator
    }

    def "should have correct entity name"() {
        expect:
        standardizedReceiptImageService.getEntityName() == "ReceiptImage"
    }

    // ===== findAllActive Tests =====

    def "findAllActive should return ServiceResult.Success with list of receipt images"() {
        given:
        def image1 = ReceiptImageBuilder.builder().withTransactionId(1L).build()
        def image2 = ReceiptImageBuilder.builder().withTransactionId(2L).build()
        def images = [image1, image2]

        when:
        receiptImageRepositoryMock.findAllByOwner(TEST_OWNER) >> images
        def result = standardizedReceiptImageService.findAllActive()

        then:
        result instanceof ServiceResult.Success
        result.data.size() == 2
        result.data == images
    }

    def "findAllActive should return ServiceResult.Success with empty list when no images"() {
        given:
        receiptImageRepositoryMock.findAllByOwner(TEST_OWNER) >> []

        when:
        def result = standardizedReceiptImageService.findAllActive()

        then:
        result instanceof ServiceResult.Success
        result.data.isEmpty()
    }

    def "findAllActive should return ServiceResult.SystemError on repository exception"() {
        given:
        receiptImageRepositoryMock.findAllByOwner(TEST_OWNER) >> { throw new RuntimeException("Database error") }

        when:
        def result = standardizedReceiptImageService.findAllActive()

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Database error")
    }

    // ===== findById Tests =====

    def "findById should return ServiceResult.Success when receipt image exists"() {
        given:
        def imageId = 1L
        def image = ReceiptImageBuilder.builder().build()
        image.receiptImageId = imageId

        when:
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, imageId) >> Optional.of(image)
        def result = standardizedReceiptImageService.findById(imageId)

        then:
        result instanceof ServiceResult.Success
        result.data == image
    }

    def "findById should return ServiceResult.NotFound when receipt image does not exist"() {
        given:
        def imageId = 999L

        when:
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, imageId) >> Optional.empty()
        def result = standardizedReceiptImageService.findById(imageId)

        then:
        result instanceof ServiceResult.NotFound
        result.message == "ReceiptImage not found: 999"
    }

    def "findById should return ServiceResult.SystemError on repository exception"() {
        given:
        def imageId = 1L
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, imageId) >> { throw new RuntimeException("Database connection failed") }

        when:
        def result = standardizedReceiptImageService.findById(imageId)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Database connection failed")
    }

    // ===== save Tests =====

    def "save should return ServiceResult.Success when valid receipt image"() {
        given:
        def image = ReceiptImageBuilder.builder()
                .withTransactionId(123L)
                .build()
        def savedImage = ReceiptImageBuilder.builder()
                .withTransactionId(123L)
                .build()
        savedImage.receiptImageId = 1L

        when:
        receiptImageRepositoryMock.saveAndFlush(image) >> savedImage
        def result = standardizedReceiptImageService.save(image)

        then:
        result instanceof ServiceResult.Success
        result.data == savedImage
    }

    def "save should return ServiceResult.ValidationError on constraint violation"() {
        given:
        def image = ReceiptImageBuilder.builder()
                .withTransactionId(-1L) // Invalid negative transaction ID
                .build()

        // Mock validation to return constraint violations
        def violation = Mock(jakarta.validation.ConstraintViolation)
        def mockPath = Mock(jakarta.validation.Path)
        mockPath.toString() >> "transactionId"
        violation.propertyPath >> mockPath
        violation.message >> "must be greater than or equal to 0"
        Set<jakarta.validation.ConstraintViolation<ReceiptImage>> violations = [violation] as Set

        when:
        standardizedReceiptImageService.validator = validatorMock
        validatorMock.validate(image) >> violations
        def result = standardizedReceiptImageService.save(image)

        then:
        result instanceof ServiceResult.ValidationError
        result.errors.containsKey("transactionId")
    }

    def "save should return ServiceResult.SystemError on repository exception"() {
        given:
        def image = ReceiptImageBuilder.builder().build()
        receiptImageRepositoryMock.saveAndFlush(image) >> { throw new RuntimeException("Save failed") }

        when:
        def result = standardizedReceiptImageService.save(image)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Save failed")
    }

    // ===== update Tests =====

    def "update should return ServiceResult.Success when receipt image exists"() {
        given:
        def image = ReceiptImageBuilder.builder().build()
        image.receiptImageId = 1L
        def existingImage = ReceiptImageBuilder.builder().build()
        existingImage.receiptImageId = 1L

        when:
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, 1L) >> Optional.of(existingImage)
        receiptImageRepositoryMock.saveAndFlush(image) >> image
        def result = standardizedReceiptImageService.update(image)

        then:
        result instanceof ServiceResult.Success
        result.data == image
    }

    def "update should return ServiceResult.NotFound when receipt image does not exist"() {
        given:
        def image = ReceiptImageBuilder.builder().build()
        image.receiptImageId = 999L

        when:
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, 999L) >> Optional.empty()
        def result = standardizedReceiptImageService.update(image)

        then:
        result instanceof ServiceResult.NotFound
        result.message == "ReceiptImage not found: 999"
    }

    def "update should return ServiceResult.SystemError on repository exception"() {
        given:
        def image = ReceiptImageBuilder.builder().build()
        image.receiptImageId = 1L
        def existingImage = ReceiptImageBuilder.builder().build()
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, 1L) >> Optional.of(existingImage)
        receiptImageRepositoryMock.saveAndFlush(image) >> { throw new RuntimeException("Update failed") }

        when:
        def result = standardizedReceiptImageService.update(image)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Update failed")
    }

    // ===== deleteById Tests =====

    def "deleteById should return ServiceResult.Success when receipt image exists"() {
        given:
        def imageId = 1L
        def image = ReceiptImageBuilder.builder().build()
        image.receiptImageId = imageId

        when:
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, imageId) >> Optional.of(image)
        receiptImageRepositoryMock.deleteById(imageId) >> {}
        def result = standardizedReceiptImageService.deleteById(imageId)

        then:
        result instanceof ServiceResult.Success
        result.data == true
    }

    def "deleteById should return ServiceResult.NotFound when receipt image does not exist"() {
        given:
        def imageId = 999L

        when:
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, imageId) >> Optional.empty()
        def result = standardizedReceiptImageService.deleteById(imageId)

        then:
        result instanceof ServiceResult.NotFound
        result.message == "ReceiptImage not found: 999"
    }

    def "deleteById should return ServiceResult.SystemError on repository exception"() {
        given:
        def imageId = 1L
        def image = ReceiptImageBuilder.builder().build()
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, imageId) >> Optional.of(image)
        receiptImageRepositoryMock.deleteById(imageId) >> { throw new RuntimeException("Delete failed") }

        when:
        def result = standardizedReceiptImageService.deleteById(imageId)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Delete failed")
    }

    // ===== findByTransactionId Tests =====

    def "findByTransactionId should return ServiceResult.Success when receipt image exists"() {
        given:
        def transactionId = 123L
        def image = ReceiptImageBuilder.builder().withTransactionId(transactionId).build()

        when:
        receiptImageRepositoryMock.findByOwnerAndTransactionId(TEST_OWNER, transactionId) >> Optional.of(image)
        def result = standardizedReceiptImageService.findByTransactionId(transactionId)

        then:
        result instanceof ServiceResult.Success
        result.data == image
    }

    def "findByTransactionId should return ServiceResult.NotFound when no receipt image for transaction"() {
        given:
        def transactionId = 999L

        when:
        receiptImageRepositoryMock.findByOwnerAndTransactionId(TEST_OWNER, transactionId) >> Optional.empty()
        def result = standardizedReceiptImageService.findByTransactionId(transactionId)

        then:
        result instanceof ServiceResult.NotFound
        result.message == "ReceiptImage not found: 999"
    }

    def "findByTransactionId should return ServiceResult.SystemError on repository exception"() {
        given:
        def transactionId = 123L
        receiptImageRepositoryMock.findByOwnerAndTransactionId(TEST_OWNER, transactionId) >> { throw new RuntimeException("Database error") }

        when:
        def result = standardizedReceiptImageService.findByTransactionId(transactionId)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Database error")
    }

    // ===== Legacy Method Compatibility Tests =====

    def "save should return ServiceResult.Success with receipt image"() {
        given:
        def image = ReceiptImageBuilder.builder().withTransactionId(123L).build()
        def savedImage = ReceiptImageBuilder.builder().withTransactionId(123L).build()
        savedImage.receiptImageId = 1L

        when:
        receiptImageRepositoryMock.saveAndFlush(image) >> savedImage
        def result = standardizedReceiptImageService.save(image)

        then:
        result instanceof ServiceResult.Success
        result.data == savedImage
        result.data.receiptImageId == 1L
    }

    def "save should return ServiceResult.ValidationError on validation failure"() {
        given:
        def image = ReceiptImageBuilder.builder()
                .withTransactionId(-1L)
                .build()

        // Mock validation to return constraint violations
        def violation = Mock(jakarta.validation.ConstraintViolation)
        def mockPath = Mock(jakarta.validation.Path)
        mockPath.toString() >> "transactionId"
        violation.propertyPath >> mockPath
        violation.message >> "must be greater than or equal to 0"
        Set<jakarta.validation.ConstraintViolation<ReceiptImage>> violations = [violation] as Set

        when:
        standardizedReceiptImageService.validator = validatorMock
        validatorMock.validate(image) >> violations
        def result = standardizedReceiptImageService.save(image)

        then:
        result instanceof ServiceResult.ValidationError
        result.errors.transactionId == "must be greater than or equal to 0"
    }

    def "save should return ServiceResult.SystemError on repository exception"() {
        given:
        def image = ReceiptImageBuilder.builder().build()
        receiptImageRepositoryMock.saveAndFlush(image) >> { throw new RuntimeException("Save failed") }

        when:
        def result = standardizedReceiptImageService.save(image)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message == "Save failed"
    }

    def "findById should return ServiceResult.Success when image found"() {
        given:
        def imageId = 1L
        def image = ReceiptImageBuilder.builder().build()
        image.receiptImageId = imageId

        when:
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, imageId) >> Optional.of(image)
        def result = standardizedReceiptImageService.findById(imageId)

        then:
        result instanceof ServiceResult.Success
        result.data == image
    }

    def "findById should return ServiceResult.NotFound when image not found"() {
        given:
        def imageId = 999L

        when:
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, imageId) >> Optional.empty()
        def result = standardizedReceiptImageService.findById(imageId)

        then:
        result instanceof ServiceResult.NotFound
        result.message.contains("ReceiptImage not found: 999")
    }

    def "deleteById should return ServiceResult.Success when deleting receipt image"() {
        given:
        def imageId = 1L
        def image = ReceiptImageBuilder.builder().build()
        image.receiptImageId = imageId

        when:
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, imageId) >> Optional.of(image)
        receiptImageRepositoryMock.deleteById(imageId) >> {}
        def result = standardizedReceiptImageService.deleteById(imageId)

        then:
        result instanceof ServiceResult.Success
        result.data == true
    }

    def "deleteById should return ServiceResult.SystemError on exception"() {
        given:
        def imageId = 1L
        def image = ReceiptImageBuilder.builder().build()
        image.receiptImageId = imageId

        when:
        receiptImageRepositoryMock.findByOwnerAndReceiptImageId(TEST_OWNER, imageId) >> Optional.of(image)
        receiptImageRepositoryMock.deleteById(imageId) >> { throw new RuntimeException("Delete failed") }
        def result = standardizedReceiptImageService.deleteById(imageId)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message == "Delete failed"
    }
}