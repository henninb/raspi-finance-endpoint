package finance.domain

import spock.lang.Specification
import spock.lang.Unroll

class ClaimStatusSpec extends Specification {

    @Unroll
    def "should create ClaimStatus from string value: #inputValue"() {
        when: "creating ClaimStatus from string"
        ClaimStatus result = ClaimStatus.fromString(inputValue)

        then: "should return correct enum value"
        result == expectedStatus
        result.label == expectedLabel

        where:
        inputValue    | expectedStatus      | expectedLabel
        "submitted"   | ClaimStatus.Submitted | "submitted"
        "processing"  | ClaimStatus.Processing | "processing"
        "approved"    | ClaimStatus.Approved   | "approved"
        "denied"      | ClaimStatus.Denied     | "denied"
        "paid"        | ClaimStatus.Paid       | "paid"
        "closed"      | ClaimStatus.Closed     | "closed"
    }

    @Unroll
    def "should create ClaimStatus from case-insensitive string: #inputValue"() {
        when: "creating ClaimStatus from case-insensitive string"
        ClaimStatus result = ClaimStatus.fromString(inputValue)

        then: "should return correct enum value"
        result == expectedStatus

        where:
        inputValue    | expectedStatus
        "SUBMITTED"   | ClaimStatus.Submitted
        "Processing"  | ClaimStatus.Processing
        "APPROVED"    | ClaimStatus.Approved
        "Denied"      | ClaimStatus.Denied
        "PAID"        | ClaimStatus.Paid
        "Closed"      | ClaimStatus.Closed
    }

    def "should throw exception for invalid string value"() {
        when: "creating ClaimStatus from invalid string"
        ClaimStatus.fromString("invalid_status")

        then: "should throw IllegalArgumentException"
        IllegalArgumentException ex = thrown()
        ex.message == "Unknown claim status: invalid_status"
    }

    def "should throw exception for null string value"() {
        when: "creating ClaimStatus from null string"
        ClaimStatus.fromString(null)

        then: "should throw IllegalArgumentException"
        thrown(IllegalArgumentException)
    }

    def "should throw exception for empty string value"() {
        when: "creating ClaimStatus from empty string"
        ClaimStatus.fromString("")

        then: "should throw IllegalArgumentException"
        thrown(IllegalArgumentException)
    }

    def "should return all valid statuses"() {
        when: "getting valid statuses"
        List<String> validStatuses = ClaimStatus.getValidStatuses()

        then: "should return all enum labels"
        validStatuses.size() == 6
        validStatuses.containsAll([
            "submitted", "processing", "approved", "denied", "paid", "closed"
        ])
    }

    @Unroll
    def "should return correct label for enum: #status"() {
        expect: "label to match expected value"
        status.label == expectedLabel
        status.toValue() == expectedLabel
        status.toString() == expectedLabel

        where:
        status                  | expectedLabel
        ClaimStatus.Submitted   | "submitted"
        ClaimStatus.Processing  | "processing"
        ClaimStatus.Approved    | "approved"
        ClaimStatus.Denied      | "denied"
        ClaimStatus.Paid        | "paid"
        ClaimStatus.Closed      | "closed"
    }

    def "should have consistent enum values count"() {
        when: "getting all enum values"
        ClaimStatus[] allValues = ClaimStatus.values()

        then: "should have expected number of values"
        allValues.length == 6
        allValues.contains(ClaimStatus.Submitted)
        allValues.contains(ClaimStatus.Processing)
        allValues.contains(ClaimStatus.Approved)
        allValues.contains(ClaimStatus.Denied)
        allValues.contains(ClaimStatus.Paid)
        allValues.contains(ClaimStatus.Closed)
    }

    def "should support JSON serialization via @JsonValue"() {
        given: "a claim status"
        ClaimStatus status = ClaimStatus.Approved

        when: "getting JSON value"
        String jsonValue = status.toValue()

        then: "should return label for JSON serialization"
        jsonValue == "approved"
    }
}