package finance.helpers

import finance.domain.ImageFormatType
import finance.domain.ReceiptImage

// curl -k --header "Content-Type: application/json" 'https://localhost:8080/receipt/image/insert' -X POST -d '{"activeStatus":true,"transactionId": 23189, "image":"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg==" }'
class ReceiptImageBuilder {
    Long receiptImageId = 0L
    Long transactionId = 22530
    Boolean activeStatus = true
    ImageFormatType imageFormatType = ImageFormatType.Png
    String thumbnail = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg=="
    String image =     "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg=="
    //String image = "/9j/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA8MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/yQALCAABAAEBAREA/8wABgAQEAX/2gAIAQEAAD8A0s8g/9k="

    static ReceiptImageBuilder builder() {
        return new ReceiptImageBuilder()
    }

    ReceiptImage build() {
        ReceiptImage receiptImage = new ReceiptImage().with {
            receiptImageId = this.receiptImageId
            transactionId = this.transactionId
            activeStatus = this.activeStatus
            imageFormatType = this.imageFormatType
            thumbnail = Base64.getDecoder().decode(this.thumbnail)
            image = Base64.getDecoder().decode(this.image)
            return it
        }
        return receiptImage
    }

    ReceiptImageBuilder withImage(String image) {
        this.image = image
        return this
    }

    ReceiptImageBuilder withThumbnail(String thumbnail) {
        this.thumbnail = thumbnail
        return this
    }

    ReceiptImageBuilder withImageFormatType(ImageFormatType imageFormatType) {
        this.imageFormatType = imageFormatType
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

    ReceiptImageBuilder withReceiptImageId(Long receiptImageId) {
        this.receiptImageId = receiptImageId
        return this
    }
}
