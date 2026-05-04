package finance.configurations

import spock.lang.Specification

class PerformanceMonitoringConfigurationSpec extends Specification {

    PerformanceMonitoringConfiguration config

    def setup() {
        config = new PerformanceMonitoringConfiguration()
    }

    def "performanceMonitoringProperties returns non-null bean"() {
        when:
        def props = config.performanceMonitoringProperties()

        then:
        props != null
    }

    def "performanceMonitoringProperties has correct default method warn threshold"() {
        when:
        def props = config.performanceMonitoringProperties()

        then:
        props.methodWarnThresholdMs == 500L
    }

    def "performanceMonitoringProperties has correct default method error threshold"() {
        when:
        def props = config.performanceMonitoringProperties()

        then:
        props.methodErrorThresholdMs == 2000L
    }

    def "performanceMonitoringProperties has correct default SQL slow query threshold"() {
        when:
        def props = config.performanceMonitoringProperties()

        then:
        props.sqlSlowQueryThresholdMs == 100L
    }

    def "performanceMonitoringProperties has correct default SQL very slow query threshold"() {
        when:
        def props = config.performanceMonitoringProperties()

        then:
        props.sqlVerySlowQueryThresholdMs == 500L
    }

    def "performanceMonitoringProperties has console reporting enabled by default"() {
        when:
        def props = config.performanceMonitoringProperties()

        then:
        props.consoleReportingEnabled == true
    }

    def "performanceMonitoringProperties has correct default console reporting interval"() {
        when:
        def props = config.performanceMonitoringProperties()

        then:
        props.consoleReportingIntervalSeconds == 60L
    }

    def "performanceMonitoringProperties has SQL logging enabled by default"() {
        when:
        def props = config.performanceMonitoringProperties()

        then:
        props.sqlLoggingEnabled == true
    }

    def "PerformanceMonitoringProperties values can be modified"() {
        given:
        def props = config.performanceMonitoringProperties()

        when:
        props.methodWarnThresholdMs = 1000L
        props.consoleReportingEnabled = false
        props.sqlLoggingEnabled = false

        then:
        props.methodWarnThresholdMs == 1000L
        props.consoleReportingEnabled == false
        props.sqlLoggingEnabled == false
    }
}
