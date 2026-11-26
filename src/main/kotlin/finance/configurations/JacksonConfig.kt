package finance.configurations

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule

@Configuration(proxyBeanMethods = false)
open class JacksonConfig {
    /**
     * Registers the Kotlin module with NullIsSameAsDefault on Spring Boot's Jackson 3 JsonMapper.
     * Runs last so it replaces the default-configured KotlinModule that findAndAddModules() picks up.
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    open fun kotlinModuleCustomizer(): JsonMapperBuilderCustomizer =
        JsonMapperBuilderCustomizer { builder ->
            builder.addModule(kotlinModule())
        }

    companion object {
        fun kotlinModule(): KotlinModule =
            KotlinModule
                .Builder()
                .configure(KotlinFeature.NullIsSameAsDefault, true)
                .build()
    }
}
