package finance.helpers

import finance.domain.ImageFormatType
import finance.domain.ReceiptImage
import groovy.util.logging.Slf4j

@Slf4j
class SmartReceiptImageBuilder {

    private String testOwner
    private Long transactionId = 1L
    private Boolean activeStatus = true
    private ImageFormatType imageFormatType = ImageFormatType.Png
    private String imageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg=="
    private String thumbnailBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mMMYfj/HwAEVwJUeAAUQgAAAABJRU5ErkJggg=="

    private SmartReceiptImageBuilder(String testOwner) {
        this.testOwner = testOwner
    }

    static SmartReceiptImageBuilder builderForOwner(String testOwner) {
        return new SmartReceiptImageBuilder(testOwner)
    }

    ReceiptImage build() {
        ReceiptImage receiptImage = new ReceiptImage().with {
            receiptImageId = 0L
            transactionId = this.transactionId
            activeStatus = this.activeStatus
            imageFormatType = this.imageFormatType
            image = Base64.getDecoder().decode(this.imageBase64)
            thumbnail = Base64.getDecoder().decode(this.thumbnailBase64)
            return it
        }
        return receiptImage
    }

    ReceiptImage buildAndValidate() {
        ReceiptImage ri = build()
        validateConstraints(ri)
        return ri
    }

    private void validateConstraints(ReceiptImage ri) {
        if (ri.transactionId == null || ri.transactionId < 0L) {
            throw new IllegalStateException("transactionId must be >= 0")
        }
        if (ri.image == null || ri.image.length == 0) {
            throw new IllegalStateException("image byte array must not be empty")
        }
        if (ri.thumbnail == null || ri.thumbnail.length == 0) {
            throw new IllegalStateException("thumbnail byte array must not be empty")
        }
        if (ri.imageFormatType == null || ri.imageFormatType == ImageFormatType.Undefined) {
            throw new IllegalStateException("imageFormatType must be set (png or jpeg)")
        }
        log.debug("ReceiptImage passed constraint validation: tx=${ri.transactionId}")
    }

    // Fluent API
    SmartReceiptImageBuilder withTransactionId(Long transactionId) {
        this.transactionId = transactionId
        return this
    }

    SmartReceiptImageBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    SmartReceiptImageBuilder withImageFormatType(ImageFormatType imageFormatType) {
        this.imageFormatType = imageFormatType
        return this
    }

    SmartReceiptImageBuilder withImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64
        return this
    }

    SmartReceiptImageBuilder withThumbnailBase64(String thumbnailBase64) {
        this.thumbnailBase64 = thumbnailBase64
        return this
    }

    // Convenience
    SmartReceiptImageBuilder asPng() {
        this.imageFormatType = ImageFormatType.Png
        return this
    }

    SmartReceiptImageBuilder asJpeg() {
        this.imageFormatType = ImageFormatType.Jpeg
        return this
    }

    SmartReceiptImageBuilder asActive() {
        this.activeStatus = true
        return this
    }

    SmartReceiptImageBuilder asInactive() {
        this.activeStatus = false
        return this
    }
}

