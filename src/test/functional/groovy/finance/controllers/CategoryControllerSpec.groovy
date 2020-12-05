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

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    protected TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    protected HttpHeaders headers

    @Autowired
    protected CategoryService categoryService

    @Autowired
    protected AccountService accountService

    @Shared
    protected Category category

    void setup() {
        headers = new HttpHeaders()
        category = CategoryBuilder.builder().build()
        category.category = UUID.randomUUID()
    }

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri
    }

    void "test -- Payment endpoint paymentId found and deleted"() {
        given:
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

    void "test -- find category endpoint category not found"() {
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

    void 'test -- delete category endpoint'() {
        given:
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

    void 'test -- insertPayment endpoint'() {
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
}
