package finance.configurations

import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import org.springframework.core.env.Environment
import org.springframework.mock.env.MockEnvironment
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

    def "CustomInfluxConfig should return enabled true from environment"() {
        given:
        def env = new MockEnvironment().withProperty("management.metrics.export.influx.enabled", "true")
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.enabled()

        then:
        result == true
    }

    def "CustomInfluxConfig should return enabled false by default"() {
        given:
        def env = new MockEnvironment()
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.enabled()

        then:
        result == false
    }

    def "CustomInfluxConfig should return password from environment"() {
        given:
        def env = new MockEnvironment().withProperty("management.metrics.export.influx.password", "secretpass")
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.password()

        then:
        result == "secretpass"
    }

    def "CustomInfluxConfig should return null for blank password"() {
        given:
        def env = new MockEnvironment().withProperty("management.metrics.export.influx.password", "")
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.password()

        then:
        result == null
    }

    def "CustomInfluxConfig should return null when password not set"() {
        given:
        def env = new MockEnvironment()
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.password()

        then:
        result == null
    }

    def "CustomInfluxConfig should return autoCreateDb false from environment"() {
        given:
        def env = new MockEnvironment().withProperty("management.metrics.export.influx.auto-create-db", "false")
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.autoCreateDb()

        then:
        result == false
    }

    def "CustomInfluxConfig should return autoCreateDb true by default"() {
        given:
        def env = new MockEnvironment()
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.autoCreateDb()

        then:
        result == true
    }

    def "CustomInfluxConfig should return compressed false from environment"() {
        given:
        def env = new MockEnvironment().withProperty("management.metrics.export.influx.compressed", "false")
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.compressed()

        then:
        result == false
    }

    def "CustomInfluxConfig should return compressed true by default"() {
        given:
        def env = new MockEnvironment()
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.compressed()

        then:
        result == true
    }

    def "CustomInfluxConfig should parse custom connectTimeout from environment"() {
        given:
        def env = new MockEnvironment().withProperty("management.metrics.export.influx.connect-timeout", "5s")
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.connectTimeout()

        then:
        result == Duration.parse("PT5S")
    }

    def "CustomInfluxConfig should use default connectTimeout of 10s"() {
        given:
        def env = new MockEnvironment()
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.connectTimeout()

        then:
        result == Duration.parse("PT10S")
    }

    def "CustomInfluxConfig should parse custom readTimeout from environment"() {
        given:
        def env = new MockEnvironment().withProperty("management.metrics.export.influx.read-timeout", "60s")
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.readTimeout()

        then:
        result == Duration.parse("PT60S")
    }

    def "CustomInfluxConfig should use default readTimeout of 30s"() {
        given:
        def env = new MockEnvironment()
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.readTimeout()

        then:
        result == Duration.parse("PT30S")
    }

    def "CustomInfluxConfig should return bucket from environment"() {
        given:
        def env = new MockEnvironment().withProperty("management.metrics.export.influx.bucket", "my-bucket")
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.bucket()

        then:
        result == "my-bucket"
    }

    def "CustomInfluxConfig should return empty string for bucket by default"() {
        given:
        def env = new MockEnvironment()
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.bucket()

        then:
        result == ""
    }

    def "CustomInfluxConfig should return org from environment"() {
        given:
        def env = new MockEnvironment().withProperty("management.metrics.export.influx.org", "my-org")
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.org()

        then:
        result == "my-org"
    }

    def "CustomInfluxConfig should return null org when not configured"() {
        given:
        def env = new MockEnvironment()
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.org()

        then:
        result == null
    }

    def "CustomInfluxConfig should return token from environment"() {
        given:
        def env = new MockEnvironment().withProperty("management.metrics.export.influx.token", "my-token-value")
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.token()

        then:
        result == "my-token-value"
    }

    def "CustomInfluxConfig should return null token when not configured"() {
        given:
        def env = new MockEnvironment()
        def config = new InfluxDbConfiguration.CustomInfluxConfig(env)

        when:
        def result = config.token()

        then:
        result == null
    }

    def "should create InfluxMeterRegistry bean"() {
        given:
        def influxConfig = Stub(InfluxConfig) {
            connectTimeout() >> Duration.ofSeconds(10)
            readTimeout() >> Duration.ofSeconds(30)
            enabled() >> false
            get(_) >> null
        }

        when:
        def registry = configuration.influxMeterRegistry(influxConfig)

        then:
        registry != null
        registry instanceof InfluxMeterRegistry

        cleanup:
        registry?.close()
    }
}