package finance.domain

import spock.lang.Specification
import spock.lang.Unroll

class ReoccurringTypeSpec extends Specification {

    @Unroll
    def "should have correct label for #reoccurringType"() {
        expect: "reoccurring type has expected label"
        reoccurringType.label == expectedLabel

        where:
        reoccurringType                | expectedLabel
        ReoccurringType.Monthly        | "monthly"
        ReoccurringType.Annually       | "annually"
        ReoccurringType.BiAnnually     | "biannually"
        ReoccurringType.FortNightly    | "fortnightly"
        ReoccurringType.Quarterly      | "quarterly"
        ReoccurringType.Onetime        | "onetime"
        ReoccurringType.Undefined      | "undefined"
    }

    @Unroll
    def "value() should return label for #reoccurringType"() {
        expect: "value returns the label"
        reoccurringType.value() == expectedValue

        where:
        reoccurringType                | expectedValue
        ReoccurringType.Monthly        | "monthly"
        ReoccurringType.Annually       | "annually"
        ReoccurringType.BiAnnually     | "biannually"
        ReoccurringType.FortNightly    | "fortnightly"
        ReoccurringType.Quarterly      | "quarterly"
        ReoccurringType.Onetime        | "onetime"
        ReoccurringType.Undefined      | "undefined"
    }

    @Unroll
    def "toString should return lowercase name for #reoccurringType"() {
        expect: "toString returns lowercase enum name"
        reoccurringType.toString() == expectedString

        where:
        reoccurringType                | expectedString
        ReoccurringType.Monthly        | "monthly"
        ReoccurringType.Annually       | "annually"
        ReoccurringType.BiAnnually     | "biannually"
        ReoccurringType.FortNightly    | "fortnightly"
        ReoccurringType.Quarterly      | "quarterly"
        ReoccurringType.Onetime        | "onetime"
        ReoccurringType.Undefined      | "undefined"
    }

    def "all enum values should have non-empty labels"() {
        when: "getting all reoccurring types"
        def allTypes = ReoccurringType.values() as List

        then: "every type has a non-empty label"
        allTypes.every { it.label != null && !it.label.isEmpty() }
    }

    def "all enum values should have lowercase labels"() {
        when: "getting all reoccurring types"
        def allTypes = ReoccurringType.values() as List

        then: "every type has a lowercase label"
        allTypes.every { it.label == it.label.toLowerCase() }
    }

    def "enum should have exactly 7 values"() {
        when: "getting all reoccurring types"
        def allTypes = ReoccurringType.values()

        then: "there are exactly 7 types"
        allTypes.length == 7
    }

    def "should support enum comparison"() {
        expect:
        ReoccurringType.Monthly == ReoccurringType.Monthly
        ReoccurringType.Monthly != ReoccurringType.Annually
    }

    def "should support switch statements"() {
        when: "using reoccurring type in switch"
        def result
        switch(ReoccurringType.Monthly) {
            case ReoccurringType.Monthly:
                result = "monthly payment"
                break
            case ReoccurringType.Annually:
                result = "annual payment"
                break
            default:
                result = "other payment"
        }

        then: "switch works correctly"
        result == "monthly payment"
    }

    def "value() and label should be identical"() {
        when: "getting all reoccurring types"
        def allTypes = ReoccurringType.values() as List

        then: "value() returns the same as label"
        allTypes.every { it.value() == it.label }
    }

    def "toString should match label for common types"() {
        expect:
        ReoccurringType.Monthly.toString() == ReoccurringType.Monthly.label
        ReoccurringType.Quarterly.toString() == ReoccurringType.Quarterly.label
        ReoccurringType.Annually.toString() == ReoccurringType.Annually.label
    }
}
