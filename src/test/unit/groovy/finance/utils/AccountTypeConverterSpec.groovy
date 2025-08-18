package finance.utils

import finance.domain.AccountType
import spock.lang.Specification

class AccountTypeConverterSpec extends Specification {

    AccountTypeConverter converter = new AccountTypeConverter()

    def "convertToDatabaseColumn converts AccountType enum to string"() {
        expect:
        converter.convertToDatabaseColumn(accountType) == expectedString

        where:
        accountType              | expectedString
        AccountType.Credit       | "credit"
        AccountType.Debit        | "debit"
        AccountType.Undefined    | "undefined"
    }

    def "convertToEntityAttribute converts string to AccountType enum"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedAccountType

        where:
        inputString    | expectedAccountType
        "credit"       | AccountType.Credit
        "debit"        | AccountType.Debit
        "undefined"    | AccountType.Undefined
    }

    def "convertToEntityAttribute handles case insensitive input"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedAccountType

        where:
        inputString    | expectedAccountType
        "CREDIT"       | AccountType.Credit
        "Credit"       | AccountType.Credit
        "DEBIT"        | AccountType.Debit
        "Debit"        | AccountType.Debit
        "UNDEFINED"    | AccountType.Undefined
        "Undefined"    | AccountType.Undefined
    }

    def "convertToEntityAttribute handles whitespace"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedAccountType

        where:
        inputString      | expectedAccountType
        " credit "       | AccountType.Credit
        "  debit  "      | AccountType.Debit
        "\tundefined\t"  | AccountType.Undefined
        "\ncredit\n"     | AccountType.Credit
    }

    def "convertToEntityAttribute throws exception for unknown attribute"() {
        when:
        converter.convertToEntityAttribute(invalidInput)

        then:
        RuntimeException ex = thrown()
        ex.message == "Unknown attribute: ${invalidInput}"

        where:
        invalidInput << ["invalid", "savings", "checking", "", " ", "null", "unknown"]
    }

    def "convertToEntityAttribute handles mixed case and whitespace with unknown values"() {
        when:
        converter.convertToEntityAttribute("  Invalid  ")

        then:
        RuntimeException ex = thrown()
        ex.message == "Unknown attribute:   Invalid  "
    }

    def "all AccountType enum values are handled in convertToDatabaseColumn"() {
        expect: "all enum values can be converted to database column"
        AccountType.values().every { accountType ->
            String result = converter.convertToDatabaseColumn(accountType)
            result != null && !result.isEmpty()
        }
    }

    def "round trip conversion works correctly"() {
        expect: "converting to database and back returns original value"
        AccountType.values().every { original ->
            String dbValue = converter.convertToDatabaseColumn(original)
            AccountType roundTrip = converter.convertToEntityAttribute(dbValue)
            roundTrip == original
        }
    }
}