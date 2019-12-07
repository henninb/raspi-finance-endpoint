package finance.controllers

import finance.Application
import finance.helpers.TransactionBuilder
import finance.helpers.TransactionDAO
import finance.services.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification

@ActiveProfiles("local")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    TransactionService transactionService = Mock(TransactionService)

    TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    HttpHeaders headers

    @Autowired
    TransactionDAO transactionDAO

    def setupSpec() {
        headers = new HttpHeaders()

        //def x = transactionDAO.transactionCount()
        //println "transaction count = ${x}"

    }

    private String createURLWithPort(String uri) {
        println "port = ${port}"

        return "http://localhost:" + port + uri
    }

    def "findTransaction test"() {
        given:
        //transactionDAO.truncateAccountTable()
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/select/340c315d-39ad-4a02-a294-84a74c1c7ddc"), HttpMethod.GET,
                entity, String.class)

        println "response: " + response.body.toString()
        then:
        //2 * transactionService.findByGuid(_) >> Optional.of(TransactionBuilder.builder().build())
        //0 * _
        assert response.statusCode == HttpStatus.OK
        //assert response.statusCode == HttpStatus.NOT_FOUND
    }
}
