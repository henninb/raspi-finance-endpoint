package finance.controllers

import finance.Application
import finance.domain.Category
import finance.helpers.CategoryBuilder
import finance.services.AccountService
import finance.services.CategoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryControllerSpec extends BaseControllerSpec {

    @Autowired
    protected CategoryService categoryService

    @Autowired
    protected AccountService accountService

    @Shared
    protected jsonPayloadInvalidActiveStatus = '''
{"category":"none", "activeStatus":"invalid"}
'''

    @Shared
    protected jsonPayloadMissingCategory = '''
{"activeStatus":true}
'''

    void 'test -- Category endpoint category found and deleted'() {
        given:
        Category category = CategoryBuilder.builder().build()
        categoryService.insertCategory(category)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/category/delete/${category.category}"), HttpMethod.DELETE,
                entity, String)

        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'test -- find category endpoint category not found'() {
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

    void 'test -- deleteCategory endpoint'() {
        given:
        Category category = CategoryBuilder.builder().build()
        categoryService.insertCategory(category)

        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort('/category/delete/' + category.category), HttpMethod.DELETE,
                entity, String)
        then:
        response.statusCode == HttpStatus.OK
        0 * _

        cleanup:
        categoryService.deleteByCategoryName(category.category)
    }

    void 'test -- insertCategory endpoint'() {
        given:
        Category category = CategoryBuilder.builder().build()
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

    @Unroll
    void 'test -- insertCategory endpoint - failure for irregular payload'() {
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
        payload                        | httpStatus             | responseBody
        'badJson'                      | HttpStatus.BAD_REQUEST | 'Unrecognized token'
        '{"test":1}'                   | HttpStatus.BAD_REQUEST | 'value failed for JSON property category due to missing'
        '{badJson:"test"}'             | HttpStatus.BAD_REQUEST | 'was expecting double-quote to start field'
        jsonPayloadInvalidActiveStatus | HttpStatus.BAD_REQUEST | 'Cannot deserialize value of type'
        jsonPayloadMissingCategory     | HttpStatus.BAD_REQUEST | 'value failed for JSON property category due to missing'
    }
}
