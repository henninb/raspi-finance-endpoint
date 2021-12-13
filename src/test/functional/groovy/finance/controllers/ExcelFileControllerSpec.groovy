package finance.controllers

import finance.Application
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExcelFileControllerSpec extends BaseControllerSpec {

//    @Ignore
//    void "test Excel File controller"() {
//        given:
//        HttpEntity entity = new HttpEntity<>(null, headers)
//
//        when:
//        ResponseEntity<String> response = restTemplate.exchange(
//                createURLWithPort('/excel/file/export'), HttpMethod.GET,
//                entity, String)
//
//        then:
//        response.statusCode == HttpStatus.OK
//        0 * _
//    }

    void 'test it'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        restTemplate.exchange(
                createURLWithPort('/excel/file/export'), HttpMethod.GET,
                entity, String)
        then:
        0 * _
    }
}
