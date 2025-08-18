package finance.utils

import finance.domain.ReoccurringType
import spock.lang.Specification

class ReoccurringTypeConverterSpec extends Specification {

    ReoccurringTypeConverter converter = new ReoccurringTypeConverter()

    def "convertToDatabaseColumn converts ReoccurringType enum to string"() {
        expect:
        converter.convertToDatabaseColumn(reoccurringType) == expectedString

        where:
        reoccurringType               | expectedString
        ReoccurringType.Annually      | "annually"
        ReoccurringType.BiAnnually    | "biannually"
        ReoccurringType.FortNightly   | "fortnightly"
        ReoccurringType.Quarterly     | "quarterly"
        ReoccurringType.Monthly       | "monthly"
        ReoccurringType.Onetime       | "onetime"
        ReoccurringType.Undefined     | "undefined"
    }

    def "convertToEntityAttribute converts string to ReoccurringType enum"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedReoccurringType

        where:
        inputString     | expectedReoccurringType
        "annually"      | ReoccurringType.Annually
        "biannually"    | ReoccurringType.BiAnnually
        "fortnightly"   | ReoccurringType.FortNightly
        "quarterly"     | ReoccurringType.Quarterly
        "monthly"       | ReoccurringType.Monthly
        "onetime"       | ReoccurringType.Onetime
        "undefined"     | ReoccurringType.Undefined
    }

    def "convertToEntityAttribute handles case insensitive input"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedReoccurringType

        where:
        inputString     | expectedReoccurringType
        "ANNUALLY"      | ReoccurringType.Annually
        "Annually"      | ReoccurringType.Annually
        "BIANNUALLY"    | ReoccurringType.BiAnnually
        "BiAnnually"    | ReoccurringType.BiAnnually
        "FORTNIGHTLY"   | ReoccurringType.FortNightly
        "Fortnightly"   | ReoccurringType.FortNightly
        "QUARTERLY"     | ReoccurringType.Quarterly
        "Quarterly"     | ReoccurringType.Quarterly
        "MONTHLY"       | ReoccurringType.Monthly
        "Monthly"       | ReoccurringType.Monthly
        "ONETIME"       | ReoccurringType.Onetime
        "Onetime"       | ReoccurringType.Onetime
        "UNDEFINED"     | ReoccurringType.Undefined
        "Undefined"     | ReoccurringType.Undefined
    }

    def "convertToEntityAttribute handles whitespace"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedReoccurringType

        where:
        inputString         | expectedReoccurringType
        " annually "        | ReoccurringType.Annually
        "  monthly  "       | ReoccurringType.Monthly
        "\tquarterly\t"     | ReoccurringType.Quarterly
        "\nfortnightly\n"   | ReoccurringType.FortNightly
        " biannually "      | ReoccurringType.BiAnnually
        "  onetime  "       | ReoccurringType.Onetime
        "\tundefined\t"     | ReoccurringType.Undefined
    }

    def "convertToEntityAttribute throws exception for unknown attribute"() {
        when:
        converter.convertToEntityAttribute(invalidInput)

        then:
        RuntimeException ex = thrown()
        ex.message == "Unknown attribute: ${invalidInput}"

        where:
        invalidInput << [
            "invalid", "weekly", "daily", "yearly", "biweekly", 
            "", " ", "null", "never", "once", "recurring"
        ]
    }

    def "convertToEntityAttribute handles mixed case and whitespace with unknown values"() {
        when:
        converter.convertToEntityAttribute("  Weekly  ")

        then:
        RuntimeException ex = thrown()
        ex.message == "Unknown attribute:   Weekly  "
    }

    def "all ReoccurringType enum values are handled in convertToDatabaseColumn"() {
        expect: "all enum values can be converted to database column"
        ReoccurringType.values().every { reoccurringType ->
            String result = converter.convertToDatabaseColumn(reoccurringType)
            result != null && !result.isEmpty()
        }
    }

    def "round trip conversion works correctly"() {
        expect: "converting to database and back returns original value"
        ReoccurringType.values().every { original ->
            String dbValue = converter.convertToDatabaseColumn(original)
            ReoccurringType roundTrip = converter.convertToEntityAttribute(dbValue)
            roundTrip == original
        }
    }
}