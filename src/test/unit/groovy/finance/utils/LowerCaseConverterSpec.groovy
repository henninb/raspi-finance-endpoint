package finance.utils

import spock.lang.Specification

class LowerCaseConverterSpec extends Specification {

    LowerCaseConverter lowerCaseConverter = new LowerCaseConverter()

    def "convertToDatabaseColumn - converts string to lowercase"() {
        given:
        def input = "TEST_ACCOUNT"

        when:
        def result = lowerCaseConverter.convertToDatabaseColumn(input)

        then:
        result == "test_account"
    }

    def "convertToDatabaseColumn - handles mixed case"() {
        given:
        def input = "Test_Account_Name"

        when:
        def result = lowerCaseConverter.convertToDatabaseColumn(input)

        then:
        result == "test_account_name"
    }

    def "convertToDatabaseColumn - handles already lowercase string"() {
        given:
        def input = "already_lowercase"

        when:
        def result = lowerCaseConverter.convertToDatabaseColumn(input)

        then:
        result == "already_lowercase"
    }

    def "convertToDatabaseColumn - handles null input"() {
        given:
        def input = null

        when:
        def result = lowerCaseConverter.convertToDatabaseColumn(input)

        then:
        result == ""
    }

    def "convertToDatabaseColumn - handles empty string"() {
        given:
        def input = ""

        when:
        def result = lowerCaseConverter.convertToDatabaseColumn(input)

        then:
        result == ""
    }

    def "convertToDatabaseColumn - handles string with numbers and special characters"() {
        given:
        def input = "TEST_123_ACCOUNT!"

        when:
        def result = lowerCaseConverter.convertToDatabaseColumn(input)

        then:
        result == "test_123_account!"
    }

    def "convertToEntityAttribute - converts string to lowercase"() {
        given:
        def input = "TEST_ACCOUNT"

        when:
        def result = lowerCaseConverter.convertToEntityAttribute(input)

        then:
        result == "test_account"
    }

    def "convertToEntityAttribute - handles mixed case"() {
        given:
        def input = "Test_Account_Name"

        when:
        def result = lowerCaseConverter.convertToEntityAttribute(input)

        then:
        result == "test_account_name"
    }

    def "convertToEntityAttribute - handles already lowercase string"() {
        given:
        def input = "already_lowercase"

        when:
        def result = lowerCaseConverter.convertToEntityAttribute(input)

        then:
        result == "already_lowercase"
    }

    def "convertToEntityAttribute - handles null input"() {
        given:
        def input = null

        when:
        def result = lowerCaseConverter.convertToEntityAttribute(input)

        then:
        result == ""
    }

    def "convertToEntityAttribute - handles empty string"() {
        given:
        def input = ""

        when:
        def result = lowerCaseConverter.convertToEntityAttribute(input)

        then:
        result == ""
    }

    def "convertToEntityAttribute - handles string with numbers and special characters"() {
        given:
        def input = "TEST_123_ACCOUNT!"

        when:
        def result = lowerCaseConverter.convertToEntityAttribute(input)

        then:
        result == "test_123_account!"
    }
}