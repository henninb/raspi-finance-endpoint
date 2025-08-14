package finance.controllers

import finance.Application
import finance.domain.Description
import finance.helpers.DescriptionBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DescriptionControllerSpec extends BaseControllerSpec {

    @Shared
    protected Description description = DescriptionBuilder.builder().withDescriptionName('test_description_unique').build()

    @Shared
    protected String endpointName = 'description'

    void 'test insert Description'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, description.toString())

        then:
        response.statusCode == HttpStatus.CREATED
        0 * _
    }

    void 'test insert Description - duplicate'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, description.toString())

        then:
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'test insert Description - empty'() {
        given:
        Description emptyDescription = DescriptionBuilder.builder().withDescriptionName('').build()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, emptyDescription.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test find description'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, description.descriptionName)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test find description - not found'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, UUID.randomUUID().toString())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test delete description'() {
        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, description.descriptionName)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test find description - not found after removal'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, description.descriptionName)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test delete Description - not found'() {
        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, UUID.randomUUID().toString())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }
}
