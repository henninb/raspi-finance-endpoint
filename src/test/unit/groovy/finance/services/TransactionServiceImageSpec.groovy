package finance.services

import finance.domain.ImageFormatType

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.Color
import java.io.ByteArrayOutputStream

class TransactionServiceImageSpec extends BaseServiceSpec {

    private byte[] pngBytes() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        for (int x=0; x<10; x++) {
            for (int y=0; y<10; y++) {
                img.setRGB(x, y, new Color(0, 128, 255).getRGB())
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream()
        ImageIO.write(img, 'png', bos)
        return bos.toByteArray()
    }

    void 'getImageFormatType returns Png for PNG bytes'() {
        given:
        byte[] bytes = pngBytes()

        when:
        def format = transactionService.getImageFormatType(bytes)

        then:
        format == ImageFormatType.Png
    }

    void 'getImageFormatType returns Undefined for random bytes'() {
        given:
        byte[] bytes = 'not-an-image'.bytes

        when:
        def format = transactionService.getImageFormatType(bytes)

        then:
        format == ImageFormatType.Undefined
    }

    private byte[] jpegBytes() {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB)
        for (int x=0; x<8; x++) {
            for (int y=0; y<8; y++) {
                img.setRGB(x, y, new Color(255, 64, 64).getRGB())
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream()
        ImageIO.write(img, 'jpeg', bos)
        return bos.toByteArray()
    }

    void 'getImageFormatType returns Jpeg for JPEG bytes and thumbnail succeeds'() {
        given:
        byte[] bytes = jpegBytes()

        when:
        def format = transactionService.getImageFormatType(bytes)
        byte[] thumb = transactionService.createThumbnail(bytes, format)

        then:
        format == ImageFormatType.Jpeg
        thumb != null && thumb.length > 0
    }

    void 'createThumbnail returns non-empty thumbnail for PNG'() {
        given:
        byte[] bytes = pngBytes()

        when:
        byte[] thumb = transactionService.createThumbnail(bytes, ImageFormatType.Png)

        then:
        thumb != null
        thumb.length > 0
    }
}
