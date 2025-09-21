package finance.resolvers

import finance.BaseIntegrationSpec
import finance.services.ITransferService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController
import spock.lang.Shared

/**
 * Test to verify if @Controller vs @RestController affects dependency injection
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ControllerAnnotationTestSpec extends BaseIntegrationSpec {

    @Shared
    @Autowired
    TestRestController testRestController

    @Shared
    @Autowired
    TestController testController

    def "should inject @RestController with dependencies"() {
        expect:
        testRestController != null
        testRestController.transferService != null
        testRestController.meterRegistry != null
    }

    def "should inject @Controller with dependencies"() {
        expect:
        testController != null
        testController.transferService != null
        testController.meterRegistry != null
    }

    @RestController
    static class TestRestController {
        private final ITransferService transferService
        private final MeterRegistry meterRegistry

        TestRestController(ITransferService transferService, MeterRegistry meterRegistry) {
            this.transferService = transferService
            this.meterRegistry = meterRegistry
        }

        ITransferService getTransferService() { return transferService }
        MeterRegistry getMeterRegistry() { return meterRegistry }
    }

    @Controller
    static class TestController {
        private final ITransferService transferService
        private final MeterRegistry meterRegistry

        TestController(ITransferService transferService, MeterRegistry meterRegistry) {
            this.transferService = transferService
            this.meterRegistry = meterRegistry
        }

        ITransferService getTransferService() { return transferService }
        MeterRegistry getMeterRegistry() { return meterRegistry }
    }
}