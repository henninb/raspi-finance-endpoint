package finance.services

import finance.domain.ImageFormatType
import net.coobird.thumbnailator.Thumbnails
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOException
import javax.imageio.ImageIO
import javax.imageio.ImageReader

/**
 * Service for handling image processing operations
 * Extracted from TransactionService for better separation of concerns and testability
 */
@Service
open class ImageProcessingService :
    BaseService(),
    IImageProcessingService {
    companion object {
        private const val THUMBNAIL_SIZE = 100
        private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024 // 5MB
    }

    override fun createThumbnail(
        rawImage: ByteArray,
        imageFormatType: ImageFormatType,
    ): ByteArray {
        return try {
            logger.debug("Creating thumbnail for image of size: ${rawImage.size} bytes, format: $imageFormatType")

            if (rawImage.isEmpty()) {
                logger.warn("Cannot create thumbnail from empty image data")
                meterService.incrementExceptionCaughtCounter("EmptyImageData")
                return byteArrayOf()
            }

            if (imageFormatType == ImageFormatType.Undefined) {
                logger.warn("Cannot create thumbnail for undefined image format")
                meterService.incrementExceptionCaughtCounter("UndefinedImageFormat")
                return byteArrayOf()
            }

            val bufferedImage = ImageIO.read(ByteArrayInputStream(rawImage))

            if (bufferedImage == null) {
                logger.warn("Could not read image data for thumbnail creation (BufferedImage is null)")
                meterService.incrementExceptionCaughtCounter("ImageReadFailure")
                return byteArrayOf()
            }

            val thumbnail =
                Thumbnails
                    .of(bufferedImage)
                    .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                    .asBufferedImage()

            val byteArrayOutputStream = ByteArrayOutputStream()
            val writeSuccess = ImageIO.write(thumbnail, imageFormatType.toString(), byteArrayOutputStream)

            if (!writeSuccess) {
                logger.warn("Failed to write thumbnail image in format: $imageFormatType")
                meterService.incrementExceptionCaughtCounter("ThumbnailWriteFailure")
                return byteArrayOf()
            }

            val thumbnailBytes = byteArrayOutputStream.toByteArray()
            logger.info("Successfully created thumbnail: ${thumbnailBytes.size} bytes from original ${rawImage.size} bytes")
            meterService.incrementExceptionThrownCounter("ThumbnailCreated")

            return thumbnailBytes
        } catch (iIOException: IIOException) {
            logger.warn("IIOException during thumbnail creation: ${iIOException.message}")
            meterService.incrementExceptionCaughtCounter("IIOException")
            return byteArrayOf()
        } catch (ex: Exception) {
            logger.error("Unexpected error during thumbnail creation: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("ThumbnailCreationError")
            return byteArrayOf()
        }
    }

    override fun getImageFormatType(rawImage: ByteArray): ImageFormatType {
        return try {
            logger.debug("Detecting image format for image of size: ${rawImage.size} bytes")

            val imageInputStream = ImageIO.createImageInputStream(ByteArrayInputStream(rawImage))
            if (imageInputStream == null) {
                logger.warn("Failed to create ImageInputStream for image format detection")
                meterService.incrementExceptionCaughtCounter("ImageInputStreamCreationFailed")
                return ImageFormatType.Undefined
            }

            try {
                val imageReaders: Iterator<ImageReader> = ImageIO.getImageReaders(imageInputStream)
                var format = ImageFormatType.Undefined

                imageReaders.forEachRemaining { imageReader ->
                    format =
                        when {
                            imageReader.formatName.lowercase() == "jpeg" -> {
                                logger.info("Detected image format: ${imageReader.formatName}")
                                ImageFormatType.Jpeg
                            }

                            imageReader.formatName.lowercase() == "png" -> {
                                logger.info("Detected image format: ${imageReader.formatName}")
                                ImageFormatType.Png
                            }

                            else -> {
                                logger.debug("Unsupported image format: ${imageReader.formatName}")
                                ImageFormatType.Undefined
                            }
                        }
                }

                logger.debug("Final detected format: $format")
                return format
            } finally {
                // Always close the ImageInputStream to prevent resource leak
                try {
                    imageInputStream.close()
                    logger.debug("ImageInputStream closed successfully")
                } catch (ex: Exception) {
                    logger.warn("Failed to close ImageInputStream: ${ex.message}")
                }
            }
        } catch (ex: Exception) {
            logger.error("Error detecting image format: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("ImageFormatDetectionError")
            return ImageFormatType.Undefined
        }
    }

    override fun validateImageSize(rawImage: ByteArray): Boolean {
        return try {
            logger.debug("Validating image size: ${rawImage.size} bytes")

            val isValid = rawImage.isNotEmpty() && rawImage.size <= MAX_IMAGE_SIZE_BYTES

            if (!isValid) {
                if (rawImage.isEmpty()) {
                    logger.warn("Image validation failed: empty image data")
                } else {
                    logger.warn("Image validation failed: size ${rawImage.size} exceeds maximum ${MAX_IMAGE_SIZE_BYTES}")
                }
                meterService.incrementExceptionCaughtCounter("ImageSizeValidationFailed")
            } else {
                logger.debug("Image size validation passed: ${rawImage.size} bytes")
            }

            return isValid
        } catch (ex: Exception) {
            logger.error("Error during image size validation: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("ImageValidationError")
            return false
        }
    }

    override fun processImage(rawImage: ByteArray): ImageProcessingResult {
        return try {
            logger.debug("Starting complete image processing for ${rawImage.size} bytes")

            // Step 1: Detect format
            val format = getImageFormatType(rawImage)

            // Step 2: Validate size
            val isValid = validateImageSize(rawImage) && format != ImageFormatType.Undefined

            // Step 3: Create thumbnail (attempt even if validation failed for consistency)
            val thumbnail =
                if (isValid) {
                    createThumbnail(rawImage, format)
                } else {
                    logger.debug("Skipping thumbnail creation due to validation failure")
                    byteArrayOf()
                }

            val result =
                ImageProcessingResult(
                    format = format,
                    thumbnail = thumbnail,
                    isValid = isValid,
                    originalSize = rawImage.size,
                    thumbnailSize = thumbnail.size,
                )

            logger.info("Image processing completed: format=$format, valid=$isValid, original=${result.originalSize}, thumbnail=${result.thumbnailSize}")
            meterService.incrementExceptionThrownCounter("ImageProcessingCompleted")

            return result
        } catch (ex: Exception) {
            logger.error("Error during complete image processing: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("ImageProcessingError")

            return ImageProcessingResult(
                format = ImageFormatType.Undefined,
                thumbnail = byteArrayOf(),
                isValid = false,
                originalSize = rawImage.size,
                thumbnailSize = 0,
            )
        }
    }
}
