package finance.domain

import spock.lang.Specification
import spock.lang.Unroll

class TransactionTypeSpec extends Specification {

    @Unroll
    def "should have correct label for #transactionType"() {
        expect: "transaction type has expected label"
        transactionType.label == expectedLabel

        where:
        transactionType            | expectedLabel
        TransactionType.Expense    | "expense"
        TransactionType.Income     | "income"
        TransactionType.Transfer   | "transfer"
        TransactionType.Undefined  | "undefined"
    }

    @Unroll
    def "toString should return lowercase name for #transactionType"() {
        expect: "toString returns lowercase enum name"
        transactionType.toString() == expectedString

        where:
        transactionType            | expectedString
        TransactionType.Expense    | "expense"
        TransactionType.Income     | "income"
        TransactionType.Transfer   | "transfer"
        TransactionType.Undefined  | "undefined"
    }

    def "all enum values should have non-empty labels"() {
        when: "getting all transaction types"
        def allTypes = TransactionType.values() as List

        then: "every type has a non-empty label"
        allTypes.every { it.label != null && !it.label.isEmpty() }
    }

    def "all enum values should have lowercase labels"() {
        when: "getting all transaction types"
        def allTypes = TransactionType.values() as List

        then: "every type has a lowercase label"
        allTypes.every { it.label == it.label.toLowerCase() }
    }

    def "enum should have exactly 4 values"() {
        when: "getting all transaction types"
        def allTypes = TransactionType.values()

        then: "there are exactly 4 types"
        allTypes.length == 4
    }

    def "should support enum comparison"() {
        expect:
        TransactionType.Expense == TransactionType.Expense
        TransactionType.Expense != TransactionType.Income
    }

    def "toString should match label for all types"() {
        expect:
        TransactionType.Expense.toString() == TransactionType.Expense.label
        TransactionType.Income.toString() == TransactionType.Income.label
        TransactionType.Transfer.toString() == TransactionType.Transfer.label
        TransactionType.Undefined.toString() == TransactionType.Undefined.label
    }

    def "should support switch statements"() {
        when: "using transaction type in switch"
        def result
        switch(TransactionType.Expense) {
            case TransactionType.Expense:
                result = "expense transaction"
                break
            case TransactionType.Income:
                result = "income transaction"
                break
            case TransactionType.Transfer:
                result = "transfer transaction"
                break
            default:
                result = "other transaction"
        }

        then: "switch works correctly"
        result == "expense transaction"
    }

    def "should distinguish between financial transaction types"() {
        expect: "expense and income are opposites"
        TransactionType.Expense.label == "expense"
        TransactionType.Income.label == "income"
        TransactionType.Expense != TransactionType.Income
    }

    def "transfer should be a distinct type"() {
        expect: "transfer is neither expense nor income"
        TransactionType.Transfer != TransactionType.Expense
        TransactionType.Transfer != TransactionType.Income
        TransactionType.Transfer.label == "transfer"
    }

    def "all types should have companion object"() {
        expect: "companion object exists"
        TransactionType.Companion != null
    }

    def "enum values should be ordered consistently"() {
        when: "getting all values"
        def allTypes = TransactionType.values()

        then: "values are in declaration order"
        allTypes[0] == TransactionType.Expense
        allTypes[1] == TransactionType.Income
        allTypes[2] == TransactionType.Transfer
        allTypes[3] == TransactionType.Undefined
    }
}
