package finance.utils

import finance.domain.AccountType
import spock.lang.Specification

class AccountTypeConverterSpec extends Specification {

    AccountTypeConverter converter = new AccountTypeConverter()

    def "convertToDatabaseColumn converts AccountType enum to string"() {
        expect:
        converter.convertToDatabaseColumn(accountType) == expectedString

        where:
        accountType                    | expectedString
        AccountType.Credit             | 'credit'
        AccountType.Debit              | 'debit'
        AccountType.Undefined          | 'undefined'
        AccountType.Checking           | 'checking'
        AccountType.Savings            | 'savings'
        AccountType.CreditCard         | 'credit_card'
        AccountType.HSA                | 'hsa'
        AccountType.FSA                | 'fsa'
        AccountType.MedicalSavings     | 'medical_savings'
        AccountType.Brokerage          | 'brokerage'
        AccountType.Mortgage           | 'mortgage'
        AccountType.BusinessChecking   | 'business_checking'
        AccountType.Cash               | 'cash'
    }

    void "convertToEntityAttribute converts string to AccountType enum"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedAccountType

        where:
        inputString          | expectedAccountType
        'credit'             | AccountType.Credit
        'debit'              | AccountType.Debit
        'undefined'          | AccountType.Undefined
        'checking'           | AccountType.Checking
        'savings'            | AccountType.Savings
        'credit_card'        | AccountType.CreditCard
        'hsa'                | AccountType.HSA
        'fsa'                | AccountType.FSA
        'medical_savings'    | AccountType.MedicalSavings
        'brokerage'          | AccountType.Brokerage
        'mortgage'           | AccountType.Mortgage
        'business_checking'  | AccountType.BusinessChecking
        'cash'               | AccountType.Cash
    }

    void "convertToEntityAttribute handles case insensitive input"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedAccountType

        where:
        inputString          | expectedAccountType
        'CREDIT'             | AccountType.Credit
        'Credit'             | AccountType.Credit
        'DEBIT'              | AccountType.Debit
        'Debit'              | AccountType.Debit
        'UNDEFINED'          | AccountType.Undefined
        'Undefined'          | AccountType.Undefined
        'CHECKING'           | AccountType.Checking
        'Checking'           | AccountType.Checking
        'SAVINGS'            | AccountType.Savings
        'Savings'            | AccountType.Savings
        'HSA'                | AccountType.HSA
        'Hsa'                | AccountType.HSA
        'MEDICAL_SAVINGS'    | AccountType.MedicalSavings
        'Medical_Savings'    | AccountType.MedicalSavings
    }

    void "convertToEntityAttribute handles whitespace"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedAccountType

        where:
        inputString             | expectedAccountType
        ' credit '              | AccountType.Credit
        '  debit  '             | AccountType.Debit
        "\tundefined\t"         | AccountType.Undefined
        "\ncredit\n"            | AccountType.Credit
        ' checking '            | AccountType.Checking
        '  hsa  '               | AccountType.HSA
        "\tmedical_savings\t"   | AccountType.MedicalSavings
    }

    def "convertToEntityAttribute throws exception for unknown attribute"() {
        when:
        converter.convertToEntityAttribute(invalidInput)

        then:
        def ex = thrown(RuntimeException)
        ex.message == "Unknown account type attribute: ${invalidInput}"

        where:
        invalidInput << ['invalid_type', 'not_valid', 'xyz', '', ' ', 'null', 'unknown_account']
    }

    def "convertToEntityAttribute handles mixed case and whitespace with unknown values"() {
        when:
        converter.convertToEntityAttribute('  Invalid_Type  ')

        then:
        RuntimeException ex = thrown()
        ex.message == "Unknown account type attribute:   Invalid_Type  "
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
