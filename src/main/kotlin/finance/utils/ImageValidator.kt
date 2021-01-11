package finance.utils

import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class ImageValidator : ConstraintValidator<ValidImage, ByteArray> {
    override fun initialize(constraintAnnotation: ValidImage) {
    }

    override fun isValid(value: ByteArray, context: ConstraintValidatorContext): Boolean {

        if(value.isEmpty()) {
            return true
        }

        var flag = false
        val imageInputStream = ImageIO.createImageInputStream(ByteArrayInputStream(value))
        val imageReaders: Iterator<ImageReader> = ImageIO.getImageReaders(imageInputStream)

        imageReaders.forEachRemaining { imageReader ->
            flag = when {
                imageReader.formatName.toLowerCase() == "jpeg" -> {
                    logger.info(imageReader.formatName)
                    true
                }
                imageReader.formatName.toLowerCase() == "png" -> {
                    logger.info(imageReader.formatName)
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