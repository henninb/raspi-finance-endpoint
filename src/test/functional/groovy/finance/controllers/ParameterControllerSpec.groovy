package finance.controllers

import finance.Application
import finance.domain.Parameter
import finance.helpers.ParameterBuilder
import groovy.util.logging.Log
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Stepwise

@Log
@Stepwise
@ActiveProfiles("int")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ParameterControllerSpec extends BaseControllerSpec {

    @Shared
    protected Parameter parameter = ParameterBuilder.builder()
            .withParameterName(UUID.randomUUID().toString())
            .withParameterValue(UUID.randomUUID().toString())
            .build()

    @Shared
    protected String endpointName = 'parm'

    void 'test insert, find, delete Parameter'() {
        given:
        ResponseEntity<String> response

        when: 'insert'
        response = insertEndpoint(endpointName, parameter.toString())

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(parameter.parameterValue)
        0 * _

        when: 'find found'
        response = selectEndpoint(endpointName, parameter.parameterName)

        then:
        response.statusCode == HttpStatus.OK
        0 * _

        when: 'attempt duplicate insert'
        response = insertEndpoint(endpointName, parameter.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _

        when: 'delete parameter'
        response = deleteEndpoint(endpointName, parameter.parameterName)

        then:
        response.statusCode == HttpStatus.OK
        0 * _

        when: 'find not found after removal'
        response = selectEndpoint(endpointName, parameter.parameterName)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'find parameter - not found'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, UUID.randomUUID().toString())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'delete parameter - not found'() {
        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, UUID.randomUUID().toString())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }
}
