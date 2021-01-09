package finance.routes

import finance.Application
import finance.configurations.CamelProperties
import org.apache.camel.CamelContext
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
class JsonFileWriterRouteBuilderSpec extends Specification {

    @Autowired
    protected JsonFileWriterRouteBuilder jsonFileWriterRouteBuilder

    @Autowired
    protected CamelProperties camelProperties

    protected ProducerTemplate producer
    protected CamelContext camelContext
    protected PollingConditions conditions = new PollingConditions(timeout: 20, initialDelay: 1.5, factor: 1.25)
    protected String baseName = new FileSystemResource("").file.absolute

    void setup() {
        camelContext = jsonFileWriterRouteBuilder.context
        producer = camelContext.createProducerTemplate()
        camelContext.start()

        camelContext.routes.each { route -> route.setAutoStartup(true) }
        producer.setDefaultEndpointUri(camelProperties.jsonFileWriterRoute)
    }

    void cleanup() {
        camelContext.stop()
    }

    void 'test -- valid payload - 1 messages'() {
        given:
        String fname = UUID.randomUUID()
        when:
        producer.sendBodyAndHeader('fileContent', 'guid', fname)

        then:
        conditions.eventually {
            ResourceUtils.getFile("${baseName}/func_json_in/.processed-successfully/${fname}")
        }
        0 * _
    }
}
