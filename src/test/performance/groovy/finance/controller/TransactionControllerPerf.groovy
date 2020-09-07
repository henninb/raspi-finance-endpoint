package finance.controller

import com.fasterxml.jackson.databind.ObjectMapper
import finance.Application
import finance.helpers.TransactionBuilder
import finance.domain.Transaction
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification

@ActiveProfiles("perf")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionControllerPerf extends Specification {
    @LocalServerPort
    protected int port

    TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    HttpHeaders headers

    @Shared
    Transaction transaction

    private ObjectMapper mapper = new ObjectMapper()

    def setup() {
        headers = new HttpHeaders()
        transaction = TransactionBuilder.builder().build()
        transaction.guid = UUID.randomUUID()
    }

    private String createURLWithPort(String uri) {
        println "port = ${port}"

        return "http://localhost:" + port + uri
    }


    def "test insertTransaction endpoint"() {
        given:
        def transaction = TransactionBuilder.builder().build()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(transaction.toString(), headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/insert/"), HttpMethod.POST,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.OK
    }

}
