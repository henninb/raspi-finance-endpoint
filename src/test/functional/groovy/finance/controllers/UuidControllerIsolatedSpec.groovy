package finance.controllers

import groovy.json.JsonSlurper
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@ActiveProfiles("func")
class UuidControllerIsolatedSpec extends BaseControllerSpec {

    @Shared
    protected String endpointName = 'uuid'

    void 'should generate single UUID successfully'() {
        when: "posting to generate UUID endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/uuid/generate"),
            HttpMethod.POST, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body != null

        and: "response should contain UUID data"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.uuid != null
        jsonResponse.uuid instanceof String
        jsonResponse.uuid.length() == 36 // Standard UUID length
        jsonResponse.timestamp != null
        jsonResponse.source == "server"
        0 * _
    }

    void 'should generate batch UUIDs successfully with default count'() {
        when: "posting to generate batch UUIDs endpoint without count"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/uuid/generate/batch"),
            HttpMethod.POST, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body != null

        and: "response should contain batch UUID data"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.uuids != null
        jsonResponse.uuids instanceof List
        jsonResponse.uuids.size() == 1 // Default count
        jsonResponse.count == 1
        jsonResponse.timestamp != null
        jsonResponse.source == "server"
        0 * _
    }

    void 'should generate batch UUIDs with specific count'() {
        given: "a specific count of UUIDs to generate"
        int count = 5

        when: "posting to generate batch UUIDs endpoint with count parameter"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/uuid/generate/batch?count=${count}"),
            HttpMethod.POST, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body != null

        and: "response should contain correct number of UUIDs"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.uuids != null
        jsonResponse.uuids instanceof List
        jsonResponse.uuids.size() == count
        jsonResponse.count == count
        jsonResponse.timestamp != null
        jsonResponse.source == "server"
        0 * _
    }

    void 'should reject batch UUIDs with count exceeding limit'() {
        given: "a count exceeding the maximum limit"
        int count = 150 // Exceeds the 100 limit

        when: "posting to generate batch UUIDs endpoint with excessive count"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/uuid/generate/batch?count=${count}"),
            HttpMethod.POST, entity, String)

        then: "response should be bad request"
        response.statusCode == HttpStatus.BAD_REQUEST

        and: "response should contain error message"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.error != null
        jsonResponse.error.contains("Count must be between 1 and 100")
        0 * _
    }

    void 'should reject batch UUIDs with zero count'() {
        given: "a count of zero"
        int count = 0

        when: "posting to generate batch UUIDs endpoint with zero count"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/uuid/generate/batch?count=${count}"),
            HttpMethod.POST, entity, String)

        then: "response should be bad request"
        response.statusCode == HttpStatus.BAD_REQUEST

        and: "response should contain error message"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.error != null
        jsonResponse.error.contains("Count must be between 1 and 100")
        0 * _
    }

    void 'should successfully respond to health check endpoint'() {
        when: "posting to health check endpoint"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/uuid/health"),
            HttpMethod.POST, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body != null

        and: "response should contain health check data"
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        jsonResponse.status == "healthy"
        jsonResponse.service == "uuid-generation"
        jsonResponse.timestamp != null
        0 * _
    }

    void 'should reject unauthorized access to UUID generation endpoints'() {
        given: "no authentication token"
        HttpHeaders cleanHeaders = new HttpHeaders()

        when: "posting to generate UUID endpoint without token"
        cleanHeaders.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(null, cleanHeaders)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/uuid/generate"),
            HttpMethod.POST, entity, String)

        then: "response should be unauthorized"
        response.statusCode == HttpStatus.UNAUTHORIZED || response.statusCode == HttpStatus.FORBIDDEN
        0 * _
    }

    void 'should generate unique UUIDs in batch generation'() {
        given: "a request for multiple UUIDs"
        int count = 10

        when: "generating batch UUIDs"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/uuid/generate/batch?count=${count}"),
            HttpMethod.POST, entity, String)

        then: "all UUIDs should be unique"
        response.statusCode == HttpStatus.OK
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        def uuids = jsonResponse.uuids as List<String>

        and: "verify uniqueness"
        Set<String> uniqueUuids = new HashSet<>(uuids)
        uniqueUuids.size() == count // All UUIDs should be unique
        0 * _
    }

    void 'should generate UUIDs with valid format'() {
        when: "generating a single UUID"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/uuid/generate"),
            HttpMethod.POST, entity, String)

        then: "UUID should match standard format"
        response.statusCode == HttpStatus.OK
        JsonSlurper jsonSlurper = new JsonSlurper()
        def jsonResponse = jsonSlurper.parseText(response.body)
        String uuid = jsonResponse.uuid

        and: "verify UUID format (8-4-4-4-12 characters separated by hyphens)"
        uuid.matches(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)
        0 * _
    }
}