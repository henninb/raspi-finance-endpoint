package finance.resolvers

import finance.BaseIntegrationSpec
import finance.controllers.TransferController
import finance.controllers.TransferGraphQLController
import finance.services.ITransferService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared

/**
 * Systematic comparison: REST controller vs GraphQL controller
 * Same test context, same dependencies, different behavior?
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestControllerComparisonSpec extends BaseIntegrationSpec {

    @Shared
    @Autowired
    TransferController transferController

    @Shared
    @Autowired
    TransferGraphQLController transferGraphQLController

    @Shared
    @Autowired
    ITransferService transferService

    @Shared
    @Autowired
    MeterRegistry meterRegistry

    def "should inject REST TransferController successfully"() {
        expect: "REST controller is injected"
        transferController != null
    }

    def "should inject GraphQL TransferGraphQLController successfully"() {
        expect: "GraphQL controller is injected"
        transferGraphQLController != null
    }

    def "should inject ITransferService successfully"() {
        expect: "service is injected"
        transferService != null
    }

    def "should inject MeterRegistry successfully"() {
        expect: "meter registry is injected"
        meterRegistry != null
    }

    def "should be able to call REST controller method"() {
        when: "calling REST controller method"
        def result = transferController.transfers()

        then: "should work without NPE"
        result != null
        result instanceof org.springframework.http.ResponseEntity

        and: "log that REST controller worked"
        println "✅ REST TransferController.transfers() worked: ${result.statusCode}"
    }

    def "should be able to call GraphQL controller method"() {
        when: "calling GraphQL controller method"
        def result = transferGraphQLController.transfers()

        then: "should work without NPE"
        result != null
        result instanceof List

        and: "log that GraphQL controller worked"
        println "✅ GraphQL TransferGraphQLController.transfers() worked: ${result.size()} transfers"
    }
}