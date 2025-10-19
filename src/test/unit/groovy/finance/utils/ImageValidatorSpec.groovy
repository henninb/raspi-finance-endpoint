package finance.utils

import spock.lang.Specification
import jakarta.validation.ConstraintValidatorContext

class ImageValidatorSpec extends Specification {

    def context = Mock(ConstraintValidatorContext)

    def "Empty byte array is valid"() {
        given:
        def validator = new ImageValidator()

        expect:
        validator.isValid(new byte[0], context)
    }

    def "JPEG image bytes are valid"() {
        given:
        def validator = new ImageValidator()
        byte[] bytes = this.class.getResourceAsStream('/viking-icon.jpg').bytes

        expect:
        validator.isValid(bytes, context)
    }

    def "Random non-image bytes are invalid"() {
        given:
        def validator = new ImageValidator()
        byte[] bytes = 'not an image'.getBytes('UTF-8')

        expect:
        !validator.isValid(bytes, context)
    }
}

