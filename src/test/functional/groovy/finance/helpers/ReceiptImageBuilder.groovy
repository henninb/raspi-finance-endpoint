package finance.helpers

import finance.domain.ReceiptImage

import java.nio.charset.Charset

class ReceiptImageBuilder {
    Long transactionId = 1001
    Boolean activeStatus = true
    String jpgImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg=="
    //Byte[] jpgImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg==".getBytes(Charset.forName("UTF-8"))
    //Byte[] jpgImage = "data:image/jpg;base64,iVBORw0KGgoAAA".getBytes()

    static ReceiptImageBuilder builder() {
        return new ReceiptImageBuilder()
    }

    ReceiptImage build() {
        ReceiptImage receiptImage = new ReceiptImage().with {
            it.transactionId = this.transactionId
            it.activeStatus = this.activeStatus
            it.jpgImage = this.jpgImage.getBytes()
            return it
        }
        return receiptImage
    }

    ReceiptImageBuilder withJpgImage(String jpgImage) {
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
