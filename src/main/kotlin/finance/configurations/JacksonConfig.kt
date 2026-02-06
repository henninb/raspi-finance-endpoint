package finance.configurations

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class JacksonConfig {
    @Bean
    open fun kotlinModulePostProcessor(): BeanPostProcessor =
        object : BeanPostProcessor {
            override fun postProcessAfterInitialization(
                bean: Any,
                beanName: String,
            ): Any {
                if (bean is ObjectMapper) {
                    bean.registerModule(
                        KotlinModule
                            .Builder()
                            .configure(KotlinFeature.NullIsSameAsDefault, true)
                            .build(),
                    )
                    logger.info("Configured KotlinModule with NullIsSameAsDefault=true on ObjectMapper bean: $beanName")
                }
                return bean
            }
        }

    companion object {
        private val logger = LogManager.getLogger()
    }
}
