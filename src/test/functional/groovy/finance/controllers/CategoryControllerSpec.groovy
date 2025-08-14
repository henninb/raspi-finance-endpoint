package finance.controllers

import finance.Application
import finance.domain.Category
import finance.helpers.CategoryBuilder
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

    @Shared
    protected Category category = CategoryBuilder.builder().withCategoryName('test_category_unique').build()

    @Shared
    protected String endpointName = 'category'

    void 'test insert Category'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, category.toString())

        then:
        response.statusCode == HttpStatus.CREATED
        0 * _
    }

    void 'test insert Category - duplicate'() {
        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, category.toString())

        then:
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'test insert Category - empty'() {
        given:
        Category category = CategoryBuilder.builder().withCategoryName('').build()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, category.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'test find category - not found'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, 'non_existent_category')

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'test find Category'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, category.categoryName)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test find active Category'() {
        given:
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/api/category/select/active'), HttpMethod.GET,
                entity, String)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test Category delete'() {
        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, category.categoryName)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test Category delete - not found'() {
        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, 'another_non_existent_category')

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
        payload                                             | httpStatus             | responseBody
        'badJson'                                           | HttpStatus.BAD_REQUEST | 'BAD_REQUEST: HttpMessageNotReadableException'
        '{"test":1}'                                        | HttpStatus.BAD_REQUEST | 'BAD_REQUEST: HttpMessageNotReadableException'
        '{badJson:"test"}'                                  | HttpStatus.BAD_REQUEST | 'BAD_REQUEST: HttpMessageNotReadableException'
        '{"categoryName":"none", "activeStatus":"invalid"}' | HttpStatus.BAD_REQUEST | 'BAD_REQUEST: HttpMessageNotReadableException'
        '{"activeStatus":true}'                             | HttpStatus.BAD_REQUEST | 'BAD_REQUEST: HttpMessageNotReadableException'
    }
}
