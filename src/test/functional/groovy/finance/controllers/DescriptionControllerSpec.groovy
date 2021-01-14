package finance.controllers

import finance.Application
import finance.domain.Description
import finance.helpers.DescriptionBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DescriptionControllerSpec extends BaseControllerSpec {

    @Shared
    protected Description description = DescriptionBuilder.builder().build()

    @Shared
    protected Description emptyDescription = DescriptionBuilder.builder().withDescription('').build()

    @Shared
    protected String endpointName = 'description'

    void 'test insert Description'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, description.toString())

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test insert Description - duplicate'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, description.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test insert Description - empty'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, description.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test find description'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/description/select/${description.description}"), HttpMethod.GET,
                entity, String)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test delete Description'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/description/delete/${description.description}"), HttpMethod.DELETE, entity, String)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test delete Description - not found'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/description/delete/${UUID.randomUUID()}"), HttpMethod.DELETE, entity, String)
        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test find description - not found'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/description/select/${description.description}"), HttpMethod.GET,
                entity, String)
        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }
}
