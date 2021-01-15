package finance.routes

import finance.Application
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.ResourceUtils

@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JsonFileWriterRouteBuilderSpec extends BaseRouteBuilderSpec {

    @Autowired
    protected JsonFileWriterRouteBuilder jsonFileWriterRouteBuilder

    void setup() {
        camelContext = jsonFileWriterRouteBuilder.context
        producer = camelContext.createProducerTemplate()
        camelContext.start()

        camelContext.routes.each { route -> route.setAutoStartup(true) }
        producer.setDefaultEndpointUri(camelProperties.jsonFileWriterRoute)
    }

    void 'test valid payload - 1 messages'() {
        given:
        String fileName = UUID.randomUUID()
        String filePath = "${baseName}/func_json_in/.processed-successfully/${fileName}"

        when:
        producer.sendBodyAndHeader('fileContent', 'guid', fileName)

        then:
        conditions.eventually {
            ResourceUtils.getFile("$filePath").exists()
        }
        0 * _
    }

    void 'test valid payload - incorrect header'() {
        given:
        String fileName = UUID.randomUUID()
        when:
        producer.sendBodyAndHeader('fileContent', 'incorrect-header', fileName)

        then:
        thrown(RuntimeException)
        0 * _
    }

    void 'test valid payload - null fileName'() {
        given:
        String fileName = null
        when:
        producer.sendBodyAndHeader('fileContent', 'guid', fileName)

        then:
        thrown(RuntimeException)
        0 * _
    }
}
