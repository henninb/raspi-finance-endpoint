package finance.utils

import spock.lang.Specification
import jakarta.validation.ConstraintValidatorContext
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
}
