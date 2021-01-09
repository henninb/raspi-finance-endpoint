package finance.helpers

import finance.domain.ReceiptImage
import org.springframework.util.Base64Utils

// curl -k --header "Content-Type: application/json" 'https://localhost:8080/receipt/image/insert' -X POST -d '{"activeStatus":true,"transactionId": 23189, "jpgImage":"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg==" }'
// https://cryptii.com/pipes/base64-to-hex
class ReceiptImageBuilder {
    Long transactionId = 22530
    Boolean activeStatus = true
    //String jpgImage = "data:image/jpeg;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg=="
    String jpgImage = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg=="
    //String jpgImage = "/9j/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA8MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/yQALCAABAAEBAREA/8wABgAQEAX/2gAIAQEAAD8A0s8g/9k="

    static ReceiptImageBuilder builder() {
        return new ReceiptImageBuilder()
    }

    ReceiptImage build() {
        ReceiptImage receiptImage = new ReceiptImage().with {
            transactionId = this.transactionId
            activeStatus = this.activeStatus
            //jpgImage = this.jpgImage.getBytes()
            jpgImage = Base64Utils.decodeFromString(this.jpgImage)
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
