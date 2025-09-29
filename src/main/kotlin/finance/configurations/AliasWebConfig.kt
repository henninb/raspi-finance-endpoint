package finance.configurations

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class AliasWebConfig : WebMvcConfigurer {
    @Autowired
    lateinit var requestLoggingInterceptor: RequestLoggingInterceptor

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/health").setViewName("forward:/actuator/health")
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(requestLoggingInterceptor)
    }
}
