package finance.controllers

import finance.Application
import finance.domain.Category
import finance.helpers.CategoryBuilder
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

    TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    HttpHeaders headers

    @Autowired
    CategoryService categoryService

    @Shared
    Category category

    def setup() {
        headers = new HttpHeaders()
        category = CategoryBuilder.builder().build()
    }

    def createURLWithPort(String uri) {
        println "port = ${port}"

        return "http://localhost:" + port + uri
    }

    def "test Payment endpoint paymentId found and deleted"() {
        given:
        categoryService.insertCategory(category)
        HttpEntity entity = new HttpEntity<>(null, headers)
        println category

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/category/delete/${category.category}"), HttpMethod.DELETE,
                entity, String.class)
        then:
        response.statusCode == HttpStatus.OK
        0 * _
    }

//    def "test findAccount endpoint accountNameOwner not found"() {
//        given:
//        HttpEntity entity = new HttpEntity<>(null, headers)
//
//        when:
//        ResponseEntity<String> response = restTemplate.exchange(
//                createURLWithPort("/account/select/" + UUID.randomUUID().toString()), HttpMethod.GET,
//                entity, String.class)
//        then:
//        response.statusCode == HttpStatus.NOT_FOUND
//        0 * _
//    }
//
//    def "test deleteAccount endpoint"() {
//        given:
//        accountService.insertAccount(account)
//
//        HttpEntity entity = new HttpEntity<>(null, headers)
//
//        when:
//        ResponseEntity<String> response = restTemplate.exchange(
//                createURLWithPort("/account/delete/" + account.accountNameOwner), HttpMethod.DELETE,
//                entity, String.class)
//        then:
//        response.statusCode == HttpStatus.OK
//        0 * _
//
//        cleanup:
//        accountService.deleteByAccountNameOwner(account.accountNameOwner)
//    }
//
    def "test insertPayment endpoint"() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(category, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/category/insert/"), HttpMethod.POST,
                entity, String.class)
        then:
        //thrown HttpMessageNotReadableException
        response.statusCode == HttpStatus.OK
        0 * _
    }
}
