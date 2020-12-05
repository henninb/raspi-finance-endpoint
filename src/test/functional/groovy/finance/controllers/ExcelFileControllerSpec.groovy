package finance.controllers

import finance.Application
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExcelFileControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    protected TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    protected HttpHeaders headers

    void setup() {
        headers = new HttpHeaders()
    }

    private String createURLWithPort(String uri) {
        return 'http://localhost:' + port + uri
    }

    @Ignore
    void "test Excel File controller"() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/excel/file/export'), HttpMethod.GET,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }
}
