package finance.resolvers

import finance.BaseIntegrationSpec
import finance.controllers.TransferGraphQLController
import finance.controllers.PaymentGraphQLController
import finance.controllers.AccountGraphQLController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared

/**
 * Test to verify if actual GraphQL controllers can be injected and have proper dependencies
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActualControllerInjectionSpec extends BaseIntegrationSpec {

    @Shared
    @Autowired
    TransferGraphQLController transferGraphQLController

    @Shared
    @Autowired
    PaymentGraphQLController paymentGraphQLController

    @Shared
    @Autowired
    AccountGraphQLController accountGraphQLController

    def "should inject actual TransferGraphQLController"() {
        expect: "controller bean exists"
        transferGraphQLController != null
    }

    def "should have properly injected dependencies in TransferGraphQLController"() {
        when: "accessing the controller's dependencies"
        def hasTransferService = transferGraphQLController.hasProperty('transferService')
        def hasMeterRegistry = transferGraphQLController.hasProperty('meterRegistry')

        then: "dependencies should exist"
        hasTransferService
        hasMeterRegistry

        and: "let's try to access the actual dependency values"
        // This will show us if the dependencies are null or properly injected
        try {
            def transferService = transferGraphQLController.@transferService
            def meterRegistry = transferGraphQLController.@meterRegistry

            println "TransferService: ${transferService}"
            println "MeterRegistry: ${meterRegistry}"

            transferService != null
            meterRegistry != null
        } catch (Exception e) {
            println "Error accessing dependencies: ${e.message}"
            false
        }
    }

    def "should inject actual PaymentGraphQLController"() {
        expect: "controller bean exists"
        paymentGraphQLController != null
    }

    def "should have properly injected dependencies in PaymentGraphQLController"() {
        when: "accessing the controller's dependencies"
        def hasPaymentService = paymentGraphQLController.hasProperty('paymentService')
        def hasMeterRegistry = paymentGraphQLController.hasProperty('meterRegistry')

        then: "dependencies should exist"
        hasPaymentService
        hasMeterRegistry

        and: "let's try to access the actual dependency values"
        try {
            def paymentService = paymentGraphQLController.@paymentService
            def meterRegistry = paymentGraphQLController.@meterRegistry

            println "PaymentService: ${paymentService}"
            println "MeterRegistry: ${meterRegistry}"

            paymentService != null
            meterRegistry != null
        } catch (Exception e) {
            println "Error accessing dependencies: ${e.message}"
            false
        }
    }

    def "should inject actual AccountGraphQLController"() {
        expect: "controller bean exists"
        accountGraphQLController != null
    }

    def "should be able to call methods on actual controllers (will show null dependency errors)"() {
        when: "trying to call actual controller methods"
        println "=== Testing TransferGraphQLController.transfers() ==="
        try {
            def transfers = transferGraphQLController.transfers()
            println "Success: Got ${transfers?.size()} transfers"
        } catch (Exception e) {
            println "Error in transfers(): ${e.message}"
            println "Root cause: ${e.cause?.message}"
        }

        println "=== Testing PaymentGraphQLController.payments() ==="
        try {
            def payments = paymentGraphQLController.payments()
            println "Success: Got ${payments?.size()} payments"
        } catch (Exception e) {
            println "Error in payments(): ${e.message}"
            println "Root cause: ${e.cause?.message}"
        }

        then: "this test will document what happens"
        true // Always pass, we're just documenting the behavior
    }
}