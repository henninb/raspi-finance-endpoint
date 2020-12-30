package finance.routes

import finance.Application
import finance.configurations.CamelProperties
import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import finance.repositories.TransactionRepository
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.ProducerTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.FileSystemResource
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.ResourceUtils
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JsonFileReaderRouteBuilderSpec extends Specification {

    @Autowired
    protected JsonFileReaderRouteBuilder jsonFileReaderRouteBuilder

    @Autowired
    protected CamelProperties camelProperties

    @Autowired
    protected TransactionRepository transactionRepository

    protected ProducerTemplate producer
    protected CamelContext camelContext
    protected PollingConditions conditions = new PollingConditions(timeout: 20, initialDelay: 1.5, factor: 1.25)
    protected String baseName = new FileSystemResource("").getFile().getAbsolutePath()

    void setup() {
        camelContext = jsonFileReaderRouteBuilder.context
        producer = camelContext.createProducerTemplate()
        camelContext.start()

        camelContext.routes.each { route -> route.setAutoStartup(true) }
        producer.setDefaultEndpointUri(camelProperties.jsonFileReaderRoute)
    }

    void 'test -- jsonFileReaderRouteBuilder happy path'() {
        given:
        Transaction transaction = TransactionBuilder.builder().amount(0.00).guid(UUID.randomUUID().toString()).build()
        List<Transaction> transactions = [transaction]

        when:
        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, "${transaction.guid}.json")

        then:
        conditions.eventually {
            Transaction databaseTransaction = transactionRepository.findByGuid(transaction.guid).get()
            databaseTransaction.guid == transaction.guid
            ResourceUtils.getFile("${baseName}/func_json_in/.processed-successfully/${transaction.guid}.json")
        }
    }

    void 'test -- jsonFileReaderRouteBuilder - with empty description'() {
        given:
        Transaction transaction = TransactionBuilder.builder().amount(0.00).guid(UUID.randomUUID().toString()).description('').build()
        List<Transaction> transactions = [transaction]

        when:
        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, "${transaction.guid}.json")

        then:
        conditions.eventually {
            ResourceUtils.getFile("${baseName}/func_json_in/.not-processed-failed-with-errors/${transaction.guid}.json")
        }
    }

    void 'test -- jsonFileReaderRouteBuilder - non json filename'() {
        given:
        Transaction transaction = TransactionBuilder.builder().guid(UUID.randomUUID().toString()).build()
        List<Transaction> transactions = [transaction]

        when:
        producer.sendBodyAndHeader(transactions.toString(), Exchange.FILE_NAME, "${transaction.guid}.txt")

        then:
        conditions.eventually {
            ResourceUtils.getFile("${baseName}/func_json_in/.not-processed-non-json-file/${transaction.guid}.txt")
        }
    }

    void 'test -- jsonFileReaderRouteBuilder -- bad file'() {
        given:
        String fname = UUID.randomUUID().toString() + ".json"
        when:
        producer.sendBodyAndHeader('invalid content', Exchange.FILE_NAME, fname)

        then:
        conditions.eventually {
            ResourceUtils.getFile("${baseName}/func_json_in/.not-processed-json-parsing-errors/${fname}")
        }
    }
}
