package finance.services

import finance.domain.ReceiptImage

class ReceiptImageServiceAdditionalSpec extends BaseServiceSpec {

    void 'findByReceiptImageId - present'() {
        given:
        def img = new ReceiptImage(receiptImageId: 9L, transactionId: 1L)

        when:
        def opt = receiptImageService.findByReceiptImageId(9L)

        then:
        1 * receiptImageRepositoryMock.findById(9L) >> Optional.of(img)
        opt.isPresent()
        opt.get().receiptImageId == 9L
    }

    void 'findByReceiptImageId - empty'() {
        when:
        def opt = receiptImageService.findByReceiptImageId(10L)

        then:
        1 * receiptImageRepositoryMock.findById(10L) >> Optional.empty()
        !opt.isPresent()
    }
}

