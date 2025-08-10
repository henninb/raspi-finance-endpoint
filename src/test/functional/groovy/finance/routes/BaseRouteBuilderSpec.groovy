package finance.routes

import finance.Application
import finance.configurations.CamelProperties
import org.apache.camel.CamelContext
import org.apache.camel.ProducerTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

@ActiveProfiles("int")
@SpringBootTest(classes = [Application], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = [Application])
class BaseRouteBuilderSpec extends Specification {

    @Autowired
    protected CamelProperties camelProperties

    protected ProducerTemplate producer
    protected CamelContext camelContext
    protected PollingConditions conditions = new PollingConditions(timeout: 20, initialDelay: 1.5, factor: 1.25)
    protected String baseName = System.getProperty("user.dir")

    void cleanup() {
        camelContext?.stop()
    }
}
