package finance.utils

import spock.lang.Specification
import java.sql.Date
import jakarta.validation.ConstraintValidatorContext

class DateValidatorSpec extends Specification {

    DateValidator dateValidator = new DateValidator()
    ConstraintValidatorContext contextMock = Mock(ConstraintValidatorContext)

    def setup() {
        dateValidator.initialize(Mock(ValidDate))
    }

    def "isValid - returns true for valid date after 2000-01-01"() {
        given:
        def validDate = Date.valueOf("2023-01-01")

        when:
        def result = dateValidator.isValid(validDate, contextMock)

        then:
        result == true
    }

    def "isValid - returns true for date exactly after 2000-01-01"() {
        given:
        def validDate = Date.valueOf("2000-01-02")

        when:
        def result = dateValidator.isValid(validDate, contextMock)

        then:
        result == true
    }

    def "isValid - returns false for date on 2000-01-01"() {
        given:
        def invalidDate = Date.valueOf("2000-01-01")

        when:
        def result = dateValidator.isValid(invalidDate, contextMock)

        then:
        result == false
    }

    def "isValid - returns false for date before 2000-01-01"() {
        given:
        def invalidDate = Date.valueOf("1999-12-31")

        when:
        def result = dateValidator.isValid(invalidDate, contextMock)

        then:
        result == false
    }

    def "isValid - returns false for very old dates"() {
        given:
        def invalidDate = Date.valueOf("1900-01-01")

        when:
        def result = dateValidator.isValid(invalidDate, contextMock)

        then:
        result == false
    }

    def "isValid - returns true for current date"() {
        given:
        def currentDate = new Date(System.currentTimeMillis())

        when:
        def result = dateValidator.isValid(currentDate, contextMock)

        then:
        result == true
    }

    def "isValid - returns true for future dates"() {
        given:
        def futureDate = Date.valueOf("2030-12-31")

        when:
        def result = dateValidator.isValid(futureDate, contextMock)

        then:
        result == true
    }
}