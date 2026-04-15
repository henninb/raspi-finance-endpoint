package finance.configurations

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** Provides a no-op ResilienceComponents bean for the functional-test profile. */
@Configuration(proxyBeanMethods = false)
@Profile("func")
open class NoOpResilienceConfiguration {
    @Bean
    open fun resilienceComponents(): ResilienceComponents = ResilienceComponents.noOp()
}
