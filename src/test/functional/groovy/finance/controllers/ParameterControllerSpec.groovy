package finance.controllers

import finance.Application
import finance.domain.Parameter
import finance.helpers.ParameterBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
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
        response.statusCode.is(HttpStatus.OK)
        0 * _
    }

    void 'test insert Parameter - duplicate'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, parameter.toString())

        then:
        response.statusCode.is(HttpStatus.BAD_REQUEST)
        0 * _
    }
}
