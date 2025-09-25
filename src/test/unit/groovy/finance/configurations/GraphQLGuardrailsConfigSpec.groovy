package finance.configurations

import graphql.analysis.MaxQueryComplexityInstrumentation
import graphql.analysis.MaxQueryDepthInstrumentation
import graphql.execution.instrumentation.ChainedInstrumentation
import spock.lang.Specification

class GraphQLGuardrailsConfigSpec extends Specification {

    def "createGuardrailInstrumentation returns chained depth and complexity instrumentations"() {
        given:
        def config = new GraphQLGuardrailsConfig()

        when:
        def instrumentation = config.createGuardrailInstrumentation(7, 123)

        then:
        instrumentation instanceof ChainedInstrumentation
        def list = (instrumentation as ChainedInstrumentation).instrumentations
        list.any { it instanceof MaxQueryDepthInstrumentation }
        list.any { it instanceof MaxQueryComplexityInstrumentation }
    }
}

