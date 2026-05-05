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

    void 'findByOwnerAndReceiptImageId returns image for correct owner'() {
        given:
        Long txnId = testDataManager.createTransactionAndGetId(testOwner)
        ReceiptImage ri = SmartReceiptImageBuilder.builderForOwner(testOwner)
                .withTransactionId(txnId)
                .asPng()
                .asActive()
                .buildAndValidate()
        ReceiptImage saved = receiptImageRepository.save(ri)

        when:
        def found = receiptImageRepository.findByOwnerAndReceiptImageId(testOwner, saved.receiptImageId)
        def wrongOwner = receiptImageRepository.findByOwnerAndReceiptImageId("wrong-owner", saved.receiptImageId)

        then:
        found.isPresent()
        found.get().receiptImageId == saved.receiptImageId
        found.get().owner == testOwner
        !wrongOwner.isPresent()
    }

    void 'findAllByOwner returns only images belonging to the owner'() {
        given:
        Long txnId1 = testDataManager.createTransactionAndGetId(testOwner)
        Long txnId2 = testDataManager.createTransactionAndGetId(testOwner)

        ReceiptImage ri1 = SmartReceiptImageBuilder.builderForOwner(testOwner)
                .withTransactionId(txnId1)
                .asPng()
                .asActive()
                .buildAndValidate()
        ReceiptImage ri2 = SmartReceiptImageBuilder.builderForOwner(testOwner)
                .withTransactionId(txnId2)
                .asJpeg()
                .asActive()
                .buildAndValidate()

        receiptImageRepository.save(ri1)
        receiptImageRepository.save(ri2)

        when:
        List<ReceiptImage> ownerImages = receiptImageRepository.findAllByOwner(testOwner)
        List<ReceiptImage> wrongOwnerImages = receiptImageRepository.findAllByOwner("wrong-owner")

        then:
        ownerImages.size() >= 2
        ownerImages.every { it.owner == testOwner }
        wrongOwnerImages.isEmpty()
    }

    void 'findByTransactionId returns image by transaction id'() {
        given:
        Long txnId = testDataManager.createTransactionAndGetId(testOwner)
        ReceiptImage ri = SmartReceiptImageBuilder.builderForOwner(testOwner)
                .withTransactionId(txnId)
                .asPng()
                .asActive()
                .buildAndValidate()
        ReceiptImage saved = receiptImageRepository.save(ri)

        when:
        def found = receiptImageRepository.findByTransactionId(txnId)
        def notFound = receiptImageRepository.findByTransactionId(-9999L)

        then:
        found.isPresent()
        found.get().receiptImageId == saved.receiptImageId
        !notFound.isPresent()
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
