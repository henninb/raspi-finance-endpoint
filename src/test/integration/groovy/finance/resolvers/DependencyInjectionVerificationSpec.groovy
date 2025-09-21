package finance.resolvers

import finance.BaseIntegrationSpec
import finance.services.ITransferService
import finance.services.TransferService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared

/**
 * Simple test to verify that ITransferService and MeterRegistry beans
 * can be properly injected in the integration test context
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DependencyInjectionVerificationSpec extends BaseIntegrationSpec {

    @Shared
    @Autowired
    ITransferService transferService

    @Shared
    @Autowired
    MeterRegistry meterRegistry

    @Shared
    @Autowired
    TransferService concreteTransferService

    def "should inject ITransferService successfully"() {
        expect: "ITransferService bean is injected"
        transferService != null
        transferService instanceof ITransferService
    }

    def "should inject MeterRegistry successfully"() {
        expect: "MeterRegistry bean is injected"
        meterRegistry != null
        meterRegistry instanceof MeterRegistry
    }

    def "should inject concrete TransferService successfully"() {
        expect: "TransferService concrete bean is injected"
        concreteTransferService != null
        concreteTransferService instanceof TransferService
    }

    def "should be able to call methods on injected services"() {
        when: "calling methods on the injected services"
        def transfers = transferService.findAllTransfers()
        def concreteTransfers = concreteTransferService.findAllTransfers()
        def counter = meterRegistry.counter("test.verification.counter")

        then: "services work properly"
        transfers != null
        concreteTransfers != null
        counter != null
        counter.count() >= 0
    }
}