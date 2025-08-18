package finance.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class UuidControllerSpec extends Specification {

    @Subject
    UuidController uuidController = new UuidController()

    def "generateUuid should return valid UUID response"() {
        when:
        ResponseEntity<Map<String, Object>> response = uuidController.generateUuid()

        then:
        response.statusCode == HttpStatus.OK
        response.body.containsKey("uuid")
        response.body.containsKey("timestamp")
        response.body.containsKey("source")
        response.body["source"] == "server"

        and: "UUID should be valid format"
        String uuid = response.body["uuid"] as String
        uuid ==~ /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/

        and: "timestamp should be reasonable"
        Long timestamp = response.body["timestamp"] as Long
        timestamp > 0
        timestamp <= System.currentTimeMillis()
    }

    def "generateBatchUuids should return valid response for valid count"() {
        given:
        int count = 5

        when:
        ResponseEntity<Map<String, Object>> response = uuidController.generateBatchUuids(count)

        then:
        response.statusCode == HttpStatus.OK
        response.body.containsKey("uuids")
        response.body.containsKey("count")
        response.body.containsKey("timestamp")
        response.body.containsKey("source")
        response.body["source"] == "server"
        response.body["count"] == count

        and: "should have correct number of UUIDs"
        List<String> uuids = response.body["uuids"] as List<String>
        uuids.size() == count

        and: "all UUIDs should be valid format"
        uuids.every { uuid ->
            uuid ==~ /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/
        }

        and: "all UUIDs should be unique"
        uuids.unique().size() == count
    }

    def "generateBatchUuids should return valid response for default count"() {
        when:
        ResponseEntity<Map<String, Object>> response = uuidController.generateBatchUuids(1)

        then:
        response.statusCode == HttpStatus.OK
        response.body["count"] == 1

        and: "should have one UUID"
        List<String> uuids = response.body["uuids"] as List<String>
        uuids.size() == 1
    }

    def "generateBatchUuids should return bad request for invalid count"() {
        when:
        ResponseEntity<Map<String, Object>> response = uuidController.generateBatchUuids(count)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.containsKey("error")
        response.body["error"] == "Count must be between 1 and 100"

        where:
        count << [0, -1, 101, 1000]
    }

    def "generateBatchUuids should handle maximum allowed count"() {
        given:
        int maxCount = 100

        when:
        ResponseEntity<Map<String, Object>> response = uuidController.generateBatchUuids(maxCount)

        then:
        response.statusCode == HttpStatus.OK
        response.body["count"] == maxCount

        and: "should have correct number of UUIDs"
        List<String> uuids = response.body["uuids"] as List<String>
        uuids.size() == maxCount

        and: "all UUIDs should be unique"
        uuids.unique().size() == maxCount
    }

    def "healthCheck should return healthy status"() {
        when:
        ResponseEntity<Map<String, Object>> response = uuidController.healthCheck()

        then:
        response.statusCode == HttpStatus.OK
        response.body.containsKey("status")
        response.body.containsKey("service")
        response.body.containsKey("timestamp")
        response.body["status"] == "healthy"
        response.body["service"] == "uuid-generation"

        and: "timestamp should be reasonable"
        Long timestamp = response.body["timestamp"] as Long
        timestamp > 0
        timestamp <= System.currentTimeMillis()
    }

    def "generated UUIDs should be unique across multiple calls"() {
        given:
        Set<String> allUuids = new HashSet<>()

        when: "generate multiple UUIDs"
        10.times {
            ResponseEntity<Map<String, Object>> response = uuidController.generateUuid()
            allUuids.add(response.body["uuid"] as String)
        }

        then: "all UUIDs should be unique"
        allUuids.size() == 10
    }

    def "batch generated UUIDs should be unique across multiple calls"() {
        given:
        Set<String> allUuids = new HashSet<>()

        when: "generate multiple batches"
        5.times {
            ResponseEntity<Map<String, Object>> response = uuidController.generateBatchUuids(3)
            List<String> uuids = response.body["uuids"] as List<String>
            allUuids.addAll(uuids)
        }

        then: "all UUIDs should be unique"
        allUuids.size() == 15
    }
}