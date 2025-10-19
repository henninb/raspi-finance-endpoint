package finance.utils

import spock.lang.Specification
import jakarta.validation.ConstraintValidatorContext
import java.sql.Date
import java.sql.Timestamp

class DateAndTimestampValidatorSpec extends Specification {

    def context = Mock(ConstraintValidatorContext)

    def "DateValidator accepts null and recent dates"() {
        given:
        def validator = new DateValidator()

        expect:
        validator.isValid(null, context)
        validator.isValid(Date.valueOf("2000-01-02"), context)

        and:
        !validator.isValid(Date.valueOf("1999-12-31"), context)
    }

    def "TimestampValidator currently throws due to invalid baseline format"() {
        given:
        def validator = new TimestampValidator()

        when:
        validator.isValid(Timestamp.valueOf("2001-01-02 00:00:00"), context)

        then:
        thrown(IllegalArgumentException)
    }
}
