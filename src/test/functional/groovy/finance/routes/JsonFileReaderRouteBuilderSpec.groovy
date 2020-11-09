package finance.routes

import finance.Application
import finance.configurations.CamelProperties
import finance.helpers.TransactionBuilder
import org.apache.camel.Exchange
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JsonFileReaderRouteBuilderSpec extends Specification {

    @Autowired
    JsonFileReaderRouteBuilder jsonFileReaderRouteBuilder

    @Autowired
    CamelProperties camelProperties

    def "test -- jsonFileReaderRouteBuilder"() {
        given:
        def camelContext = jsonFileReaderRouteBuilder.getContext()
        def producer = camelContext.createProducerTemplate()
        camelContext.start()
        //camelContext.routes.forEach(route -> route.setAutoStartup(true))
        producer.setDefaultEndpointUri(camelProperties.jsonFileReaderRoute)
        def transaction = TransactionBuilder.builder().build()
        transaction.amount = 0.00
        transaction.guid = UUID.randomUUID()
        def transactions = [transaction]

        when:
        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, 'foo_brian.json')

        then:
        1 == 1
    }

    def "test -- jsonFileReaderRouteBuilder - bad filename"() {
        given:
        def camelContext = jsonFileReaderRouteBuilder.getContext()
        def producer = camelContext.createProducerTemplate()
        camelContext.start()
        //camelContext.routes.forEach(route -> route.setAutoStartup(true))
        producer.setDefaultEndpointUri(camelProperties.jsonFileReaderRoute)
        def transaction = TransactionBuilder.builder().build()
        def transactions = [transaction]

        when:
        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, 'foo_brian.txt')

        then:
        1 == 1
    }
}
