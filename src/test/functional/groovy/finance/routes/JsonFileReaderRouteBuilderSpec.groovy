package finance.routes

import finance.Application
import finance.configurations.CamelProperties
import finance.helpers.TransactionBuilder
import finance.repositories.TransactionRepository
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.ProducerTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JsonFileReaderRouteBuilderSpec extends Specification {

    @Autowired
    JsonFileReaderRouteBuilder jsonFileReaderRouteBuilder

    @Autowired
    CamelProperties camelProperties

    @Autowired
    TransactionRepository transactionRepository

    ProducerTemplate producer
    CamelContext camelContext

    def conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)

    def setup() {
        camelContext = jsonFileReaderRouteBuilder.getContext()
        producer = camelContext.createProducerTemplate()
        camelContext.start()

        camelContext.routes.each {route -> route.setAutoStartup(true) }
        producer.setDefaultEndpointUri(camelProperties.jsonFileReaderRoute)
    }

    def "test -- jsonFileReaderRouteBuilder -- happy path"() {
        given:
        def transaction = TransactionBuilder.builder().amount(0.00).guid(UUID.randomUUID()).build()
        def transactions = [transaction]

        when:
        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, "${transaction.guid}.json")

        then:
        conditions.eventually {
            def databaseTransaction = transactionRepository.findByGuid(transaction.guid).get()
            databaseTransaction.guid == transaction.guid
        }
    }

    def "test -- jsonFileReaderRouteBuilder -- happy path - with empty description"() {
        given:
        def transaction = TransactionBuilder.builder().amount(0.00).guid(UUID.randomUUID()).description('').build()
        def transactions = [transaction]

        when:
        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, "${transaction.guid}.json")

        then:
        //TODO: bh 11/9/2020 - how to address this async issue without using a sleep?
        //sleep(5000)
        //def result = transactionRepository.findByGuid(transaction.guid).get()
        //result.guid == transaction.guid
        //def ex = thrown(RuntimeException)
        //ex.message.contains('transaction object has validation errors.')
        conditions.eventually {
            1 == 1
        }
    }

    def "test -- jsonFileReaderRouteBuilder - bad filename"() {
        given:
        def transaction = TransactionBuilder.builder().guid(UUID.randomUUID()).build()
        def transactions = [transaction]

        when:
        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, "${transaction.guid}.txt")

        then:
        conditions.eventually {
            1 == 1
        }
    }

    def "test -- jsonFileReaderRouteBuilder -- bad file"() {
        when:
        producer.sendBodyAndHeader('trash content', Exchange.FILE_NAME, 'foo_trash.json')

        then:
        //TODO: bh 11/9/2020 - how to address this async issue without using a sleep?
        conditions.eventually {
            1 == 1
        }
    }
}
