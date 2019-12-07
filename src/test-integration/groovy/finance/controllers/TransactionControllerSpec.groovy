package finance.controllers

import finance.Application
import finance.domain.Transaction
import finance.helpers.TransactionBuilder
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

@ActiveProfiles("stage")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    HttpHeaders headers

    @Autowired
    TransactionService transactionService

    //@Autowired
    //TransactionDAO transactionDAO

    Transaction transaction = TransactionBuilder.builder().build()
    String guid = transaction.getGuid()



    def setupSpec() {
        headers = new HttpHeaders()

        //def x = transactionDAO.transactionCount()
        //println "transaction count = ${x}"

    }

    private String createURLWithPort(String uri) {
        println "port = ${port}"

        return "http://localhost:" + port + uri
    }

    def "test findTransaction endpoint"() {
        given:
        transactionService.insertTransaction(transaction)
        HttpEntity entity = new HttpEntity<>(null, headers)

        when: "rest call is initiated"
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/select/" + guid), HttpMethod.GET,
                entity, String.class)

        //println "response: " + response.body.toString()
        then:
        assert response.statusCode == HttpStatus.OK
        //assert response.statusCode == HttpStatus.NOT_FOUND
    }
}
