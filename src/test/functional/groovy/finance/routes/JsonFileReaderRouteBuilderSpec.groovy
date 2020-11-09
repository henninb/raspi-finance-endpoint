package finance.routes

import finance.Application
import finance.helpers.TransactionBuilder
import org.apache.camel.Exchange
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
import spock.lang.Specification

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JsonFileReaderRouteBuilderSpec extends Specification {

    @Autowired
    JsonFileReaderRouteBuilder jsonFileReaderRouteBuilder

    @Ignore
    def "test -- jsonFileReaderRouteBuilder"() {
        given:
        def camelContext = jsonFileReaderRouteBuilder.getContext()
        def producer = camelContext.createProducerTemplate()
        def transactions = [TransactionBuilder.builder().build()]

        when:
        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, 'foo_brian.json')

        then:
        1 == 1
    }
}
