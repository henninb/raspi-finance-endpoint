package finance.resolvers

import finance.BaseIntegrationSpec
import finance.services.ITransferService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller
import spock.lang.Shared

/**
 * Test to verify if GraphQL annotations affect dependency injection
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GraphQLAnnotationTestSpec extends BaseIntegrationSpec {

    @Shared
    @Autowired
    PlainController plainController

    @Shared
    @Autowired
    GraphQLControllerTest graphQLController

    def "should inject plain @Controller with dependencies"() {
        expect:
        plainController != null
        plainController.transferService != null
        plainController.meterRegistry != null
    }

    def "should inject @Controller with GraphQL annotations and dependencies"() {
        expect:
        graphQLController != null
        graphQLController.transferService != null
        graphQLController.meterRegistry != null
    }

    @Controller
    static class PlainController {
        private final ITransferService transferService
        private final MeterRegistry meterRegistry

        PlainController(ITransferService transferService, MeterRegistry meterRegistry) {
            this.transferService = transferService
            this.meterRegistry = meterRegistry
        }

        ITransferService getTransferService() { return transferService }
        MeterRegistry getMeterRegistry() { return meterRegistry }
    }

    @Controller
    static class GraphQLControllerTest {
        private final ITransferService transferService
        private final MeterRegistry meterRegistry

        GraphQLControllerTest(ITransferService transferService, MeterRegistry meterRegistry) {
            this.transferService = transferService
            this.meterRegistry = meterRegistry
        }

        ITransferService getTransferService() { return transferService }
        MeterRegistry getMeterRegistry() { return meterRegistry }

        @QueryMapping
        String testQuery() {
            return "test"
        }

        @MutationMapping
        String testMutation() {
            return "test"
        }
    }
}