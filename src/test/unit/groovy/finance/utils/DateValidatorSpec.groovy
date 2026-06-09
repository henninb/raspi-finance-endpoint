package finance.utils

import spock.lang.Specification
import java.time.LocalDate
import jakarta.validation.ConstraintValidatorContext

class DateValidatorSpec extends Specification {

    DateValidator dateValidator = new DateValidator()
    ConstraintValidatorContext contextMock = Mock(ConstraintValidatorContext)

    def setup() {
        dateValidator.initialize(Mock(ValidDate))
    }

    def "isValid - returns true for valid date after 2000-01-01"() {
        given:
        def validDate = LocalDate.parse("2023-01-01")

        when:
        def result = dateValidator.isValid(validDate, contextMock)

        then:
        result == true
    }

    def "isValid - returns true for date exactly after 2000-01-01"() {
        given:
        def validDate = LocalDate.parse("2000-01-02")

        when:
        def result = dateValidator.isValid(validDate, contextMock)

        then:
        result == true
    }

    def "isValid - returns false for date on 2000-01-01"() {
        given:
        def invalidDate = LocalDate.parse("2000-01-01")

        when:
        def result = dateValidator.isValid(invalidDate, contextMock)

        then:
        result == false
    }

    def "isValid - returns false for date before 2000-01-01"() {
        given:
        def invalidDate = LocalDate.parse("1999-12-31")

        when:
        def result = dateValidator.isValid(invalidDate, contextMock)

        then:
        result == false
    }

    def "isValid - returns false for very old dates"() {
        given:
        def invalidDate = LocalDate.parse("1900-01-01")

        when:
        def result = dateValidator.isValid(invalidDate, contextMock)

        then:
        result == false
    }

    def "isValid - returns true for current date"() {
        given:
        def currentDate = LocalDate.now()

        when:
        def result = dateValidator.isValid(currentDate, contextMock)

        then:
        result == true
    }

    def "isValid - returns true for future dates within 50-year window"() {
        given:
        def futureDate = LocalDate.parse("2030-12-31")

        when:
        def result = dateValidator.isValid(futureDate, contextMock)

        then:
        result == true
    }

    // #9: upper-bound tests — dates beyond 50 years from now must be rejected
    def "isValid - returns false for date more than 50 years in the future"() {
        given:
        def farFuture = LocalDate.now().plusYears(51)

        when:
        def result = dateValidator.isValid(farFuture, contextMock)

        then:
        result == false
    }

    def "isValid - returns false for date exactly 50 years from now"() {
        given:
        // plusYears(50) is NOT before itself, so isBefore(maxDate) == false
        def boundaryDate = LocalDate.now().plusYears(50)

        when:
        def result = dateValidator.isValid(boundaryDate, contextMock)

        then:
        result == false
    }

    def "isValid - returns true for date one day before the 50-year boundary"() {
        given:
        def nearBoundary = LocalDate.now().plusYears(50).minusDays(1)

        when:
        def result = dateValidator.isValid(nearBoundary, contextMock)

        then:
        result == true
    }
}
