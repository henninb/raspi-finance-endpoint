package finance.routes

import finance.Application
import finance.domain.Transaction
import finance.helpers.TransactionBuilder
import org.apache.camel.CamelExecutionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionToDatabaseRouteBuilderSpec extends BaseRouteBuilderSpec {

    @Autowired
    protected TransactionToDatabaseRouteBuilder transactionToDatabaseRouteBuilder

    void setup() {
        camelContext = transactionToDatabaseRouteBuilder.context
        producer = camelContext.createProducerTemplate()
        camelContext.start()

        camelContext.routes.each { route -> route.setAutoStartup(true) }
        producer.setDefaultEndpointUri(camelProperties.transactionToDatabaseRoute)
    }

    void 'test valid payload - 1 transaction'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        producer.sendBody([transaction])

        then:
        noExceptionThrown()
        0 * _
    }

    void 'test valid payload - 1 transaction with validation errors'() {
        given:
        Transaction transaction = TransactionBuilder.builder().withAccountNameOwner('').build()

        when:
        producer.sendBody([transaction])

        then:
        thrown(CamelExecutionException)
        0 * _
    }

    void 'test valid payload - invalid payload'() {
        when:
        producer.sendBody('transaction')

        then:
        thrown(CamelExecutionException)
        0 * _
    }
}
