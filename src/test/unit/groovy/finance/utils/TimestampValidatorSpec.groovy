package finance.utils

import spock.lang.Specification
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.constraints.PastOrPresent
import java.lang.annotation.Annotation
import java.sql.Timestamp

class TimestampValidatorSpec extends Specification {
    def validator = new TimestampValidator()
    def context = Mock(ConstraintValidatorContext)

    def "isValid throws due to invalid cutoff constant format"() {
        when:
        validator.isValid(new Timestamp(System.currentTimeMillis()), context)

        then:
        thrown(IllegalArgumentException)
    }

    def "initialize does not throw with annotation"() {
        given:
        def annotation = [
            message: { -> "timestamp must be greater than 1/1/2000." },
            groups: { -> [] as Class[] },
            payload: { -> [] as Class[] },
            annotationType: { -> ValidTimestamp }
        ] as ValidTimestamp

        when:
        validator.initialize(annotation)

        then:
        noExceptionThrown()
    }
}
