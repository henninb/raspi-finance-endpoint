package finance.controllers

import finance.Application
import finance.domain.Category
import finance.helpers.CategoryBuilder
import finance.services.CategoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryControllerSpec extends BaseControllerSpec {

    @Autowired
    protected CategoryService categoryService

    @Shared
    protected Category category = CategoryBuilder.builder().build()

    void 'test insert Category'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(category, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/category/insert/'), HttpMethod.POST,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test insert Category - duplicate'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(category, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/category/insert/'), HttpMethod.POST,
                entity, String)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test insert Category - empty'() {
        given:
        Category category = CategoryBuilder.builder().withCategory('').build()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(category, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/category/insert/'), HttpMethod.POST,
                entity, String)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test find category - not found'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/category/select/' + UUID.randomUUID()), HttpMethod.GET,
                entity, String)
        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test find Category'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/category/select/${category.category}"), HttpMethod.GET,
                entity, String)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test find active Category'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/category/select/active'), HttpMethod.GET,
                entity, String)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test Category delete'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/category/delete/${category.category}"), HttpMethod.DELETE,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    @Ignore('should fail')
    void 'test Category delete - not found'() {
        given:
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/category/delete/${UUID.randomUUID()}"), HttpMethod.DELETE,
                entity, String)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    @Unroll
    void 'test insert Category - failure for irregular payload'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(payload, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/category/insert/'), HttpMethod.POST,
                entity, String)
        then:
        response.statusCode.is(httpStatus)
        response.body.contains(responseBody)
        0 * _

        where:
        payload                                         | httpStatus             | responseBody
        'badJson'                                       | HttpStatus.BAD_REQUEST | 'Unrecognized token'
        '{"test":1}'                                    | HttpStatus.BAD_REQUEST | 'value failed for JSON property category due to missing'
        '{badJson:"test"}'                              | HttpStatus.BAD_REQUEST | 'was expecting double-quote to start field'
        '{"category":"none", "activeStatus":"invalid"}' | HttpStatus.BAD_REQUEST | 'Cannot deserialize value of type'
        '{"activeStatus":true}'                         | HttpStatus.BAD_REQUEST | 'value failed for JSON property category due to missing'
    }
}
