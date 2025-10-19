package finance.utils

import finance.domain.AccountType
import finance.domain.TransactionType
import spock.lang.Specification

class EnumConvertersSpec extends Specification {

    def "AccountTypeConverter maps enums to labels and back"() {
        given:
        def converter = new AccountTypeConverter()

        expect:
        converter.convertToDatabaseColumn(AccountType.Credit) == 'credit'
        converter.convertToEntityAttribute('credit') == AccountType.Credit
        converter.convertToEntityAttribute('HSA') == AccountType.HSA
        converter.convertToEntityAttribute('  business_credit  ') == AccountType.BusinessCredit
    }

    def "AccountTypeConverter throws on unknown attribute"() {
        given:
        def converter = new AccountTypeConverter()

        when:
        converter.convertToEntityAttribute('not-a-real-type')

        then:
        thrown(RuntimeException)
    }

    def "TransactionTypeConverter maps enums to labels and back"() {
        given:
        def converter = new TransactionTypeConverter()

        expect:
        converter.convertToDatabaseColumn(TransactionType.Income) == 'income'
        converter.convertToEntityAttribute('transfer') == TransactionType.Transfer
    }

    def "TransactionTypeConverter throws on unknown attribute"() {
        given:
        def converter = new TransactionTypeConverter()

        when:
        converter.convertToEntityAttribute('mystery')

        then:
        thrown(RuntimeException)
    }
}

