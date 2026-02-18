package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.ReceiptImage
import finance.helpers.SmartReceiptImageBuilder
import org.springframework.beans.factory.annotation.Autowired

class ReceiptImageRepositoryIntSpec extends BaseIntegrationSpec {

    @Autowired
    ReceiptImageRepository receiptImageRepository

    void 'receipt image basic CRUD operations'() {
        given:
        Long txnId = testDataManager.createTransactionAndGetId(testOwner)
        ReceiptImage ri = SmartReceiptImageBuilder.builderForOwner(testOwner)
                .withTransactionId(txnId)
                .asPng()
                .asActive()
                .buildAndValidate()

        when:
        ReceiptImage saved = receiptImageRepository.save(ri)

        then:
        saved.receiptImageId != null
        saved.transactionId == txnId
        saved.image != null && saved.image.length > 0
        saved.thumbnail != null && saved.thumbnail.length > 0

        when:
        def found = receiptImageRepository.findByOwnerAndTransactionId(testOwner, txnId)

        then:
        found.isPresent()
        found.get().receiptImageId == saved.receiptImageId

        when:
        receiptImageRepository.delete(saved)
        def afterDelete = receiptImageRepository.findByOwnerAndTransactionId(testOwner, txnId)

        then:
        !afterDelete.isPresent()
    }

    void 'invalid image bytes trigger constraint violation'() {
        given:
        Long txnId = testDataManager.createTransactionAndGetId(testOwner)
        // Use non-image base64 data to produce invalid bytes for validator
        String badBase64 = 'aGVsbG8=' // "hello"
        ReceiptImage ri = SmartReceiptImageBuilder.builderForOwner(testOwner)
                .withTransactionId(txnId)
                .asPng()
                .asActive()
                .build() // bypass builder validation
        ri.image = java.util.Base64.getDecoder().decode(badBase64)
        ri.thumbnail = java.util.Base64.getDecoder().decode(badBase64)

        when:
        receiptImageRepository.save(ri)
        receiptImageRepository.flush()

        then:
        thrown(jakarta.validation.ConstraintViolationException)
    }

    void 'invalid thumbnail bytes trigger constraint violation while image is valid'() {
        given:
        Long txnId = testDataManager.createTransactionAndGetId(testOwner)
        // Start with valid image bytes from builder, then corrupt only the thumbnail
        String badBase64 = 'aGVsbG8=' // "hello"
        ReceiptImage ri = SmartReceiptImageBuilder.builderForOwner(testOwner)
                .withTransactionId(txnId)
                .asJpeg()
                .asActive()
                .build() // keep valid image bytes from builder
        // Leave ri.image as valid; corrupt thumbnail only
        ri.thumbnail = java.util.Base64.getDecoder().decode(badBase64)

        when:
        receiptImageRepository.save(ri)
        receiptImageRepository.flush()

        then:
        thrown(jakarta.validation.ConstraintViolationException)
    }
}
