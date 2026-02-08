package finance.helpers

import finance.domain.ImageFormatType
import finance.domain.ReceiptImage

/*
 Multi-line example curl for reference:
 curl -k --header "Content-Type: application/json" \
   'https://localhost:8080/receipt/image/insert' \
   -X POST \
   -d '{"activeStatus":true,"transactionId":23189,"image":"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg=="}'
*/
class ReceiptImageBuilder {
    private static final String SAMPLE_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg=="
    Long receiptImageId = 0L
    String owner = 'test_owner'
    Long transactionId = 22530
    Boolean activeStatus = true
    ImageFormatType imageFormatType = ImageFormatType.Png
    String thumbnail = SAMPLE_BASE64
    String image =     SAMPLE_BASE64
    //String image = "/9j/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA8MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/yQALCAABAAEBAREA/8wABgAQEAX/2gAIAQEAAD8A0s8g/9k="

    static ReceiptImageBuilder builder() {
        return new ReceiptImageBuilder()
    }

    ReceiptImage build() {
        ReceiptImage receiptImage = new ReceiptImage().with {
            receiptImageId = this.receiptImageId
            owner = this.owner
            transactionId = this.transactionId
            activeStatus = this.activeStatus
            imageFormatType = this.imageFormatType
            thumbnail = Base64.getDecoder().decode(this.thumbnail)
            image = Base64.getDecoder().decode(this.image)
            it
        }
        receiptImage
    }

    ReceiptImageBuilder withOwner(String owner) {
        this.owner = owner
        this
    }

    ReceiptImageBuilder withImage(String image) {
        this.image = image
        this
    }

    ReceiptImageBuilder withThumbnail(String thumbnail) {
        this.thumbnail = thumbnail
        this
    }

    ReceiptImageBuilder withImageFormatType(ImageFormatType imageFormatType) {
        this.imageFormatType = imageFormatType
        this
    }

    ReceiptImageBuilder withTransactionId(Long transactionId) {
        this.transactionId = transactionId
        this
    }

    ReceiptImageBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        this
    }

    ReceiptImageBuilder withReceiptImageId(Long receiptImageId) {
        this.receiptImageId = receiptImageId
        this
    }
}
