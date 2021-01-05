package finance.helpers

import finance.domain.ReceiptImage

class ReceiptImageBuilder {
    Long transactionId = 1001
    Boolean activeStatus = true
    Byte[] jpgImage = null

    static ReceiptImageBuilder builder() {
        return new ReceiptImageBuilder()
    }

    ReceiptImage build() {
        ReceiptImage receiptImage = new ReceiptImage().with {
            it.transactionId = this.transactionId
            it.activeStatus = this.activeStatus
            it.jpgImage = this.jpgImage
            return it
        }
        return receiptImage
    }

    ReceiptImageBuilder withJpgImage(Byte[] jpgImage) {
        this.jpgImage = jpgImage
        return this
    }

    ReceiptImageBuilder withTransactionId(Long transactionId) {
        this.transactionId = transactionId
        return this
    }

    ReceiptImageBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }
}
