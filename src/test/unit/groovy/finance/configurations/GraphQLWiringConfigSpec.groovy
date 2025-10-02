package finance.configurations

import graphql.scalars.ExtendedScalars
import spock.lang.Specification

class GraphQLWiringConfigSpec extends Specification {

    GraphQLWiringConfig config

    def setup() {
        config = new GraphQLWiringConfig()
    }

    def "should create RuntimeWiringConfigurer bean"() {
        when:
        def configurer = config.runtimeWiringConfigurer()

        then:
        configurer != null
        configurer instanceof org.springframework.graphql.execution.RuntimeWiringConfigurer
    }

    def "should verify ExtendedScalars are available for Spring Boot 4.0"() {
        expect:
        ExtendedScalars.GraphQLLong != null
        ExtendedScalars.GraphQLBigDecimal != null
        ExtendedScalars.GraphQLLong.name == "Long"
        ExtendedScalars.GraphQLBigDecimal.name == "BigDecimal"
    }

    def "should verify SqlDateScalar is available"() {
        expect:
        SqlDateScalar.INSTANCE != null
    }

    def "should verify TimestampScalar is available"() {
        expect:
        TimestampScalar.INSTANCE != null
    }

    def "should be Spring Configuration"() {
        expect:
        config.class.isAnnotationPresent(org.springframework.context.annotation.Configuration)
    }
}