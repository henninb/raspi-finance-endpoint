package finance.utils

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.imageio.ImageReader

class ImageValidator : ConstraintValidator<ValidImage, ByteArray> {
    override fun initialize(constraintAnnotation: ValidImage) {
    }

    override fun isValid(
        value: ByteArray,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (value.isEmpty()) {
            return true
        }

        var flag = false
        val imageInputStream = ImageIO.createImageInputStream(ByteArrayInputStream(value))
        val imageReaders: Iterator<ImageReader> = ImageIO.getImageReaders(imageInputStream)

        imageReaders.forEachRemaining { imageReader ->
            flag =
                when {
                    imageReader.formatName.lowercase() == "jpeg" -> {
                        logger.debug("image format: ${imageReader.formatName}")
                        true
                    }
                    imageReader.formatName.lowercase() == "png" -> {
                        logger.debug("image format: ${imageReader.formatName}")
                        true
                    }
                    else -> {
                        false
                    }
                }
        }

        return flag
    }

    companion object {
        private val logger = LogManager.getLogger()
    }
}
