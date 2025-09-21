package finance.resolvers

import finance.BaseIntegrationSpec
import finance.controllers.TransferGraphQLController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared

/**
 * Simple test to verify ONLY TransferGraphQLController field injection works
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferGraphQLControllerOnlySpec extends BaseIntegrationSpec {

    @Shared
    @Autowired
    TransferGraphQLController transferGraphQLController

    def "should inject TransferGraphQLController successfully"() {
        expect: "controller is injected"
        transferGraphQLController != null
    }

    def "should be able to call transfers() method without null pointer exception"() {
        when: "calling transfers method"
        def result = transferGraphQLController.transfers()

        then: "should not throw null pointer exception"
        result != null
        result instanceof List

        and: "verify we got this far without dependency injection errors"
        true
    }

    def "should have field injection working (not constructor injection)"() {
        when: "checking the controller class structure"
        def hasConstructorParams = transferGraphQLController.class.constructors.any {
            it.parameterTypes.length > 0
        }

        then: "should not have constructor parameters (confirming field injection)"
        !hasConstructorParams

        and: "verify this is using field injection approach"
        transferGraphQLController.class.declaredFields.any {
            it.name == 'transferService'
        }
    }
}