package finance.utils

import finance.domain.TransactionType
import spock.lang.Specification

class TransactionTypeConverterSpec extends Specification {

    TransactionTypeConverter converter = new TransactionTypeConverter()

    def "convertToDatabaseColumn converts TransactionType enum to string"() {
        expect:
        converter.convertToDatabaseColumn(transactionType) == expectedString

        where:
        transactionType              | expectedString
        TransactionType.Expense      | "expense"
        TransactionType.Income       | "income"
        TransactionType.Transfer     | "transfer"
        TransactionType.Undefined    | "undefined"
    }

    def "convertToEntityAttribute converts string to TransactionType enum"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedTransactionType

        where:
        inputString    | expectedTransactionType
        "expense"      | TransactionType.Expense
        "income"       | TransactionType.Income
        "transfer"     | TransactionType.Transfer
        "undefined"    | TransactionType.Undefined
    }

    def "convertToEntityAttribute handles case insensitive input"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedTransactionType

        where:
        inputString    | expectedTransactionType
        "EXPENSE"      | TransactionType.Expense
        "Expense"      | TransactionType.Expense
        "INCOME"       | TransactionType.Income
        "Income"       | TransactionType.Income
        "TRANSFER"     | TransactionType.Transfer
        "Transfer"     | TransactionType.Transfer
        "UNDEFINED"    | TransactionType.Undefined
        "Undefined"    | TransactionType.Undefined
    }

    def "convertToEntityAttribute handles whitespace"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedTransactionType

        where:
        inputString        | expectedTransactionType
        " expense "        | TransactionType.Expense
        "  income  "       | TransactionType.Income
        "\ttransfer\t"     | TransactionType.Transfer
        "\nundefined\n"    | TransactionType.Undefined
    }

    def "convertToEntityAttribute throws exception for unknown attribute"() {
        when:
        converter.convertToEntityAttribute(invalidInput)

        then:
        RuntimeException ex = thrown()
        ex.message == "Unknown attribute: ${invalidInput}"

        where:
        invalidInput << ["invalid", "debit", "credit", "", " ", "null", "payment", "withdrawal"]
    }

    def "convertToEntityAttribute handles mixed case and whitespace with unknown values"() {
        when:
        converter.convertToEntityAttribute("  Payment  ")

        then:
        RuntimeException ex = thrown()
        ex.message == "Unknown attribute:   Payment  "
    }

    def "all TransactionType enum values are handled in convertToDatabaseColumn"() {
        expect: "all enum values can be converted to database column"
        TransactionType.values().every { transactionType ->
            String result = converter.convertToDatabaseColumn(transactionType)
            result != null && !result.isEmpty()
        }
    }

    def "round trip conversion works correctly"() {
        expect: "converting to database and back returns original value"
        TransactionType.values().every { original ->
            String dbValue = converter.convertToDatabaseColumn(original)
            TransactionType roundTrip = converter.convertToEntityAttribute(dbValue)
            roundTrip == original
        }
    }
}