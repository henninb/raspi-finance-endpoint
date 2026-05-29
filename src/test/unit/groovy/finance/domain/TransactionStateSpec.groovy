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

    def "fromString should return correct state for all valid labels"() {
        expect: "each label returns correct state"
        TransactionState.fromString(label) == expectedState

        where:
        label         | expectedState
        "cleared"     | TransactionState.Cleared
        "outstanding" | TransactionState.Outstanding
        "future"      | TransactionState.Future
        "undefined"   | TransactionState.Undefined
    }

    def "fromString should be case insensitive"() {
        expect:
        TransactionState.fromString("CLEARED") == TransactionState.Cleared
        TransactionState.fromString("Outstanding") == TransactionState.Outstanding
    }

    def "fromString should throw for invalid label"() {
        when:
        TransactionState.fromString("nonexistent")

        then:
        thrown(IllegalArgumentException)
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
        switch (TransactionState.Cleared) {
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

}
