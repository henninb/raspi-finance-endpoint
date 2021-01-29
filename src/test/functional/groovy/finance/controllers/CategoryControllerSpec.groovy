package finance.controllers

import finance.Application
import finance.domain.Category
import finance.helpers.CategoryBuilder
import finance.services.CategoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
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

    @Shared
    protected String endpointName = 'category'

    void 'test insert Category'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, category.toString())

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test insert Category - duplicate'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, category.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test insert Category - empty'() {
        given:
        Category category = CategoryBuilder.builder().withCategory('').build()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, category.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test find category - not found'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, UUID.randomUUID().toString())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test find Category'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, category.category)

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
        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, category.category)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test Category delete - not found'() {
        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, UUID.randomUUID().toString())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    @Unroll
    void 'test insert Category - failure for irregular payload'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payload)

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
