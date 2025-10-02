package finance.configurations

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class BeanConfig {
    @Bean
    open fun timedAspect(meterRegistry: MeterRegistry): TimedAspect = TimedAspect(meterRegistry)
}
