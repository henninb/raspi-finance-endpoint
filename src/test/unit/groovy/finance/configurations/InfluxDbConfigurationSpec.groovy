package finance.configurations

import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import org.springframework.core.env.Environment
import spock.lang.Specification

import java.time.Duration

class InfluxDbConfigurationSpec extends Specification {

    InfluxDbConfiguration configuration
    Environment mockEnvironment

    def setup() {
        configuration = new InfluxDbConfiguration()
        mockEnvironment = Mock(Environment)
    }

    def "should create InfluxConfig bean"() {
        when:
        def config = configuration.influxConfig(mockEnvironment)

        then:
        config != null
        config instanceof InfluxConfig
        config instanceof InfluxDbConfiguration.CustomInfluxConfig
    }

    def "CustomInfluxConfig should return environment property for apiVersion"() {
        given:
        mockEnvironment.getProperty("management.metrics.export.influx.api-version", "v1") >> "v2"
        def config = new InfluxDbConfiguration.CustomInfluxConfig(mockEnvironment)

        when:
        def result = config.get("apiVersion")

        then:
        result == "v2"
    }

    def "CustomInfluxConfig should return null for unknown keys"() {
        given:
        def config = new InfluxDbConfiguration.CustomInfluxConfig(mockEnvironment)

        when:
        def result = config.get("unknownKey")

        then:
        result == null
    }

    def "CustomInfluxConfig should return URI from environment"() {
        given:
        mockEnvironment.getProperty("management.metrics.export.influx.uri", "http://localhost:8086") >> "http://metrics.example.com:8086"
        def config = new InfluxDbConfiguration.CustomInfluxConfig(mockEnvironment)

        when:
        def result = config.uri()

        then:
        result == "http://metrics.example.com:8086"
    }

    def "CustomInfluxConfig should return database name from environment"() {
        given:
        mockEnvironment.getProperty("management.metrics.export.influx.db", "metrics") >> "finance_metrics"
        def config = new InfluxDbConfiguration.CustomInfluxConfig(mockEnvironment)

        when:
        def result = config.db()

        then:
        result == "finance_metrics"
    }

    def "CustomInfluxConfig should handle username configuration"() {
        given:
        mockEnvironment.getProperty("management.metrics.export.influx.user-name") >> "influx_user"
        def config = new InfluxDbConfiguration.CustomInfluxConfig(mockEnvironment)

        when:
        def result = config.userName()

        then:
        result == "influx_user"
    }

    def "CustomInfluxConfig should return null for blank username"() {
        given:
        mockEnvironment.getProperty("management.metrics.export.influx.user-name") >> ""
        def config = new InfluxDbConfiguration.CustomInfluxConfig(mockEnvironment)

        when:
        def result = config.userName()

        then:
        result == null
    }

    def "CustomInfluxConfig should parse step duration"() {
        given:
        mockEnvironment.getProperty("management.metrics.export.influx.step", "1m") >> "30s"
        def config = new InfluxDbConfiguration.CustomInfluxConfig(mockEnvironment)

        when:
        def result = config.step()

        then:
        result == Duration.parse("PT30s")
    }

    def "should be Spring Configuration with conditional property"() {
        expect:
        configuration.class.isAnnotationPresent(org.springframework.context.annotation.Configuration)
        configuration.class.isAnnotationPresent(org.springframework.boot.autoconfigure.condition.ConditionalOnProperty)
    }

    def "ConditionalOnProperty should check influx enabled property"() {
        given:
        def annotation = configuration.class.getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnProperty)

        expect:
        annotation.name() == ["management.metrics.export.influx.enabled"]
        annotation.havingValue() == "true"
    }
}