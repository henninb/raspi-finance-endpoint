package finance.domain

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class ClaimStatusJsonSpec extends Specification {

    ObjectMapper objectMapper = new ObjectMapper()

    def "should serialize ClaimStatus to JSON correctly"() {
        given: "a ClaimStatus enum"
        ClaimStatus status = ClaimStatus.Submitted

        when: "serializing to JSON"
        String json = objectMapper.writeValueAsString(status)

        then: "should produce correct JSON value"
        json == '"submitted"'
    }

    def "should deserialize ClaimStatus from JSON correctly"() {
        given: "a JSON string"
        String json = '"submitted"'

        when: "deserializing from JSON"
        ClaimStatus status = objectMapper.readValue(json, ClaimStatus.class)

        then: "should produce correct enum value"
        status == ClaimStatus.Submitted
        status.label == "submitted"
    }

    def "should handle all ClaimStatus values"() {
        expect: "ClaimStatus enum works correctly"
        ClaimStatus.Submitted.label == "submitted"
        ClaimStatus.Processing.label == "processing"
        ClaimStatus.Approved.label == "approved"
        ClaimStatus.Denied.label == "denied"
        ClaimStatus.Paid.label == "paid"
        ClaimStatus.Closed.label == "closed"
        
        and: "fromString method works"
        ClaimStatus.fromString("submitted") == ClaimStatus.Submitted
        ClaimStatus.fromString("processing") == ClaimStatus.Processing
        ClaimStatus.fromString("approved") == ClaimStatus.Approved
        ClaimStatus.fromString("denied") == ClaimStatus.Denied
        ClaimStatus.fromString("paid") == ClaimStatus.Paid
        ClaimStatus.fromString("closed") == ClaimStatus.Closed
    }
}