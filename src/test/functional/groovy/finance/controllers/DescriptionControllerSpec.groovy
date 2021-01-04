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
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Stepwise

import java.sql.Date


@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DescriptionControllerSpec extends BaseControllerSpec {

    @Shared
    Description description = DescriptionBuilder.builder().build()

    void 'test insertDescription endpoint'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(description, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/description/insert/'), HttpMethod.POST,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    @Ignore
    void 'test find description endpoint'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/description/select/' + description.description), HttpMethod.GET,
                entity, String)
        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test deleteDescription endpoint'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/description/delete/' + description.description), HttpMethod.DELETE, entity, String)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }
}
