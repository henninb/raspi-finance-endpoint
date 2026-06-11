package finance.utils

import spock.lang.Specification

class LowerCaseConverterSpec extends Specification {

    def "Converts strings to lowercase for DB and entity"() {
        given:
        def converter = new LowerCaseConverter()

        expect:
        converter.convertToDatabaseColumn("FooBAR") == "foobar"
        converter.convertToEntityAttribute("BaZ") == "baz"
    }

    def "Null throws IllegalArgumentException in both directions"() {
        given:
        def converter = new LowerCaseConverter()

        when:
        converter.convertToDatabaseColumn(null)

        then:
        thrown(IllegalArgumentException)

        when:
        converter.convertToEntityAttribute(null)

        then:
        thrown(IllegalArgumentException)
    }
}

