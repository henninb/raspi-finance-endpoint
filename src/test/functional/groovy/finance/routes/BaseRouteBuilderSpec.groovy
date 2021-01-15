package finance.routes

import finance.configurations.CamelProperties

import org.apache.camel.CamelContext
import org.apache.camel.ProducerTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class BaseRouteBuilderSpec extends Specification {

    @Autowired
    protected CamelProperties camelProperties

    protected ProducerTemplate producer
    protected CamelContext camelContext
    protected PollingConditions conditions = new PollingConditions(timeout: 20, initialDelay: 1.5, factor: 1.25)
    protected String baseName = new FileSystemResource("").file.absolutePath

    void cleanup() {
        camelContext.stop()
    }
}
