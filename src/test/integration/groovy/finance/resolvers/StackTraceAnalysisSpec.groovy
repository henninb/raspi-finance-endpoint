package finance.resolvers

import finance.BaseIntegrationSpec
import finance.controllers.TransferGraphQLController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared

/**
 * Get detailed stack trace of exactly where NPE occurs
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StackTraceAnalysisSpec extends BaseIntegrationSpec {

    @Shared
    @Autowired
    TransferGraphQLController transferGraphQLController

    def "should capture exact NPE location with full stack trace"() {
        given: "controller is injected"
        println "Controller injected: ${transferGraphQLController != null}"
        println "Controller class: ${transferGraphQLController?.class?.name}"

        when: "calling GraphQL controller method and catching exception"
        Exception caughtException = null
        def result = null

        try {
            result = transferGraphQLController.transfers()
            println "SUCCESS: Got result ${result}"
        } catch (Exception e) {
            caughtException = e
            println "EXCEPTION CAUGHT:"
            println "Type: ${e.class.name}"
            println "Message: ${e.message}"
            println "Stack trace:"
            e.printStackTrace()
        }

        then: "analyze what happened"
        if (caughtException != null) {
            println "\n=== DETAILED ANALYSIS ==="
            println "Exception type: ${caughtException.class.simpleName}"
            println "Exception message: ${caughtException.message}"

            def stackTrace = caughtException.stackTrace
            if (stackTrace.length > 0) {
                println "First stack frame: ${stackTrace[0].className}.${stackTrace[0].methodName}:${stackTrace[0].lineNumber}"
                println "Second stack frame: ${stackTrace[1].className}.${stackTrace[1].methodName}:${stackTrace[1].lineNumber}"
            }
        }

        // Always pass so we can see the output
        true
    }
}