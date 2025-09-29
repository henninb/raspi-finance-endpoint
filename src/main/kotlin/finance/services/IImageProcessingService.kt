package finance.services

import finance.domain.ImageFormatType

/**
 * Interface for image processing operations extracted from TransactionService
 * Handles receipt image processing in a focused, testable service
 */
interface IImageProcessingService {
    /**
     * Creates a thumbnail from raw image data
     * @param rawImage The raw image bytes
     * @param imageFormatType The format of the image (JPEG, PNG, etc.)
     * @return Thumbnail image as byte array (100x100 pixels)
     */
    fun createThumbnail(
        rawImage: ByteArray,
        imageFormatType: ImageFormatType,
    ): ByteArray

    /**
     * Determines the image format type from raw image data
     * @param rawImage The raw image bytes
     * @return ImageFormatType enum value (Jpeg, Png, or Undefined)
     */
    fun getImageFormatType(rawImage: ByteArray): ImageFormatType

    /**
     * Validates that the image size is within acceptable limits
     * @param rawImage The raw image bytes
     * @return true if image size is valid, false otherwise
     */
    fun validateImageSize(rawImage: ByteArray): Boolean

    /**
     * Processes raw image data into a complete image processing result
     * Combines format detection, validation, and thumbnail creation
     * @param rawImage The raw image bytes
     * @return ImageProcessingResult containing format, thumbnail, and validation status
     */
    fun processImage(rawImage: ByteArray): ImageProcessingResult
}

/**
 * Result of image processing operations
 */
data class ImageProcessingResult(
    val format: ImageFormatType,
    val thumbnail: ByteArray,
    val isValid: Boolean,
    val originalSize: Int,
    val thumbnailSize: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageProcessingResult

        if (format != other.format) return false
        if (!thumbnail.contentEquals(other.thumbnail)) return false
        if (isValid != other.isValid) return false
        if (originalSize != other.originalSize) return false
        if (thumbnailSize != other.thumbnailSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + thumbnail.contentHashCode()
        result = 31 * result + isValid.hashCode()
        result = 31 * result + originalSize
        result = 31 * result + thumbnailSize
        return result
    }
}
