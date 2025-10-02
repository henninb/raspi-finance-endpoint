package finance.domain

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class MergeDescriptionsRequestSpec extends Specification {

    ObjectMapper objectMapper = new ObjectMapper()

    def "should create MergeDescriptionsRequest with valid data"() {
        when: "creating a MergeDescriptionsRequest"
        def request = new MergeDescriptionsRequest(["amazon", "amazon_prime", "amzn"], "amazon")

        then: "request is created successfully"
        request.sourceNames == ["amazon", "amazon_prime", "amzn"]
        request.targetName == "amazon"
    }

    def "should create MergeDescriptionsRequest with default empty values"() {
        when: "creating a MergeDescriptionsRequest with defaults"
        def request = new MergeDescriptionsRequest()

        then: "default values are empty"
        request.sourceNames == []
        request.targetName == ""
    }

    def "should serialize to JSON string via toString"() {
        given: "a MergeDescriptionsRequest"
        def request = new MergeDescriptionsRequest(["source1", "source2"], "target")

        when: "converting to string"
        def jsonString = request.toString()

        then: "JSON string is generated"
        jsonString.contains("sourceNames")
        jsonString.contains("targetName")
        jsonString.contains("source1")
        jsonString.contains("source2")
        jsonString.contains("target")
    }

    def "should parse from JSON correctly"() {
        given: "a JSON string"
        def json = '''
        {
            "sourceNames": ["walmart", "wal_mart"],
            "targetName": "walmart"
        }
        '''

        when: "parsing the JSON"
        def request = objectMapper.readValue(json, MergeDescriptionsRequest)

        then: "request is parsed correctly"
        request.sourceNames == ["walmart", "wal_mart"]
        request.targetName == "walmart"
    }

    def "should support data class equality"() {
        given: "two MergeDescriptionsRequest objects with same data"
        def request1 = new MergeDescriptionsRequest(["a", "b"], "target")
        def request2 = new MergeDescriptionsRequest(["a", "b"], "target")

        expect: "they are equal"
        request1 == request2
        request1.hashCode() == request2.hashCode()
    }

    def "should support data class inequality"() {
        given: "two MergeDescriptionsRequest objects with different data"
        def request1 = new MergeDescriptionsRequest(["a", "b"], "target1")
        def request2 = new MergeDescriptionsRequest(["a", "b"], "target2")

        expect: "they are not equal"
        request1 != request2
    }

    def "should handle empty sourceNames list"() {
        when: "creating a request with empty sourceNames"
        def request = new MergeDescriptionsRequest([], "target")

        then: "request is created with empty list"
        request.sourceNames == []
        request.targetName == "target"
    }

    def "should handle single source name"() {
        when: "creating a request with single source name"
        def request = new MergeDescriptionsRequest(["single"], "target")

        then: "request is created with single element list"
        request.sourceNames == ["single"]
        request.sourceNames.size() == 1
    }

    def "should handle multiple source names"() {
        when: "creating a request with multiple source names"
        def request = new MergeDescriptionsRequest(["name1", "name2", "name3", "name4", "name5"], "target")

        then: "request is created with all source names"
        request.sourceNames.size() == 5
        request.sourceNames.containsAll(["name1", "name2", "name3", "name4", "name5"])
    }

    def "should preserve sourceNames order"() {
        given: "a request with ordered sourceNames"
        def request = new MergeDescriptionsRequest(["first", "second", "third"], "target")

        expect: "order is preserved"
        request.sourceNames[0] == "first"
        request.sourceNames[1] == "second"
        request.sourceNames[2] == "third"
    }

    def "toString should produce valid JSON"() {
        given: "a MergeDescriptionsRequest"
        def request = new MergeDescriptionsRequest(["test"], "merged")

        when: "converting to string and parsing back"
        def jsonString = request.toString()
        def parsed = objectMapper.readValue(jsonString, MergeDescriptionsRequest)

        then: "parsed object matches original"
        parsed.sourceNames == request.sourceNames
        parsed.targetName == request.targetName
    }
}
