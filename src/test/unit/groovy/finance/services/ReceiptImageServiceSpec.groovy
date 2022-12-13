package finance.services

import finance.domain.ReceiptImage
import finance.helpers.ReceiptImageBuilder
import jakarta.validation.ConstraintViolation


class ReceiptImageServiceSpec extends BaseServiceSpec {
    void setup() {
        receiptImageService.validator = validatorMock
        receiptImageService.meterService = meterService
    }

    void 'test - insert receiptImage'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()
        Set<ConstraintViolation<ReceiptImage>> constraintViolations = validator.validate(receiptImage)

        when:
        ReceiptImage receiptImageInserted = receiptImageService.insertReceiptImage(receiptImage)

        then:
        receiptImageInserted.transactionId == receiptImage.transactionId
        1 * validatorMock.validate(receiptImage) >> constraintViolations
        1 * receiptImageRepositoryMock.saveAndFlush(receiptImage) >> receiptImage
        0 * _
    }

//    void 'test - insert receiptImage empty receiptImage'() {
//        given:
//        ReceiptImage receiptImage = ReceiptImageBuilder.builder().withImage('').build()
//        Set<ConstraintViolation<ReceiptImage>> constraintViolations = validator.validate(receiptImage)
//
//        when:
//        receiptImageService.insertReceiptImage(receiptImage)
//
//        then:
//        constraintViolations.size() == 1
//        thrown(ValidationException)
//        1 * validatorMock.validate(receiptImage) >> constraintViolations
//        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
//        1 * counter.increment()
//        0 * _
//    }

    void 'test - delete receiptImage'() {
        given:
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()

        when:
        receiptImageService.deleteReceiptImage(receiptImage)

        then:
        1 * receiptImageRepositoryMock.deleteById(receiptImage.receiptImageId)
        0 * _
    }
}
