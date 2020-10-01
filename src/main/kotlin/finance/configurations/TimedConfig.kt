package finance.configurations

import io.micrometer.core.aop.TimedAspect

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
open class TimedConfig {
    @Bean
    open fun timedAspect(registry: MeterRegistry): TimedAspect {
        return TimedAspect(registry)
    }
}