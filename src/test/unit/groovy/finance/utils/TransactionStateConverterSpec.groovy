package finance.utils

import finance.domain.TransactionState
import spock.lang.Specification

class TransactionStateConverterSpec extends Specification {

    TransactionStateConverter converter = new TransactionStateConverter()

    def "convertToDatabaseColumn converts TransactionState enum to string"() {
        expect:
        converter.convertToDatabaseColumn(transactionState) == expectedString

        where:
        transactionState               | expectedString
        TransactionState.Outstanding   | "outstanding"
        TransactionState.Future        | "future"
        TransactionState.Cleared       | "cleared"
        TransactionState.Undefined     | "undefined"
    }

    def "convertToEntityAttribute converts string to TransactionState enum"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedTransactionState

        where:
        inputString     | expectedTransactionState
        "outstanding"   | TransactionState.Outstanding
        "future"        | TransactionState.Future
        "cleared"       | TransactionState.Cleared
        "undefined"     | TransactionState.Undefined
    }

    def "convertToEntityAttribute handles case insensitive input"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedTransactionState

        where:
        inputString     | expectedTransactionState
        "OUTSTANDING"   | TransactionState.Outstanding
        "Outstanding"   | TransactionState.Outstanding
        "FUTURE"        | TransactionState.Future
        "Future"        | TransactionState.Future
        "CLEARED"       | TransactionState.Cleared
        "Cleared"       | TransactionState.Cleared
        "UNDEFINED"     | TransactionState.Undefined
        "Undefined"     | TransactionState.Undefined
    }

    def "convertToEntityAttribute handles whitespace"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedTransactionState

        where:
        inputString         | expectedTransactionState
        " outstanding "     | TransactionState.Outstanding
        "  future  "        | TransactionState.Future
        "\tcleared\t"       | TransactionState.Cleared
        "\nundefined\n"     | TransactionState.Undefined
    }

    def "convertToEntityAttribute throws exception for unknown attribute"() {
        when:
        converter.convertToEntityAttribute(invalidInput)

        then:
        RuntimeException ex = thrown()
        ex.message == "Unknown attribute: ${invalidInput}"

        where:
        invalidInput << ["invalid", "pending", "completed", "processing", "", " ", "null", "paid", "unpaid"]
    }

    def "convertToEntityAttribute handles mixed case and whitespace with unknown values"() {
        when:
        converter.convertToEntityAttribute("  Pending  ")

        then:
        RuntimeException ex = thrown()
        ex.message == "Unknown attribute:   Pending  "
    }

    def "all TransactionState enum values are handled in convertToDatabaseColumn"() {
        expect: "all enum values can be converted to database column"
        TransactionState.values().every { transactionState ->
            String result = converter.convertToDatabaseColumn(transactionState)
            result != null && !result.isEmpty()
        }
    }

    def "round trip conversion works correctly"() {
        expect: "converting to database and back returns original value"
        TransactionState.values().every { original ->
            String dbValue = converter.convertToDatabaseColumn(original)
            TransactionState roundTrip = converter.convertToEntityAttribute(dbValue)
            roundTrip == original
        }
    }
}