package finance.services

import finance.domain.ImageFormatType
import spock.lang.Shared
import spock.lang.Unroll

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

/**
 * TDD Specification for ImageProcessingService
 * Tests the extracted image processing functionality from TransactionService
 */
class ImageProcessingServiceSpec extends BaseServiceSpec {

    protected ImageProcessingService imageProcessingService = new ImageProcessingService()

    @Shared
    byte[] validJpegImage

    @Shared
    byte[] validPngImage

    @Shared
    byte[] invalidImageData

    void setup() {
        imageProcessingService.meterService = meterService
        imageProcessingService.validator = validatorMock
    }

    def setupSpec() {
        // Create test image data
        validJpegImage = createTestJpegImage()
        validPngImage = createTestPngImage()
        invalidImageData = "invalid image data".getBytes()
    }

    // ===== TDD Tests for createThumbnail() =====

    def "should create thumbnail from valid JPEG image"() {
        given: "a valid JPEG image"
        byte[] jpegImage = validJpegImage

        when: "creating thumbnail"
        byte[] thumbnail = imageProcessingService.createThumbnail(jpegImage, ImageFormatType.Jpeg)

        then: "thumbnail should be created successfully"
        thumbnail != null
        thumbnail.length > 0
        thumbnail.length < jpegImage.length // Thumbnail should be smaller
        0 * _  // Focus on functionality, not implementation details
    }

    def "should create thumbnail from valid PNG image"() {
        given: "a valid PNG image"
        byte[] pngImage = validPngImage

        when: "creating thumbnail"
        byte[] thumbnail = imageProcessingService.createThumbnail(pngImage, ImageFormatType.Png)

        then: "thumbnail should be created successfully"
        thumbnail != null
        thumbnail.length > 0
        thumbnail.length < pngImage.length // Thumbnail should be smaller
        0 * _  // Focus on functionality, not implementation details
    }

    def "should return empty byte array for invalid image data"() {
        given: "invalid image data"
        byte[] invalidImage = invalidImageData

        when: "creating thumbnail"
        byte[] thumbnail = imageProcessingService.createThumbnail(invalidImage, ImageFormatType.Jpeg)

        then: "should return empty byte array"
        thumbnail != null
        thumbnail.length == 0
        0 * _  // Focus on functionality, not implementation details
    }

    // ===== TDD Tests for getImageFormatType() =====

    def "should detect JPEG format correctly"() {
        given: "a valid JPEG image"
        byte[] jpegImage = validJpegImage

        when: "detecting image format"
        ImageFormatType format = imageProcessingService.getImageFormatType(jpegImage)

        then: "should return JPEG format"
        format == ImageFormatType.Jpeg
    }

    def "should detect PNG format correctly"() {
        given: "a valid PNG image"
        byte[] pngImage = validPngImage

        when: "detecting image format"
        ImageFormatType format = imageProcessingService.getImageFormatType(pngImage)

        then: "should return PNG format"
        format == ImageFormatType.Png
    }

    def "should return Undefined for invalid image data"() {
        given: "invalid image data"
        byte[] invalidImage = invalidImageData

        when: "detecting image format"
        ImageFormatType format = imageProcessingService.getImageFormatType(invalidImage)

        then: "should return Undefined format"
        format == ImageFormatType.Undefined
    }

    // ===== TDD Tests for validateImageSize() =====

    @Unroll
    def "should validate image size correctly for #description"() {
        given: "image data of size #imageSize bytes"
        byte[] testImage = new byte[imageSize]

        when: "validating image size"
        boolean isValid = imageProcessingService.validateImageSize(testImage)

        then: "validation result should be #expectedResult"
        isValid == expectedResult

        where:
        description           | imageSize    | expectedResult
        "small image"        | 1024         | true
        "medium image"       | 1024 * 500   | true  // 500KB
        "large valid image"  | 1024 * 1024  | true  // 1MB
        "oversized image"    | 1024 * 1024 * 10 | false // 10MB
        "empty image"        | 0            | false
    }

    // ===== TDD Tests for processImage() =====

    def "should process valid JPEG image completely"() {
        given: "a valid JPEG image"
        byte[] jpegImage = validJpegImage

        when: "processing image"
        ImageProcessingResult result = imageProcessingService.processImage(jpegImage)

        then: "should return complete processing result"
        result != null
        result.format == ImageFormatType.Jpeg
        result.thumbnail != null
        result.thumbnail.length > 0
        result.isValid == true
        result.originalSize == jpegImage.length
        result.thumbnailSize == result.thumbnail.length
        result.thumbnailSize < result.originalSize
    }

    def "should process invalid image data gracefully"() {
        given: "invalid image data"
        byte[] invalidImage = invalidImageData

        when: "processing image"
        ImageProcessingResult result = imageProcessingService.processImage(invalidImage)

        then: "should return failed processing result"
        result != null
        result.format == ImageFormatType.Undefined
        result.thumbnail != null
        result.thumbnail.length == 0
        result.isValid == false
        result.originalSize == invalidImage.length
        result.thumbnailSize == 0
    }

    // ===== TDD Tests for Integration with Existing TransactionService Behavior =====

    def "should maintain compatibility with existing thumbnail creation logic"() {
        given: "image data similar to what TransactionService would use"
        byte[] imageData = validJpegImage
        ImageFormatType detectedFormat = imageProcessingService.getImageFormatType(imageData)

        when: "creating thumbnail using the same pattern as TransactionService"
        byte[] thumbnail = imageProcessingService.createThumbnail(imageData, detectedFormat)

        then: "should produce equivalent results to original TransactionService logic"
        thumbnail != null
        thumbnail.length > 0
        detectedFormat == ImageFormatType.Jpeg

        and: "thumbnail should be approximately 100x100 pixels when rendered"
        // Note: Exact size verification would require image parsing,
        // but we verify the thumbnail is smaller than original
        thumbnail.length < imageData.length
    }

    // ===== Test Data Helper Methods =====

    private byte[] createTestJpegImage() {
        // Create a simple JPEG image for testing
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB)

        // Fill with a simple pattern
        for (int x = 0; x < 200; x++) {
            for (int y = 0; y < 200; y++) {
                int rgb = (x + y) % 255
                image.setRGB(x, y, (rgb << 16) | (rgb << 8) | rgb)
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ImageIO.write(image, "JPEG", baos)
        return baos.toByteArray()
    }

    private byte[] createTestPngImage() {
        // Create a simple PNG image for testing
        BufferedImage image = new BufferedImage(150, 150, BufferedImage.TYPE_INT_ARGB)

        // Fill with a simple pattern
        for (int x = 0; x < 150; x++) {
            for (int y = 0; y < 150; y++) {
                int rgb = (x * y) % 255
                image.setRGB(x, y, (255 << 24) | (rgb << 16) | (rgb << 8) | rgb) // ARGB
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ImageIO.write(image, "PNG", baos)
        return baos.toByteArray()
    }
}