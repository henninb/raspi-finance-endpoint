package finance.domain

import spock.lang.Specification
import spock.lang.Unroll

class TransactionStateSpec extends Specification {

    @Unroll
    def "should have correct label for #transactionState"() {
        expect: "transaction state has expected label"
        transactionState.label == expectedLabel

        where:
        transactionState               | expectedLabel
        TransactionState.Cleared       | "cleared"
        TransactionState.Outstanding   | "outstanding"
        TransactionState.Future        | "future"
        TransactionState.Undefined     | "undefined"
    }

    @Unroll
    def "value() should return label for #transactionState"() {
        expect: "value returns the label"
        transactionState.value() == expectedValue

        where:
        transactionState               | expectedValue
        TransactionState.Cleared       | "cleared"
        TransactionState.Outstanding   | "outstanding"
        TransactionState.Future        | "future"
        TransactionState.Undefined     | "undefined"
    }

    @Unroll
    def "toString should return lowercase name for #transactionState"() {
        expect: "toString returns lowercase enum name"
        transactionState.toString() == expectedString

        where:
        transactionState               | expectedString
        TransactionState.Cleared       | "cleared"
        TransactionState.Outstanding   | "outstanding"
        TransactionState.Future        | "future"
        TransactionState.Undefined     | "undefined"
    }

    def "all enum values should have non-empty labels"() {
        when: "getting all transaction states"
        def allStates = TransactionState.values() as List

        then: "every state has a non-empty label"
        allStates.every { it.label != null && !it.label.isEmpty() }
    }

    def "all enum values should have lowercase labels"() {
        when: "getting all transaction states"
        def allStates = TransactionState.values() as List

        then: "every state has a lowercase label"
        allStates.every { it.label == it.label.toLowerCase() }
    }

    def "enum should have exactly 4 values"() {
        when: "getting all transaction states"
        def allStates = TransactionState.values()

        then: "there are exactly 4 states"
        allStates.length == 4
    }

    def "should support enum comparison"() {
        expect:
        TransactionState.Cleared == TransactionState.Cleared
        TransactionState.Cleared != TransactionState.Outstanding
    }

    def "getByValue should return correct state when label matches"() {
        when: "getting state by value"
        def result = TransactionState.getByValue("cleared")

        then: "correct state is returned"
        result == TransactionState.Cleared
    }

    def "getByValue should return correct state for all valid labels"() {
        expect: "each label returns correct state"
        TransactionState.getByValue(label) == expectedState

        where:
        label         | expectedState
        "cleared"     | TransactionState.Cleared
        "outstanding" | TransactionState.Outstanding
        "future"      | TransactionState.Future
        "undefined"   | TransactionState.Undefined
    }

    def "getByValue should return null when label does not match"() {
        when: "getting state by invalid value"
        def result = TransactionState.getByValue("nonexistent")

        then: "null is returned"
        result == null
    }

    def "getByValue should be case sensitive"() {
        when: "getting state with wrong case"
        def result = TransactionState.getByValue("CLEARED")

        then: "null is returned"
        result == null
    }

    def "value() and label should be identical"() {
        when: "getting all transaction states"
        def allStates = TransactionState.values() as List

        then: "value() returns the same as label"
        allStates.every { it.value() == it.label }
    }

    def "toString should match label for all states"() {
        expect:
        TransactionState.Cleared.toString() == TransactionState.Cleared.label
        TransactionState.Outstanding.toString() == TransactionState.Outstanding.label
        TransactionState.Future.toString() == TransactionState.Future.label
        TransactionState.Undefined.toString() == TransactionState.Undefined.label
    }

    def "should support switch statements"() {
        when: "using transaction state in switch"
        def result
        switch(TransactionState.Cleared) {
            case TransactionState.Cleared:
                result = "cleared transaction"
                break
            case TransactionState.Outstanding:
                result = "outstanding transaction"
                break
            default:
                result = "other transaction"
        }

        then: "switch works correctly"
        result == "cleared transaction"
    }

    def "companion object should provide getByValue functionality"() {
        when: "getting values by label"
        def cleared = TransactionState.getByValue("cleared")
        def outstanding = TransactionState.getByValue("outstanding")
        def invalid = TransactionState.getByValue("invalid")

        then: "correct values are returned"
        cleared == TransactionState.Cleared
        outstanding == TransactionState.Outstanding
        invalid == null
    }
}
