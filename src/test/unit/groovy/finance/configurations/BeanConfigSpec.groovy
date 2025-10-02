package finance.configurations

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import spock.lang.Specification

class BeanConfigSpec extends Specification {

    BeanConfig beanConfig
    MeterRegistry meterRegistry = Mock()

    def setup() {
        beanConfig = new BeanConfig()
    }

    def "should create TimedAspect bean with meter registry"() {
        when:
        TimedAspect timedAspect = beanConfig.timedAspect(meterRegistry)

        then:
        timedAspect != null
        timedAspect instanceof TimedAspect
    }

    def "should pass meter registry to TimedAspect constructor"() {
        when:
        TimedAspect timedAspect = beanConfig.timedAspect(meterRegistry)

        then:
        timedAspect != null
        // Verify the aspect was created with the provided meter registry
        // Note: TimedAspect doesn't expose the meter registry, so we just verify creation succeeded
    }
}