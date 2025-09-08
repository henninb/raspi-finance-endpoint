package finance.utils

import spock.lang.Specification

import javax.imageio.ImageIO
import jakarta.validation.ConstraintValidatorContext
import java.awt.image.BufferedImage
import java.awt.Color
import java.io.ByteArrayOutputStream

class ImageValidatorSpec extends Specification {
    def validator = new ImageValidator()
    def context = Mock(ConstraintValidatorContext)

    private static byte[] makePngBytes(int w = 1, int h = 1, Color color = Color.RED) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, color.getRGB())
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ImageIO.write(img, 'png', baos)
        return baos.toByteArray()
    }

    def "empty byte array is considered valid"() {
        expect:
        validator.isValid(new byte[0], context)
    }

    def "valid PNG bytes are accepted"() {
        given:
        byte[] bytes = makePngBytes()

        expect:
        validator.isValid(bytes, context)
    }

    def "random bytes are rejected"() {
        given:
        byte[] bytes = new byte[32]
        new Random(1234).nextBytes(bytes)

        expect:
        !validator.isValid(bytes, context)
    }
}
