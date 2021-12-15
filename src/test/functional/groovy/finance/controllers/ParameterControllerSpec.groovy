package finance.controllers

import finance.Application
import finance.domain.Parameter
import finance.helpers.ParameterBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ParameterControllerSpec extends BaseControllerSpec {

    @Shared
    protected Parameter parameter = ParameterBuilder.builder().withParameterName('unique').build()

    @Shared
    protected String endpointName = 'parm'

    void 'test insert Parameter'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, parameter.toString())

        then:
        response.statusCode
        0 * _
    }

    void 'test insert Parameter - duplicate'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, parameter.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

//    void 'test find parameter - not found'() {
//        when:
//        ResponseEntity<String> response = selectEndpoint(endpointName, UUID.randomUUID().toString())
//
//        then:
//        response.statusCode == HttpStatus.NOT_FOUND
//        0 * _
//    }
//
//    void 'test find parameter'() {
//        when:
//        ResponseEntity<String> response = selectEndpoint(endpointName, parameter.parameterName)
//
//        then:
//        response.statusCode == HttpStatus.OK
//        0 * _
//    }
//
//    void 'test delete parameter'() {
//        when:
//        ResponseEntity<String> response = deleteEndpoint(endpointName, parameter.parameterName)
//
//        then:
//        response.statusCode == HttpStatus.OK
//        0 * _
//    }
//
//    void 'test find description - not found after removal'() {
//        when:
//        ResponseEntity<String> response = selectEndpoint(endpointName, parameter.parameterName)
//
//        then:
//        response.statusCode == HttpStatus.NOT_FOUND
//        0 * _
//    }
//
//    @Ignore('should return a 404 NOT_FOUND')
//    void 'test delete parameter - not found'() {
//        when:
//        ResponseEntity<String> response = deleteEndpoint(endpointName, UUID.randomUUID().toString())
//
//        then:
//        response.statusCode == HttpStatus.NOT_FOUND
//        0 * _
//    }
}
