package finance.controller

import com.fasterxml.jackson.databind.ObjectMapper
import finance.Application
import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

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
        return "http://localhost:" + port + uri
    }

    @Unroll
    def "test insertTransaction endpoint"() {
        given:
        def transaction = TransactionBuilder.builder().build()
        transaction.notes = notes
        transaction.guid = guid
        transaction.description = description

        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(transaction.toString(), headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/transaction/insert/"), HttpMethod.POST,
                entity, String.class)
        then:
        assert response.statusCode == HttpStatus.OK

        where:
        notes                                  | guid              | description
        generatingRandomAlphanumericString(10) | UUID.randomUUID() | generatingRandomAlphanumericString(25)
        generatingRandomAlphanumericString(10) | UUID.randomUUID() | generatingRandomAlphanumericString(25)
        generatingRandomAlphanumericString(10) | UUID.randomUUID() | generatingRandomAlphanumericString(25)
        generatingRandomAlphanumericString(10) | UUID.randomUUID() | generatingRandomAlphanumericString(25)
        generatingRandomAlphanumericString(10) | UUID.randomUUID() | generatingRandomAlphanumericString(25)
        generatingRandomAlphanumericString(10) | UUID.randomUUID() | generatingRandomAlphanumericString(25)
        generatingRandomAlphanumericString(10) | UUID.randomUUID() | generatingRandomAlphanumericString(25)
        generatingRandomAlphanumericString(10) | UUID.randomUUID() | generatingRandomAlphanumericString(25)
        generatingRandomAlphanumericString(10) | UUID.randomUUID() | generatingRandomAlphanumericString(25)
        generatingRandomAlphanumericString(10) | UUID.randomUUID() | generatingRandomAlphanumericString(25)
        generatingRandomAlphanumericString(10) | UUID.randomUUID() | generatingRandomAlphanumericString(25)
    }

    static def generatingRandomAlphanumericString(targetStringLength) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString
    }

}
